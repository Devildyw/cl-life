package top.devildyw.hmdp.service;

import top.devildyw.hmdp.dto.Result;
import top.devildyw.hmdp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {

    /**
     * 查询商品信息并且缓存
     * @param id
     * @return
     */
    Result queryById(Long id);

    /**
     * 更新数据库中的商铺信息
     * 同时更新缓存
     *
     * Cache Aside Pattern 更新数据库的同时 更新缓存
     * 选择删除缓存的策略（避免无效写缓存）
     * 选择先更新数据库再删缓存的方式（出现缓存不一致的概率较小）
     * @param shop
     * @return
     */
    Result update(Shop shop);
}
