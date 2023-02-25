package top.devildyw.hmdp.service;

import top.devildyw.hmdp.dto.Result;
import top.devildyw.hmdp.entity.Voucher;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Devil
 * @since 2023-01-11-15:35
 */
public interface IVoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);
}
