package top.devildyw.cl_dianping.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.devildyw.cl_dianping.common.DTO.Result;
import top.devildyw.cl_dianping.core.entity.ShopType;


/**
 * <p>
 * 服务类
 * </p>
 *
 * @author Devil
 * @since 2023-01-11-15:35
 */
public interface IShopTypeService extends IService<ShopType> {

    /**
     * 查询商品类型列表（缓存）
     *
     * @return
     */
    Result queryTypeList();

}
