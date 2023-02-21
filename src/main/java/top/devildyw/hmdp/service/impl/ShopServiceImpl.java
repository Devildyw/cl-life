package top.devildyw.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import top.devildyw.hmdp.dto.Result;
import top.devildyw.hmdp.entity.Shop;
import top.devildyw.hmdp.mapper.ShopMapper;
import top.devildyw.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.devildyw.hmdp.utils.CacheClient;
import top.devildyw.hmdp.utils.RedisData;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static top.devildyw.hmdp.utils.RedisConstants.*;

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

//        //1. 互斥锁解决缓存击穿
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
}
