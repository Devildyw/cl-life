package top.devildyw.hmdp.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import top.devildyw.hmdp.dto.SeckillVoucherMQDTO;
import top.devildyw.hmdp.entity.VoucherOrder;
import top.devildyw.hmdp.service.IVoucherOrderService;

import javax.annotation.Resource;

import static top.devildyw.hmdp.utils.MQConstants.*;
import static top.devildyw.hmdp.utils.RedisConstants.ORDER_ID_KEY;


/**
 * 优惠券秒杀消费者
 * @author Devil
 * @since 2023-02-25-21:23
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = SECKILL_TOPIC,selectorExpression = SECKILL_VOUCHER_TAG,consumerGroup = SECKILL_VOUCHER_GROUP,
        selectorType = SelectorType.TAG,messageModel = MessageModel.CLUSTERING)
public class SeckillVoucherConsumer implements RocketMQListener<SeckillVoucherMQDTO> {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public void onMessage(SeckillVoucherMQDTO message) {
        //获得到队列中的消息

        //1. 封装订单信息
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setVoucherId(message.getVoucherId());
        voucherOrder.setId(message.getOrderId());
        voucherOrder.setUserId(message.getUserId());

        //2. 存入数据库
        handlerVoucherOrder(voucherOrder);

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
            voucherOrderService.createVoucherOrder(voucherOrder);
        } finally {
            //5.5释放锁
            lock.unlock();
        }

    }
}
