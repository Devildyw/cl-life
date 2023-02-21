package top.devildyw.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @author Devil
 * @since 2023-02-21-20:43
 */
@Component
public class RedisIdWorker {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final long BEGIN_TIMESTAMP = 1672531200L;

    /**
     * 序列号位数
     */
    private static final int COUNT_BITS = 32;

    /**
     * 全局id生成
     * id为long类型一共64位组成分为3部分
     * 符号位：1bit,永远为0
     * 时间戳：31bit，可以使用69年
     * 序列号：32bit，一秒钟内可以生成2^32个id
     *
     * @param keyPrefix 标识redis自增的key 一般为业务名
     * @return
     */
    public long nextId(String keyPrefix) {
        //1. 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;
        //2. 生成序列号
        //3. 获取当前日期，精确到天 既可以统计那一天的订单量 还可以避免同一个key的情况下 自增超过2^32
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        Long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + date);

        //3. 拼接并返回
        //移位拼接 世界戳向左移动32位 让出序列号的位置，时间戳不会超过31位
        return (timestamp << COUNT_BITS) | count;

    }
}
