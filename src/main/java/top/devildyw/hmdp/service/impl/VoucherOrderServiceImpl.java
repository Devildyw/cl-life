package top.devildyw.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.aspectj.weaver.ast.Var;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.remote.ResponseEntry;
import org.springframework.aop.framework.AopContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.devildyw.hmdp.dto.Result;
import top.devildyw.hmdp.entity.SeckillVoucher;
import top.devildyw.hmdp.entity.VoucherOrder;
import top.devildyw.hmdp.mapper.VoucherOrderMapper;
import top.devildyw.hmdp.service.ISeckillVoucherService;
import top.devildyw.hmdp.service.IVoucherOrderService;
import top.devildyw.hmdp.service.IVoucherService;
import top.devildyw.hmdp.utils.RedisIdWorker;
import top.devildyw.hmdp.utils.SimpleRedisLock;
import top.devildyw.hmdp.utils.UserHolder;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static top.devildyw.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker idWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Resource
    private IVoucherOrderService orderService;


    /**
     * 线程池
     */
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    @PostConstruct //带上这个注解的方法会在类初始化完成后 执行
    public void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }
    private class VoucherOrderHandler implements Runnable{
        String queueName = "stream.orders";
        @Override
        public void run() {
            while (true){
                try {
                    //1. 获取消息队列中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCKING 2000 STREAMS streams.order > ‘>’代表最近一条未消费的消息
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );
                    //2. 判断消息获取是否成功
                    if (list==null||list.isEmpty()){
                        //2.1 获取失败，说明没有消息，继续下一次循环
                        continue;
                    }

                    //3. 获取成功，可以下单
                    //3.1 解析消息中的订单信息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> values = record.getValue();
                    //将map赋值给bean
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                    handlerVoucherOrder(voucherOrder);

                    //4. ACK确认消息已经被消费 SACK streams.order g1 id
                    stringRedisTemplate.opsForStream().acknowledge(
                            queueName,
                            "g1",
                            record.getId()
                    );
                } catch (Exception e) {
                    log.error("处理订单异常",e);
                    //如果在执行的过程中出现了异常，调用方法去pending-list中获取消息消费 防止消息丢失
                    handlePendingList();
                }

            }
        }

        /**
         * 读取 pending-list 中没有被确定的第一条消息，防止消息丢失
         */
        private void handlePendingList() {
            while (true){
                try {
                    //1. 获取pending-list中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCKING 2000 STREAMS streams.order 0 ‘0’代表最近一条消费了但没确认的消息
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.from("0"))
                    );
                    //2. 判断消息获取是否成功
                    if (list==null||list.isEmpty()){
                        //2.1 获取失败，说明没有pending-list消息，结束循环
                        break;
                    }

                    //3. 获取成功，可以下单
                    //3.1 解析消息中的订单信息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> values = record.getValue();
                    //将map赋值给bean
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                    handlerVoucherOrder(voucherOrder);

                    //4. ACK确认消息已经被消费 SACK streams.order g1 id
                    stringRedisTemplate.opsForStream().acknowledge(
                            queueName,
                            "g1",
                            record.getId()
                    );
                } catch (Exception e) {
                    log.error("处理 pending-list 订单异常",e);
                    try {
                        //休眠50毫秒 防止 cpu 开销过大
                        Thread.sleep(50);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    //循环
                }

            }
        }
    }




    private void handlerVoucherOrder(VoucherOrder voucherOrder) {
        // 获取用户id 这里是新开的线程所以无法获取到上一个线程中的线程变量
        Long userId = voucherOrder.getUserId();

        //5.1 获取锁对象 之所以这里还使用锁是为了防止 redis 执行lua 脚本出现异常 但这里其实是单个线程下单 上不上锁都一样的
        RLock lock = redissonClient.getLock(ORDER_ID_KEY + userId);

        //5.2 获取锁
        boolean isLock = lock.tryLock();

        if (!isLock){
            //5.3 获取锁失败,返回错误或重试
            log.error("不允许重复下单！");
            return;
        }

        //5.4 获取锁成功
        try {
            orderService.createVoucherOrder(voucherOrder);
        } finally {
            //5.5释放锁
            lock.unlock();
        }

    }


    /**
     * 秒杀优惠券 高性能版本--Redis stream 消息队列版本
     * @param voucherId 优惠券id
     * @return
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
        //获取用户 id
        Long userId = UserHolder.getUser().getId();
        //获取订单id  redis id 生成器 是全局唯一
        long orderId = idWorker.nextId(ORDER_ID_KEY);

        //1. 执行 lua 脚本判断是否有抢购资格 如有则将下单相关消息发送到消息队列
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT
                , Arrays.asList(SECKILL_STOCK_KEY + voucherId, SECKILL_ORDER_KEY + voucherId),
                voucherId.toString(), userId.toString(),String.valueOf(orderId));

        //2. 判断执行结果 0为具有抢购资格
        int r = result.intValue();
        if (r!=0){
            //2.1 如果没有资格
            return Result.fail(r==1?"库存不足!":"你已经下过单了!");
        }

        //3. 返回订单id
        return Result.ok(orderId);

    }


    @Override
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {

        // 用户id
        Long userId = voucherOrder.getUserId();

        //获取优惠券id
        long voucherId = voucherOrder.getVoucherId();


        //5. 一人一单 判断该用户对于该优惠券是否已经有订单了
        //5.1 查询订单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        //5.2 判断是否存在
        if (count>0){
            //用户已经购买过了
            log.error("用户已经下过单了！");
            return;
        }
        //6. 扣减库存 使用 CAS 来优化超卖问题(update from tb_xxx where voucher_id = voucherId and stock > 0)
        boolean success = seckillVoucherService.update()
                .setSql("stock=stock-1")
                .eq("voucher_id", voucherId).gt("stock",0).update();
        if (!success){
            log.error("库存不足！");
            return;
        }

        save(voucherOrder);
    }




//--------------------------------------------------------------------------------------------------------
//    /**
//     * 阻塞队列 当阻塞队列中没有元素时，线程会阻塞 使用他来完成异步下单
//     */
//    private BlockingQueue<VoucherOrder> orderTask = new ArrayBlockingQueue<>(1024*1024);
//    private class VoucherOrderHandler implements Runnable{
//
//        @Override
//        public void run() {
//            //1. 一直执行获取队列中的任务
//            while (true){
//                try {
//                    //2. 获取队列中的订单信息
//                    VoucherOrder voucherOrder = orderTask.take();
//                    //3. 创建订单
//                    handlerVoucherOrder(voucherOrder);
//                } catch (InterruptedException e) {
//                    log.error("error");
//                    throw new RuntimeException(e);
//                }
//            }
//        }
//    }


//    /**
//     * 秒杀优惠券 高性能版本--阻塞队列版本
//     * @param voucherId 优惠券id
//     * @return
//     */
//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        //获取用户 id
//        Long userId = UserHolder.getUser().getId();
//
//        //1. 执行 lua 脚本判断是否有抢购资格
//        Long result = stringRedisTemplate.execute(
//                SECKILL_SCRIPT
//                , Arrays.asList(SECKILL_STOCK_KEY + voucherId, SECKILL_ORDER_KEY + voucherId),
//                voucherId.toString(), userId.toString());
//
//        //2. 判断执行结果 0为具有抢购资格
//        int r = result.intValue();
//        if (r!=0){
//            //2.1 如果没有资格
//            return Result.fail(r==1?"库存不足!":"你已经下过单了!");
//        }
//
//        //3. 为0，有购买资格,将下单信息保存到阻塞队列 创建订单
//        VoucherOrder voucherOrder = new VoucherOrder();
//        //3.1 订单id redis id 生成器 是其全局唯一
//        long orderId = idWorker.nextId(ORDER_ID_KEY);
//        voucherOrder.setId(orderId);
//        //3.2 用户id
//        voucherOrder.setUserId(userId);
//        //3.3 优惠券id
//        voucherOrder.setVoucherId(voucherId);
//
//        //3.4 保存到阻塞队列
//        orderTask.add(voucherOrder);
//
//        //4. 返回订单id
//        return Result.ok(orderId);
//
//    }

//    秒杀优惠券 低性能版本
//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        //1. 查询优惠券
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//
//        //2. 判断秒杀是否开始
//        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
//            //尚未开始
//            return Result.fail("秒杀尚未开始！");
//        }
//
//        //3. 判断秒杀是否结束
//        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
//            //已经结束
//            return Result.fail("秒杀已经结束！");
//        }
//
//        //4. 判断库存是否充足
//        if (voucher.getStock() < 1) {
//            //库存不足
//            return Result.fail("库存不足！");
//        }

//        java 本地锁 synchronized
//        //用户id 从 ThreadLocalMap 中取出
//        Long userId = UserHolder.getUser().getId();
//        //锁住同一个用户 userId.toString() 底层会创建一个字符串对象，加上intern就会在字符串常量池中找
//        //锁住整个方法，防止事务来不及提交导致的线程安全问题
//        synchronized(userId.toString().intern()) {
//            //使用代理对象调用方法使得方法事务生效（AOP 需要找到它的代理对象才能生效）
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            //8. 返回订单id
//            return proxy.createVoucherOrder(voucherId);
//        }

//        //5. Redis 分布式锁
//        //用户id 从 ThreadLocalMap 中取出
//        Long userId = UserHolder.getUser().getId();
//        //5.1 获取锁对象
////        SimpleRedisLock lock = new SimpleRedisLock(stringRedisTemplate, ORDER_ID_KEY + userId);
//        RLock lock = redissonClient.getLock(ORDER_ID_KEY + userId);
//
//        //5.2 获取锁
//        boolean isLock = lock.tryLock();
//
//        if (!isLock){
//            //5.3 获取锁失败,返回错误或重试
//            return Result.fail("不允许重复下单!");
//        }
//
//        //5.4 获取锁成功
//        try {
//            //获取代理对象
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            //5.5释放锁
//            lock.unlock();
//        }
//        return createVoucherOrder(voucherId);
//    }

}
