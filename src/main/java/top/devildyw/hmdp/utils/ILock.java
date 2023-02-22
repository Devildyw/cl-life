package top.devildyw.hmdp.utils;

/**
 * 锁的接口规范锁必须实现 tryLock 和 unLock 方法
 * @author Devil
 * @since 2023-02-21-19:08
 */
public interface ILock {

    /**
     * 尝试获取锁(非阻塞模式)
     * @param timeoutSec 锁持有的超时时间，过期后自动释放
     * @return true 代表获取锁成功，false 代表获取锁失败
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     */
    void unLock();
}
