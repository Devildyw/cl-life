package top.devildyw.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import top.devildyw.hmdp.dto.Result;
import top.devildyw.hmdp.entity.Shop;
import top.devildyw.hmdp.mapper.ShopMapper;
import top.devildyw.hmdp.service.IShopService;
import top.devildyw.hmdp.utils.CacheClient;
import top.devildyw.hmdp.utils.SystemConstants;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static top.devildyw.hmdp.utils.RedisConstants.*;
import static top.devildyw.hmdp.utils.SystemConstants.DEFAULT_PAGE_SIZE;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;


    @Override
    public Result queryById(Long id) {
        //防止缓存穿透
//        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);

        //1. 互斥锁解决缓存击穿
        Shop shop = cacheClient.queryWithMutex(CACHE_SHOP_KEY,id, Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);

        //2. 逻辑过期时间解决缓存击穿（需要在项目启动时 预热缓存数据）
//        Shop shop = cacheClient.queryWithLogicalLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        if (ObjectUtil.isNull(shop)){
            return Result.fail("商品不存在");
        }

        //返回结果
        return Result.ok(shop);
    }


    //互斥锁
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }


    @Override
    public Result update(Shop shop) {
        //校验参数有效性 todo: jsr303参数校验
        Long id = shop.getId();
        if (ObjectUtil.isNull(id)) {
            return Result.fail("店铺id不能为空");
        }
        String cacheKey = CACHE_SHOP_KEY + id;

        //1. 先更新数据库
        updateById(shop);
        //2. 删除缓存
        stringRedisTemplate.delete(cacheKey);
        //3. 返回结果
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        //1. 判断是否需要根据坐标查询
        if (x==null||y==null){
            //1.1 按正常的情况查
            // 根据类型分页查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }
        //2. 按地理位置排序查
        //2.1 计算分页参数
        int from = (current-1)*DEFAULT_PAGE_SIZE;
        int end = current*DEFAULT_PAGE_SIZE;

        //2.2 查询 redis，按照距离排序，结果:shopId distance
        String key = SHOP_GEO_KEY+typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo() //GEOSEARCH key BYLONLAT x y BYRADIUS 10 WITHDISTANCE
                .search(
                        key
                        , GeoReference.fromCoordinate(x, y)
                        , new Distance(5000)
                        , RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().limit(end)
                );
        //2.3 判空
        if (ObjectUtil.isNull(results)){
            return Result.ok(Collections.emptyList());
        }

        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        //3. 判断是否还有下一页
        if (list.size()<=from){
            //3.1 没有下一页，结束
            return Result.ok(Collections.emptyList());
        }

        //4. 截取 from ~ end 的部分
        List<Long> ids = new ArrayList<>(list.size());
        HashMap<String, Distance> distanceMap = new HashMap<>(list.size());
        list.stream().skip(from).forEach(result->{
            //4.1 获取店铺id
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            //4.2 获取店铺距离
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr,distance);
        });

        //5.1 根据id查询shop 按照给定的id顺序
        List<Shop> shops = getListOrderByShopIds(ids);
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }

        //6. 返回
        return Result.ok(shops);

    }

    /**
     * 根据给定的商铺id顺序批量查询商品
     * @param ids
     * @return
     */
    private List<Shop> getListOrderByShopIds(List<Long> ids) {
        return baseMapper.selectBatchIdsOrderByIds(ids);
    }
}
