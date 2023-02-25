package top.devildyw.hmdp.mapper;

import top.devildyw.hmdp.entity.Shop;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Devil
 * @since 2023-01-11-15:30
 */
public interface ShopMapper extends BaseMapper<Shop> {

    List<Shop> selectBatchIdsOrderByIds(List<Long> ids);
}
