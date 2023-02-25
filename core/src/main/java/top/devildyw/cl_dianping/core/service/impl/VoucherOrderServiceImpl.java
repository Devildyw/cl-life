package top.devildyw.cl_dianping.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.devildyw.cl_dianping.common.DTO.Result;
import top.devildyw.cl_dianping.common.DTO.SeckillVoucherMQDTO;
import top.devildyw.cl_dianping.common.utils.RedisIdWorker;
import top.devildyw.cl_dianping.common.utils.UserHolder;
import top.devildyw.cl_dianping.core.entity.VoucherOrder;
import top.devildyw.cl_dianping.core.mapper.VoucherOrderMapper;
import top.devildyw.cl_dianping.core.service.ISeckillVoucherService;
import top.devildyw.cl_dianping.core.service.IVoucherOrderService;

import javax.annotation.Resource;
import java.util.Arrays;

import static top.devildyw.cl_dianping.common.constants.MQConstants.SECKILL_TOPIC;
import static top.devildyw.cl_dianping.common.constants.MQConstants.SECKILL_VOUCHER_TAG;
import static top.devildyw.cl_dianping.common.constants.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author Devil
 * @since 2023-01-11-15:35
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    /**
     * lua 脚本
     */
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker idWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 秒杀优惠券 高性能版本--RocketMQ 消息队列版本
     *
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
                voucherId.toString(), userId.toString());

        //2. 判断执行结果 0为具有抢购资格
        int r = result.intValue();
        if (r != 0) {
            //2.1 如果没有资格
            return Result.fail(r == 1 ? "库存不足!" : "你已经下过单了!");
        }

        //3. 封装消息发送到rocketmq消息队列
        //3.1 封装消息对象
        SeckillVoucherMQDTO message = new SeckillVoucherMQDTO(userId, voucherId, orderId);

        rocketMQTemplate.asyncSend(SECKILL_TOPIC + ":" + SECKILL_VOUCHER_TAG, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                //成功了 这里可以什么都不做
            }

            @Override
            public void onException(Throwable e) {
                //失败了 这里可以打日志写入日志系统
                log.error("记录日志");
            }
        });

        //4. 返回订单id
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
        if (count > 0) {
            //用户已经购买过了
            log.error("用户已经下过单了！");
            return;
        }
        //6. 扣减库存 使用 CAS 来优化超卖问题(update from tb_xxx where voucher_id = voucherId and stock > 0)
        boolean success = seckillVoucherService.update()
                .setSql("stock=stock-1")
                .eq("voucher_id", voucherId).gt("stock", 0).update();
        if (!success) {
            log.error("库存不足！");
            return;
        }

        save(voucherOrder);
    }


}
