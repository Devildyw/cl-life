package top.devildyw.hmdp.service;

import top.devildyw.hmdp.dto.Result;
import top.devildyw.hmdp.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Devil
 * @since 2023-01-11-15:35
 */
public interface IShopTypeService extends IService<ShopType> {

    /**
     * 查询商品类型列表（缓存）
     * @return
     */
    Result queryTypeList();

}
