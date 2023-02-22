-- -- 锁的key 动态参数 key 存放在 KEYS 数组
-- local key = KEYS[1]
-- -- 线程标识 value 存放在 ARGV 数组
-- local threadId = ARGV[1]
-- -- 获取锁中的线程标识
-- local id = redis.call('get',key)
-- -- 比较线程标识与锁中的标识是否一致
-- if(id==threadId) then
--     -- 锁释放 del key
--     return redis.call('del',key);
-- end
-- -- 不一致就不释放锁
-- return 0

if(redis.call('get',KEYS[1])==ARGV[1]) then
    return redis.call('del',KEYS[1])
end
return 0
