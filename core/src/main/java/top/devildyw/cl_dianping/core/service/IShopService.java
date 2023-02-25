package top.devildyw.cl_dianping.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.devildyw.cl_dianping.common.DTO.Result;
import top.devildyw.cl_dianping.core.entity.Shop;


/**
 * <p>
 * 服务类
 * </p>
 *
 * @author Devil
 * @since 2023-01-11-15:35
 */
public interface IShopService extends IService<Shop> {

    /**
     * 查询商品信息并且缓存
     *
     * @param id
     * @return
     */
    Result queryById(Long id);

    /**
     * 更新数据库中的商铺信息
     * 同时更新缓存
     * <p>
     * Cache Aside Pattern 更新数据库的同时 更新缓存
     * 选择删除缓存的策略（避免无效写缓存）
     * 选择先更新数据库再删缓存的方式（出现缓存不一致的概率较小）
     *
     * @param shop
     * @return
     */
    Result update(Shop shop);

    /**
     * 根据分类id 和 地理位置排序 分页查询店铺列表
     *
     * @param typeId  分类id
     * @param current 当前页
     * @param x
     * @param y
     * @return
     */
    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
