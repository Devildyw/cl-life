package top.devildyw.hmdp.service;

import top.devildyw.hmdp.dto.Result;
import top.devildyw.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    /**
     * 实现秒杀优惠券
     * @param voucherId 优惠券id
     * @return
     */
    Result seckillVoucher(Long voucherId);

    /**
     * 供代理对象使用
     * @param voucherOrder
     * @return
     */
    void createVoucherOrder(VoucherOrder voucherOrder);
}
