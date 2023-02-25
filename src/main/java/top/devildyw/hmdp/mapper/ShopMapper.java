package top.devildyw.hmdp.mapper;

import top.devildyw.hmdp.entity.Shop;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface ShopMapper extends BaseMapper<Shop> {

    List<Shop> selectBatchIdsOrderByIds(List<Long> ids);
}
