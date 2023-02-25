package top.devildyw.cl_dianping.common.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author Devil
 * @since 2023-02-02-11:18
 */

public class SimpleRedisLock implements ILock {


    /**
     * 锁的前缀
     */
    private static final String KEY_PREFIX = "lock:";
    /**
     * 线程标识id
     */
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    private StringRedisTemplate stringRedisTemplate;
    /**
     * 锁的名称
     */
    private String name;


    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }


    @Override
    public boolean tryLock(long timeoutSec) {
        //获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        //获取锁
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag); //防止拆箱null值情况
    }

    /**
     * 首先获取锁标识
     * 判断锁标识是否与当前线程的标识一致
     * 一致才释放锁
     */
    @Override
    public void unLock() {
        //锁的线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();

        //调用lua 脚本
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT
                , Collections.singletonList(KEY_PREFIX + name)
                , threadId);
    }
}
