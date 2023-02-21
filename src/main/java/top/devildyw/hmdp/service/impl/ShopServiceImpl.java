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

//    /**
//     * 缓存穿透
//     * @return
//     */
//    private Shop queryWithPassThrough(Long id){
//        String cacheKey = CACHE_SHOP_KEY + id;
//
//        //1. 从 redis 中查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(cacheKey);
//        //2. 判断是否存在（命中）
//        if (StringUtils.hasText(shopJson)) {
//            //3. 存在（命中）直接返回
//            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//            return shop;
//        }
//
//        //判断命中的是否是空字符串（因为数据库中不存在的我们都存的是空字符串“”） 为了防止查询到数据库中不存在的值 这里用空值返回
//        if (shopJson!=null){
//            //是则返回一个错误信息
//            return null;
//        }
//
//        //不是则未命中
//        //4. 不存在（未命中）查询数据库
//        Shop shop = getById(id);
//
//
//        //5. 数据库也不存在 返回错误信息
//        if (ObjectUtil.isNull(shop)){
//            //将空值写入redis 防止缓存穿透
//            stringRedisTemplate.opsForValue().set(cacheKey,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
//            return null;
//        }
//
//        //6. 存在，写入缓存
//        stringRedisTemplate.opsForValue().set(cacheKey,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
//
//        //7. 返回数据
//        return shop;
//    }

//    private void saveShop2Redis(Long id,Long expireSeconds){
//        //1. 查询店铺数据
//        Shop shop = getById(id);
//        //2. 封装逻辑过期时间
//        RedisData redisData = new RedisData();
//        redisData.setData(shop);
//        redisData.setExpireTime(LocalDateTime.now().plusSeconds());
//
//
//        //3. 写入redis
//    }

    /**
     * 缓存击穿 互斥锁
     * @param id
     * @return
     */
    private Shop queryWithMutex(Long id){
        String cacheKey = CACHE_SHOP_KEY + id;

        //1. 从 redis 中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(cacheKey);
        //2. 判断是否存在（命中）
        if (StringUtils.hasText(shopJson)) {
            //3. 存在（命中）直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }

        //判断命中的是否是空字符串（因为数据库中不存在的我们都存的是空字符串“”） 为了防止查询到数据库中不存在的值 这里用空值返回
        if (shopJson!=null){
            //是则返回一个错误信息
            return null;
        }

        //不是则未命中
        //4. 不存在（未命中）查询数据库 这里如果是热点数据/构建非常困难的数据需要使用互斥锁进行缓存重建
        //4.1 实现缓存重建
        //4.2 获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id; //这里使用细粒度的锁 只锁住一个店铺
        Shop shop = null;
        try {
            boolean flag = tryLock(lockKey);
            //4.3 判断是否获取成功
            if (!flag){
                //4.4 失败休眠并重试
                Thread.sleep(50);
                //重试
                queryWithMutex(id);
            }

            //4.5 获取到锁后判断有无其他线程已经构建 检查缓存
            shopJson = stringRedisTemplate.opsForValue().get(cacheKey);
            //2. 判断是否存在（命中）
            if (StringUtils.hasText(shopJson)) {
                //3. 存在（命中）直接返回
                shop = JSONUtil.toBean(shopJson, Shop.class);
                return shop;
            }

            //4.6 如果依旧没有缓存命中则从数据库重新构建
            Thread.sleep(200);
            shop = getById(id);

            //5. 数据库也不存在 返回错误信息
            if (ObjectUtil.isNull(shop)){
                //将空值写入redis 防止缓存穿透
                stringRedisTemplate.opsForValue().set(cacheKey,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
                return null;
            }

            //6. 存在，写入缓存
            stringRedisTemplate.opsForValue().set(cacheKey,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);

        }catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            //释放互斥锁
            unLock(lockKey);
        }


        //7. 返回数据
        return shop;
    }

//    @Override 简单缓存
//    public Result queryById(Long id) {
//        String cacheKey = CACHE_SHOP_KEY + id;
//
//        //1. 从 redis 中查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(cacheKey);
//        //2. 判断是否存在（命中）
//        if (StringUtils.hasText(shopJson)) {
//            //3. 存在（命中）直接返回
//            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//            return Result.ok(shop);
//        }
//
//        //判断命中的是否是空字符串（因为数据库中不存在的我们都存的是空字符串“”） 为了防止查询到数据库中不存在的值 这里用空值返回
//        if (shopJson!=null){
//            //是则返回一个错误信息
//            return Result.fail("店铺信息不存在");
//        }
//
//        //不是则未命中
//        //4. 不存在（未命中）查询数据库
//        Shop shop = getById(id);
//
//
//        //5. 数据库也不存在 返回错误信息
//        if (ObjectUtil.isNull(shop)){
//            //将空值写入redis 防止缓存穿透
//            stringRedisTemplate.opsForValue().set(cacheKey,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
//            return Result.fail("店铺不存在！");
//        }
//
//        //6. 存在，写入缓存
//        stringRedisTemplate.opsForValue().set(cacheKey,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
//
//        //7. 返回数据
//        return Result.ok(shop);
//
//    }


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
