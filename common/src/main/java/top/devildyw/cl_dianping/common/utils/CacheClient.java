package top.devildyw.cl_dianping.common.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static top.devildyw.cl_dianping.common.constants.RedisConstants.*;

/**
 * @author Devil
 * @since 2023-02-01-15:27
 */
@Slf4j
@Component
public class CacheClient {

    private static final ThreadPoolExecutor CACHE_REBUILD_EXECUTOR = new ThreadPoolExecutor(10, 20, 10, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(), new ThreadPoolExecutor.DiscardPolicy());
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 存入redis缓存
     *
     * @param key
     * @param value
     * @param time
     * @param unit
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 设置逻辑过期
     *
     * @param key
     * @param value
     * @param expireTime
     * @param unit
     */
    public void setWithLogicalExpire(String key, Object value, Long expireTime, TimeUnit unit) {
        //设置逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(expireTime)));

        //写入 redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }


    /**
     * 泛型方法
     * 解决缓存穿透
     *
     * @param keyPrefix  前缀
     * @param id         构建缓存所用的标识
     * @param type       返回的具体类型
     * @param dbFallback 调用者传入的有参有返回值的函数
     * @param expireTime 过期时间
     * @param unit       时间单位
     * @param <R>
     * @param <ID>
     * @return
     */
    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long expireTime, TimeUnit unit) {
        String cacheKey = keyPrefix + id;

        //1. 从 redis 中查询对应key的缓存
        String json = stringRedisTemplate.opsForValue().get(cacheKey);
        //2. 判断是否存在（命中）
        if (StringUtils.hasText(json)) {
            //3. 存在（命中）直接返回
            return JSONUtil.toBean(json, type);
        }

        //判断命中的是否是空字符串（因为数据库中不存在的我们都存的是空字符串“”） 为了防止查询到数据库中不存在的值 这里用空值返回
        if (json != null) {
            //是则返回一个错误信息
            return null;
        }

        //不是则未命中
        //4. 不存在（未命中）查询数据库
        R r = dbFallback.apply(id);


        //5. 数据库也不存在 返回错误信息
        if (ObjectUtil.isNull(r)) {
            //将空值写入redis 防止缓存穿透
            set(cacheKey, "", expireTime, unit);
            return null;
        }

        //6. 存在，写入缓存
        set(cacheKey, r, expireTime, unit);//复用

        //7. 返回数据
        return r;
    }

    /**
     * 缓存穿透为基础解决缓存击穿
     *
     * @param keyPrefix  缓存key前缀
     * @param id         构建缓存用的标识
     * @param type       类型
     * @param dbFallback 构建缓存用的数据库方法
     * @param expireTime 过期时间
     * @param unit       时间单位
     * @param <R>
     * @param <ID>
     * @return
     */
    public <R, ID> R queryWithLogicalLogicalExpire(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long expireTime, TimeUnit unit) {

        /**
         * 逻辑过期解决是通过预先在缓存中加载热点数据设置逻辑过期时间，所以数据是不会过期的，通过业务代码来判断是否过期
         * 不存在缓存击穿问题，因为会直接在缓存中判断是否缓存，如果没有则代表数据库没有
         * 如果命中了缓存，我们需要判断是否逻辑时间过期，过期了需要重新查库更新
         */
        String cacheKey = keyPrefix + id;

        //1. 从 redis 中查询对应key的缓存
        String json = stringRedisTemplate.opsForValue().get(cacheKey);
        //2.是否存在（未命中）
        if (!StringUtils.hasText(json)) {
            //3. 不存在（未命中）直接返回
            return null;
        }

        //4. 命中，需要先反序列化获取RedisData
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        //4.1 获取逻辑过期时间，判断是否过期
        LocalDateTime time = redisData.getExpireTime();

        //4.2 是否过期
        if (time.isAfter(LocalDateTime.now())) {
            //4.3 未过期，直接返回缓存数据
            return r;
        }

        //4.4.1过期 重新构建缓存
        //4.4.2 获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        if (tryLock(lockKey)) {
            //4.4.3 获取锁成功，线程池调用一个线程构建缓存
            CACHE_REBUILD_EXECUTOR.execute(() -> {
                try {
                    //查询数据库重建缓存
                    R newR = dbFallback.apply(id);
                    //重建缓存
                    this.setWithLogicalExpire(cacheKey, newR, expireTime, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    unLock(lockKey);
                }
            });
        }

        //5. 返回现缓存数据（存在段暂不一致性）获取到锁的那个线程开了一个新线程去重建缓存后也返回旧数据
        return r;
    }


    /**
     * 使用互斥锁解决缓存击穿问题
     *
     * @param keyPrefix  缓存key前缀
     * @param id         构建缓存用的标识
     * @param type       类型
     * @param dbFallback 构建缓存用的数据库方法
     * @param expireTime 过期时间
     * @param unit       时间单位
     * @param <R>
     * @param <ID>
     * @return
     */
    public <R, ID> R queryWithMutex(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long expireTime, TimeUnit unit) {
        String cacheKey = keyPrefix + id;

        //1. 从 redis 中查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(cacheKey);
        //2. 判断是否存在（命中）
        if (StringUtils.hasText(json)) {
            //3. 存在（命中）直接返回
            R r = JSONUtil.toBean(json, type);
            return r;
        }

        //判断命中的是否是空字符串（因为数据库中不存在的我们都存的是空字符串“”） 为了防止查询到数据库中不存在的值 这里用空值返回
        if (json != null) {
            //是则返回一个错误信息
            return null;
        }

        //不是则未命中
        //4. 不存在（未命中）查询数据库 这里如果是热点数据/构建非常困难的数据需要使用互斥锁进行缓存重建
        //4.1 实现缓存重建
        //4.2 获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id; //这里使用细粒度的锁 只锁住一个店铺
        R r = null;
        try {
            //4.3 判断是否获取成功
            if (!tryLock(lockKey)) {
                //4.4 失败休眠并重试
                Thread.sleep(50);
                //重试
                queryWithMutex(keyPrefix, id, type, dbFallback, expireTime, unit);
            }

            //4.5 获取到锁后判断有无其他线程已经构建 检查缓存
            json = stringRedisTemplate.opsForValue().get(cacheKey);
            //2. 判断是否存在（命中）
            if (StringUtils.hasText(json)) {
                //3. 存在（命中）直接返回
                r = JSONUtil.toBean(json, type);
                return r;
            }

            //4.6 如果依旧没有缓存命中则从数据库重新构建
            Thread.sleep(200);
            r = dbFallback.apply(id);

            //5. 数据库也不存在 返回错误信息
            if (ObjectUtil.isNull(r)) {
                //将空值写入redis 防止缓存穿透
                stringRedisTemplate.opsForValue().set(cacheKey, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }

            //6. 存在，写入缓存
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(r), CACHE_SHOP_TTL, TimeUnit.MINUTES);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //释放互斥锁
            unLock(lockKey);
        }


        //7. 返回数据
        return r;
    }

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }
}
