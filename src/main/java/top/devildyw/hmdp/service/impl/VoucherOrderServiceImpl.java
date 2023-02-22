package top.devildyw.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.aop.framework.AopContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.core.StringRedisTemplate;
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

import javax.annotation.Resource;
import java.time.LocalDateTime;

import static top.devildyw.hmdp.utils.RedisConstants.ORDER_ID_KEY;

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

    @Override
    public Result seckillVoucher(Long voucherId) {
        //1. 查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);

        //2. 判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            //尚未开始
            return Result.fail("秒杀尚未开始！");
        }

        //3. 判断秒杀是否结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            //已经结束
            return Result.fail("秒杀已经结束！");
        }

        //4. 判断库存是否充足
        if (voucher.getStock() < 1) {
            //库存不足
            return Result.fail("库存不足！");
        }

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

        //5. Redis 分布式锁
        //用户id 从 ThreadLocalMap 中取出
        Long userId = UserHolder.getUser().getId();
        //5.1 创建锁对象
        SimpleRedisLock lock = new SimpleRedisLock(stringRedisTemplate, ORDER_ID_KEY + userId);
        //5.2 获取锁
        boolean isLock = lock.tryLock(1200);

        if (!isLock){
            //5.3 获取锁失败,返回错误或重试
            return Result.fail("不允许重复下单!");
        }

        //5.4 获取锁成功
        try {
            //获取代理对象
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }finally {
            //5.5释放锁
            lock.unLock();
        }
    }

    @Override
    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        // 用户id 从 ThreadLocalMap 中取出
        Long userId = UserHolder.getUser().getId();

        //5. 一人一单 判断该用户对于该优惠券是否已经有订单了
        //5.1 查询订单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        //5.2 判断是否存在
        if (count>0){
            //用户已经购买过了
            return Result.fail("用户已经下过单了！");
        }

        //6. 扣减库存 使用 CAS 来优化超卖问题(update from tb_xxx where voucher_id = voucherId and stock > 0)
        boolean success = seckillVoucherService.update()
                .setSql("stock=stock-1")
                .eq("voucher_id", voucherId).gt("stock",0).update();
        if (!success){
            return Result.fail("库存不足！");
        }

        //7. 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //7.1 订单id redis id 生成器 是其全局唯一
        long orderId = idWorker.nextId(ORDER_ID_KEY);
        voucherOrder.setId(orderId);
        //7.2 用户id
        voucherOrder.setUserId(userId);
        //7.3 优惠券id
        voucherOrder.setVoucherId(voucherId);
        //7.4 入库
        save(voucherOrder);

        return Result.ok(orderId);

    }
}
