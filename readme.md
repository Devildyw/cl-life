# 黑马点评

## 项目结构

数据库表

![image-20230220130422194](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230220130422194.png)

基础架构

![image-20230220130348772](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230220130348772.png)

## 基于 redis session 共享方式实现登录验证

验证码：

![image-20230220202242465](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230220202242465.png)

> key:phone，value:code 

登录：

![image-20230220203112474](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230220203112474.png)

保存用户登录信息，可以使用 String 结构，以 JSON 字符串来保存，比较直观。

![image-20230220202544709](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230220202544709.png)

以 Hash 结构可以将对象中的每个字段独立存储，可以针对单个字段做 CRUD，并且占用内存更少（不用像 JSON 那样保存额外的符号）

校验登录状态

![image-20230220202946962](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230220202946962.png)

**session共享问题**
tomcat session 拷贝缺点：内存浪费，拷贝延迟

redis session 共享方式
满足 数据共享
内存存储
key,value 完美支持

> Redis 代替 session 需要考虑的问题：
>
> * 选择合适的数据结构
> * 选择合适的key
> * 选择合适的存储粒度（只存储不敏感，有用的信息防止浪费内存）

### 登录拦截器优化

![image-20230220211821023](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230220211821023.png)

> 单一的登录拦截器，无法涵盖所有接口（因为有些接口不登录 用户也能看）如果将 token 刷新集成在登录拦截器中会有上述问题，所以将 token 获取刷新抽取出来独立为一个拦截器。

## 缓存
### 缓存的作用
应用缓存是为了解决内存和磁盘读写速度差异过大的问题
* 降低后端负载
* 提高读写效率，降低响应时间
* 减小对磁盘访问的压力

应用软件中常用 redis 作为缓存，因为其完美符合的特性（key value）

### 缓存的成本
* 数据一致性成本（一般对实时性要求比较高的 都不会做缓存，因为会频繁的缓存删除和更新，缓存的内容应当是允许一段时间内的缓存不一致的）
* 代码维护成本（使用缓存会使得代码更加复杂，增加维护成本）
* 运维成本



### 添加 Redis 缓存

这里还没考虑 缓存穿透、缓存雪崩、缓存击穿等问题，只是实现一个简单的缓存

![image-20230221110036245](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230221110036245.png)

### 缓存更新策略

![image-20230221113756558](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230221113756558.png)

内存淘汰是 Redis 默认的一种机制，这种方式的一致性最差，因为不可控所以当内存充裕的时候是不会淘汰数据的（就会导致一直读到旧数据）。

业务场景：

* 低一致性需求：使用内存淘汰机制。例如店铺类型的查询缓存
* 高一致性需求：主动更新，并以超时剔除作为兜底方案。例如店铺详情查询的缓存。



#### 主动更新策略

1. **Cache Aside Pattern***

   由缓存的调用者，在更新数据库的同时更新缓存。

2. **Read/Write Through Pattern**

   缓存与数据库整合为一个服务，由服务来维护一致性，调用者调用该服务，无需关心缓存一致性问题。

3. **Write Behind Caching Pattern**

   调用者只操作缓存，由其他线程异步的将缓存数据持久化到数据库，保证最终一致性。

   

**Cache Aside Pattern**

需要考虑的问题：

删除缓存还是更新缓存？

* 每次更新数据库且更新缓存，无效写操作比较多（不一定每次都有用户来访问）

* 每次更新数据库且删除缓存（让缓存失效），查询时再更新缓存（避免了无效写的情况）

如何保证缓存与数据库操作的同时成功或失败（原子性）？

* 单体系统，将缓存与数据库操作放在一个事务
* 分布式系统，利用 tcc 等分布式事务方案

先操作缓存还是先操作数据库？

* 先删缓存，在操作数据库
* 先操作数据库，再操作缓存



缓存穿透

![image-20230221141224118](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230221141224118.png)

缓存雪崩

 ![image-20230221142108302](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230221142108302.png)

缓存击穿

![image-20230221143007496](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230221143007496.png)

互斥锁方式解决缓存击穿问题

![image-20230221143732266](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230221143732266.png)





![image-20230221152346457](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230221152346457.png)



## Redis 实现秒杀服务

### 全局 ID 生成器（实现雪花算法）

在秒杀服务中如果使用数据库自增ID就可能会出现一些问题

* id 的规律太过明显
* 受表单数据量的限制（数据量大的情况下会进行分表，这样如果使用 数据库自带的自增 id 就会导致 id 重复的情况）

全局 id 需要满足以下几点：

* 唯一性
* 高可用
* 高性能
* 递增性
* 安全性（防止id规律被猜出）

![image-20230221204106295](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230221204106295.png)

ID 组成：

* 符号位：1bit ，永远为0
* 时间戳：31bit，以秒为单位，可以使用69年
* 序列号：32bit，秒内的计数器，支持每秒产生 2^32个不同ID



全局唯一 ID 生成策略

* UUID
* Redis 自增
* snowflake 算法
* 数据库自增（单独做一张表来做全局id生成）

Redis 自增 ID 生成策略：

* 每天一个 key，方便统计订单量
* ID 构造是 时间戳 + Redis 计数器

### 优惠卷秒杀

下单注意两点：

* 秒杀是否开始或结束，如果尚未开始或已经结束则无法下单
* 库存是否充足，不足则无法下单

**秒杀流程**

![image-20230221213827911](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230221213827911.png)

### 超卖问题

在高并发的情况下，可能会出现线程安全问题，这时我们就需要通过加锁来避免

锁分为乐观锁和悲观锁。

在高并发的情况下，使用悲观锁会导致性能太低。

乐观锁更为适合，只是在修改数据之前判断数据在修改期间有没有被其他线程修改。

* 缺点：性能低

乐观锁使用 CAS 法，但不需要考虑 ABA 问题，因为就算出现了 ABA 问题，那也不会影响业务。

* 缺点：存在成功率低的情况（多线程同时访问到相同的变量值，但有一个线程在他们之前修改了这个值，导致这些线程都失败）

优化不需要判断数据与修改前一致的情况，只需要考虑优惠卷是否大于0，就可以减库存

### 一人一单

对于同一个用户对于指定的优惠券只能下一单

但是只是简单地从数据库中查询用户该优惠券的订单时，会出现线程安全问题（多个线程同时访问看到同一个量，发现都为0，就去下单）

解决方案：加锁



### 集群下的线程安全问题

原因就是：因为多集群的情况下，会有不同的JVM实例，导致锁住的对象是不一样的。

解决方案：**分布式锁**

> 分布式锁：满足分布式系统或集群模式下，多进程可见并且互斥的锁。

分布式锁：

* 多进程可见
* 互斥
* 高可用
* 高并发
* 安全性（死锁问题）

常用的分布式锁实现：

![image-20230222110938919](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230222110938919.png)

### 基于 Redis 实现简单的分布式锁

> 原理：`setnx` `setex`

简单分布式锁

获取锁（互斥）：

```bash
#添加锁，NX是互斥，EX是设置超时时间（两者一起写保证原子性） 超时时间事防止业务系统获取锁后宕机无法释放锁的情况。
SET lock thread1 EX 10 NX
#非阻塞的方式 tryLock 尝试成功返回true 失败返回false
```

释放锁：

```
DEL key
```



### 简单的分布式锁高并发下存在的问题

**情况一**：线程1执行业务阻塞超时，锁超时释放，此时线程2执行业务，获取到锁，执行业务；此时线程1阻塞结束执行业务完成，释放了本应该是线程2的锁；线程3到来获取锁，获取成功，执行业务；此时就会出现线程2和线程3并发执行的情况，会出现线程安全问题。

![image-20230222114810318](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230222114810318.png)

**解决方案：**为锁添加线程标识，线程只能释放带有自己标识的锁，释放前先获取锁标识然后判断。

![image-20230222114933506](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230222114933506.png)

----

**情况二**：线程一获取锁标识，判断为自己的准备释放，但是在释放过程中因一些原因阻塞（网络延迟等），线程2进入业务获取到锁，执行；此时线程1阻塞结束释放了锁，导致了线程2的锁被释放，这就会导致后续线程3进入也能获取到锁和线程并发执行。

**原因**：判断锁标识和释放锁是两个操作并不是原子操作，可能存在线程安全问题。

**解决方案**：lua 脚本实现释放锁的原子性。

```lua
-- 锁的key 动态参数 key 存放在 KEYS 数组
local key = KEYS[1]
-- 线程标识 value 存放在 ARGV 数组
local threadId = ARGV[1]
-- 获取锁中的线程标识
local id = redis.call('get',key)
-- 比较线程标识与锁中的标识是否一致
if(id==threadId) then
    -- 锁释放 del key
    return redis.call('del',key);
end
-- 不一致就不释放锁
return 0
```

lua 脚本可以确保 redis 操作的原子性

