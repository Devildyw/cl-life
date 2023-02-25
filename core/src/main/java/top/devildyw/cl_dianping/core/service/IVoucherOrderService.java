package top.devildyw.cl_dianping.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.devildyw.cl_dianping.common.DTO.Result;
import top.devildyw.cl_dianping.core.entity.VoucherOrder;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author Devil
 * @since 2023-01-11-15:35
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    /**
     * 实现秒杀优惠券
     *
     * @param voucherId 优惠券id
     * @return
     */
    Result seckillVoucher(Long voucherId);

    /**
     * 供代理对象使用
     *
     * @param voucherOrder
     * @return
     */
    void createVoucherOrder(VoucherOrder voucherOrder);
}
