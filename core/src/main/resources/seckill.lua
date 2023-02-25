-- 1. 参数列表
-- 1.1 优惠券 id
local voucherId = ARGV[1]
-- 1.2 用户 id
local userId = ARGV[2]
-- 1.3 订单id
--local orderId = ARGV[3]

-- 2. 数据 key
-- 2.1 库存 key
local stockKey = KEYS[1]
-- 2.2 订单 key
local orderKey = KEYS[2]



-- 3. 脚本业务
-- 3.1 判断库存是否充足 get stockKey
if (tonumber(redis.call('get', stockKey)) <= 0) then
    -- 3.2 库存不足，返回 1
    return 1
end
-- 3.3 判断用户是否下过单， SISMEMBER orderKey userId
if (redis.call('SISMEMBER', orderKey, userId) == 1) then
    -- 3.4 用户已经下过单了，返回2
    return 2
end
-- 3.5 扣 redis 中的库存 incrby stockKey -1
redis.call('incrby', stockKey, -1)
-- 3.6 下单(保存用户下单记录) sadd orderKey userId
redis.call('sadd', orderKey, userId)
-- -- 3.7 发送消息到队列中，XADD stream.orders * k1 v1 k2 v2 .... fixme:借助rocketmq实现
--redis.call('xadd','stream.orders','*','userId',userId,'voucherId',voucherId,'id',orderId)
-- 3.8 成功返回 0
return 0
