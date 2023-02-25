package top.devildyw.hmdp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.devildyw.hmdp.entity.Voucher;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Devil
 * @since 2023-01-11-15:30
 */
public interface VoucherMapper extends BaseMapper<Voucher> {

    List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);
}
