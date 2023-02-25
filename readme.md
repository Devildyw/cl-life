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



### 简单分布式锁存在的问题

* **不可重入**：例如线程1进入方法1获取到了分布式锁，调用了方法2，方法2内又想要获取分布式锁，就会导致线程死锁。
* **不可重试**：对于某些业务，可能会要求获取不到锁就重试阻塞到获取为止，而不是获取不到就返回。
* **超时释放**：锁超时释放虽然可以避免死锁，但如果业务执行耗时较长，也会导致锁释放，存在安全隐患。
* **主从一致性**：集群情况下，主从同步存在延迟，如果在同步时主节点宕机了或者是发生了脑裂，就会导致从无法获得主节点上的锁信息，导致**锁失效**。



### 使用 Redisson 解决简单分布式锁的问题

Redisson 提供的不仅仅是分布式锁的解决方案，还提供了多种分布式 Java 对象功能十分强大。



Redisson 可重入锁原理

使用了 Redis 的 Hash 结构，key 是锁的标识，hash中的key为 线程标识，value 是锁的可重入次数。

![image-20230223132554525](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230223132554525.png)

Redisson 底层对于锁的操作都是基于 lua 脚本的，保障了操作的原子性。

**对于可重入锁**：redisson 底层通过lua 脚本来完成上述的流程，将锁作为 key ，线程标识以 hash结构存储，value则为锁的可重入次数，每次释放锁判断锁的可重入次数是否为 0 为 0 则删除锁。

**对于获取不到锁的重试机制**：并不是一直轮询而是采用了 Redis 的 Pub/Sub 模型，当锁被释放了，就会 pub 消息，使得 sub 的线程执行轮询操作，减少了一直轮询对 CPU 的开销

**超时释放**问题：引入了 WatchDog 机制，底层是由 Java 代码维护的，在业务获取到锁执行的过程中，看门狗会一直为锁续期。因为是 java 代码维护的，所以当执行业务代码的主机宕机了，就不会续期了，即满足了宕机锁超时释放的要求；也解决了因为业务执行时间较长导致锁超时释放的缺点。（看门狗续期还是通过 lua 脚本实现的）。

![image-20230223181952927](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230223181952927.png)

> 单体 Redis 实现分布式锁，性能肯定是够用的，但是如果部署 Redis 的主机宕机就会导致分布式锁服务不可用，因此可以采用集群部署。
>
> 主从一致性问题：但是集群部署也存在问题，一般 Redis 集群采用主从的结构，从节点会与主节点进行数据同步，这个操作是有延迟的；如果主节点宕机了或者发生了脑裂**从节点还没来得及同步**，集群中的哨兵就会选出一个新的 Leader，这时锁就会丢失，导致出现并发安全问题。
>
> ![image-20230223181504652](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230223181504652.png)

**主从一致性问题解决：**采用部署多主多从的 Redis 节点，每次获取锁都需要多个主节点同时获取到锁才算成功，如果有一台主节点宕机了而导致获取不到锁，但其他主节点可以获取到锁，这也是算获取失败。提供给系统一个获取失败的信息（相比于单体），基本不会出现线程安全问题（相比与单主多从）。

![image-20230223181810617](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230223181810617.png)

> 使用 Redisson 的 `redissonClient.getMultiLock()` 方法实现。
>
> 底层是通过迭代指定多个锁来获取锁，每个锁获取的流程与单体一致。



## Redis 秒杀优化

当前存在的性能问题，我们的秒杀服务需要经过：

1. 查询优惠券
2. 判断秒杀库存
3. 查询订单
4. 校验一人一单
5. 删减库存
6. 创建订单的操作

6个步骤有4个都是需要操作数据库的，也就是说用户要完成一个秒杀需要等到这六个操作都执行完才返回，一旦用户量增加就会导致服务器吞吐量下降。

![image-20230223202657521](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230223202657521.png)

为了提高用户的并发量，就需要对该代码进行优化；

**优化思路**：将与数据库有交互的地方进行拆分，开额外的线程去执行，异步去执行数据库操作。

**方案**：使用 Redis 存储优惠券的库存（判断秒杀库存 String），还有存储优惠券对应的消费的用户（一人一单 set）；这样当一个秒杀请求到来，直接在 Redis 中判断是否满足下单条件，如果满足通过异步线程去修改数据库，如果不满足就直接返回，因为通过异步线程去操作，所以用户请求基本不经过数据库，大大提高性能。

![image-20230223203351472](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230223203351472.png)

![image-20230223203659183](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230223203659183.png)

在 Redis 中判断用户是否有下单资格的操作交给 lua 脚本去做，满足操作的原子性。



### 实现

* 在新增秒杀优惠券的时候，将优惠券保存到 Redis 中。
* 基于 Lua 脚本，判断秒杀库存，一人一单，决定用户是否抢购成功（秒杀资格）
* 如果抢购成功，将优惠券 id 和用户 id 封装后存入阻塞队列
* 开启线程任务，不断从阻塞队列中获取信息，实现异步下单



### 使用阻塞队列的缺点

* 阻塞队列内存限制安全
* 数据安全问题（宕机后任务丢失）

### 使用消息队列（Message Queue）

* 消息队列：存储和管理消息，也被称为消息代理
* 生产者：发送消息到消息队列
* 消费者：从消息队列获取消息并处理消息

![image-20230223221653661](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230223221653661.png)

优点：

* 独立服务，不受 JVM 内存影响
* 持久化消息，数据安全有保证
* 可以满足消息有序性



### 使用 Redis 实现消息队列

Redis 提供了三种方式实现消息队列

* list 结构：基于List结构模拟消息队列
* Pub/Sub：基本的点对点消息模型
* Stream：比较完整的消息队列模型

#### 基于 list 实现消息队列

Redis的list数据结构是一个双向链表，很容易模拟出队列效果。

队列是入口和出口不在一边，因此我们可以利用：LPUSH 结合 RPOP、或者 RPUSH 结合 LPOP来实现。不过要注意的是，当队列中没有消息时RPOP或LPOP操作会返回null，并不像JVM的阻塞队列那样会阻塞并等待消息。因此这里应该使用BRPOP或者BLPOP来实现阻塞效果。

![image-20230223222236498](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230223222236498.png)

缺点：

* 无法避免消息丢失（当消费者拉取到消息后，如果消费者宕机，那么这个消息就丢失了）
* 只支持单消费者模式/不支持重复消费



#### 基于 Pub/Sub 的消息队列

PubSub（发布订阅）是Redis2.0版本引入的消息传递模型。顾名思义，消费者可以订阅一个或多个channel，生产者向对应channel发送消息后，所有订阅者都能收到相关消息。

`SUBSCRIBE channel [channel]` ：订阅一个或多个频道

`PUBLISH channel msg` ：向一个频道发送消息

`PSUBSCRIBE pattern[pattern] `：订阅与pattern格式匹配的所有频道（通配符）

![image-20230223222703076](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230223222703076.png)

优点：

* 采用发布订阅模型，支持多生产、多消费

缺点：

* **不支持数据持久化**
* 无法避免消息丢失（消费者要是没启动就发送了，那就会导致接收不到生产者之前发送的消息）
* 消息堆积有上限，超出时数据丢失。



#### 基于 Stream 结构实现消息队列

Redis 5.0 引入的一种新数据类型，可以实现一个功能非常完善的消息队列。

发送消息的指令：

![image-20230223224054890](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230223224054890.png)

读取消息的指令：

![image-20230223224202066](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230223224202066.png)

使用循环来完成 XREAD 阻塞方式，读取消息

```java
while(true){
    //尝试获取锁，最多阻塞两秒
    Object msg = redis.execute("XREAD COUNT 1 BLOCK 2000 STREAMS users $");
    if(msg == null){
        continue;
    }
    
    //处理消息
    handleMessage(msg);
}
```

> 当我们指定 $ 为起始位置时，代表我们读取最新的消息，如果我们处理一条消息的过程中，又有超过一条以上的消息到达队列，则下次获取时也只能获取到最新的一条，就会出现**漏读消息**的问题。

**XREAD:**

单消费者的模式去读取消息

优点：

* 消息可回溯
* 一个消息可以被多个消费者读取
* 可以阻塞读取

缺点：

* 有消息漏读的风险
* 不支持消息堆积



**XGROUPREAD:**

消费者组的模式去读取消息

> 消费者组：将多个消费者划分到一个组中，监听同一个队列。
>
> 特点：
>
> * 消息分流：队列中的消息会分流给组内的不同消费者，从而加快消息的处理速度。
> * 消息标识：消费者组会维护一个标识，记录最后一个被处理的消息，即使消费者宕机重启，也会从标识之后的读取消息，确保每一个消息都会被消费。
> * 消息确认：消费者获取消息后，消息处于 pending 状态，并存入 pending-list，当处理完成后需要通过 sack 来确认消息，标记消息已处理，才会从 pending-list 中移除。

优点：

* 消息可回溯
* 可以多消费者争抢消息，加快消费速度
* 可以阻塞读取
* 没有消息漏读的风险
* 有消息确认机制，确保消息至少被消费一次



### 总结

![image-20230224161145302](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230224161145302.png)



## Redis 实现点赞

### 点赞

需求：

* 同一个用户只能点赞一次，再次点赞则取消点赞
* 如果当前用户已经点赞，则点赞按钮高亮显示（前端实现，根据字段 Blog 类的 isLike 属性）

实现：

* 通过在 Blog 类中添加 isLiked 字段，在查询 Blog 列表时，供给前端实现高亮。
* 将点赞过的用户存入到 Redis set 结构中，通过这样来set中是否有当前用户来判断是否点赞，如果没有就将用户添加到 set 中。

### 点赞排行榜

需求：按照点赞时间先后排序，返回 TOP 5 的用户

![image-20230224161238177](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230224161238177.png)

为了实现排序的功能，又要实现点赞一个用户只能点一次的性质，可以使用 Redis 的 SortedSet 结构

> SortedSet 的与 Set 不同没有直接提供 `isMember` 的方法来判断元素是否存在。但是可以通过 zscore 方法获取一个元素的分数来判断是否元素存在。



### Redis 实现查询共同关注

使用 Redis 的 set 集合记录每个用户的关注列表，当需要查询用户的关注列表时，直接对两个集合进行求交集即可。



## Redis 实现关注推送功能

**Feed 流**

关注推送也叫 Feed 流，直译为**投喂**。为用户持续的提供 “沉浸式” 的体验，通过无线下拉刷新获取新的信息。

![image-20230224182307446](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230224182307446.png)

Feed 流有两种常见模式：

* Timeline：不做内容筛选，简单的按照内容发布时间排序，常用于好友或关注。例如朋友圈。

  * 优点：信息全面，不会有缺失。并且实现也相对简单
  * 缺点：信息噪音比较多，用户不一定感兴趣，内容获取效率低。

* 智能排序：利用智能算法屏蔽掉违规的、用户不感兴趣的内容。推送用户感兴趣的信息来吸引用户

  * 优点：提供给用户感兴趣的信息，用户黏度很高，容易沉迷
  * 缺点：如果算法不精确，可能起到反作用。

  

本项目基于 Timeline 模式实现 feed 流（主要基于关注的好友来做 feed 流）。

### Timeline

**拉模式：**也叫读扩散

实现：发送者和关注者都维护一个信箱，一个用于发送，一个用于收件；当发送者将信发送到发件箱后，关注者就会去拉取发送人信箱中的信然后对时间做排序。

![image-20230224192611583](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230224192611583.png)

优点：节省内存空间（只用保存发送人的那一条信，关注者查看时自行拉去）

缺点：如果用户关注了很多用户，就会导致拉取的很慢，使得延迟高。

---

**推模式：**写扩散

实现：发送者不在维护自己的信箱，每次发送消息都将消息写到订阅者的信箱中。

![image-20230224192858458](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230224192858458.png)

优点：用户获取信息延迟低（当用户关注了很多用户的时候，还是可以很快获取到信息。）

缺点：如果用户被很多用户关注会导致写很多，使得写空间太大。

---

**推拉结合模式：**也叫做读写混合，兼具推和拉两种模式

实现：根据用户的活跃情况来判断用户是否为活跃用户，如果是活跃用户，则采用推模式；如果是普通用户则采用拉模式。因为活跃用户会经常获取信息，采用推模式可以使得获取信息延迟更低；普通用户较活跃用户获取信息没那么频繁，如果采用推会导致信息浪费（推了，但是用户却没有看浪费了空间）。

![image-20230224193612659](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230224193612659.png)

----



**三种模式对比**

![image-20230224193638198](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230224193638198.png)



本系统用户量少，且推模式实现简单所以这里使用推模式



### 推模式实现 Feed 流

当用户发送 blog 的时候同时将 blog 的id 推送到用户粉丝的 ZSet 信箱中（因为要按照时间先后来获取，时间戳作为 score）中。

等到粉丝用户想要获取到他的关注列表内的blog时，直接从信箱（ZSet）查询即可。

### Feed 流分页问题

Feed 流中的数据会不断更新，所以数据的角标也在变化，所以传统的分页方式不能使用。

传统分页方式是根据当前数据（page size），但是 Feed 流中的数据是不断变化的而且是按时间戳排序获取，这样会导致我们每次去获取分页数据时可能会出现重复元素的情况，

![image-20230225124432760](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230225124432760.png)

> 所以我们需要使用另一种方式，每次都从上一次的最后一个位置的下标开始取（ZSet 我们按照时间戳取），所以需要维护一个最小下标。
>
> 又因为时间戳在高并发的情况下是可能重复的，所以这里还要记录一个偏移量。用来规避上次获取到的元素（因为是按照上次维护最小时间戳开始查，如果不记录从最小时间戳的第几个开始查就会导致查出上次获取到的元素）

```java
public Result queryBlogOfFollow(Long max, Integer offset) {
    //1. 获取用户id
    Long userId = UserHolder.getUser().getId();

    //2. 查询收件箱 ZREVRANGEBYSCORE key Max Min LIMIT offset count 根据时间戳降序获取
    String key = FEED_KEY + userId;
    Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
            .reverseRangeByScoreWithScores(key, 0, max, offset, DEFAULT_PAGE_SIZE);

    //3. 判空
    if (typedTuples == null || typedTuples.isEmpty()) {
        return Result.ok(Collections.emptyList());
    }

    //4. 解析数据得到 blogId min(这次获取数据中的最小时间戳 方便下次获取作为起始位置) offset 下次获取时的偏移量 之所以变化是为了跳过时间戳相同的且上次获取过的数据
    long minTime = 0;
    int newOffset = 1;
    List<Long> blogIds = new ArrayList<>(typedTuples.size());
    for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
        //4.1 获取 blogId
        blogIds.add(Long.valueOf(typedTuple.getValue()));
        //4.2 获取时间戳
        long time = typedTuple.getScore().longValue();

        //4.3 获得最小值 且根据最小值出现的次数取更新 newOffset ZSet是有序的
        if (time == minTime) {
            newOffset++;
        } else {
            minTime = time;
            newOffset = 1;
        }
    }

    //5. 有了blogId 需要查出 blog 的信息 包括用户信息 当前用户是否点赞等信息
    List<Blog> blogs = getListOrderByBlogIds(blogIds);

    //todo:优化
    for (Blog blog : blogs) {
        //5.1 获取blog的作者信息
        queryBlogUser(blog);
        //5.2判断当前登录用户是否点赞
        isBlogLiked(blog);
    }

    //6. 封装并返回
    return Result.ok(new ScrollResult(blogs, minTime, newOffset));

}
```

## Redis 实现附近商铺

通过 Redis 的 GEO 数据结构来存储商铺的 id 和 商铺的地理位置（在新增商铺/修改商铺的时候）。

> key：“shop:geo:typeId”，因为一般用户都是按商铺类型来查询商铺，所以这里直接按typeId存储

通过这些数据在用户想要获取附近商铺的信息时，前端获取到用户的地理位置然后传到后端，通过 GEO 的GEOSEARCH 来获取按矩形范围/圆形范围内的。



## Redis 实现签到功能

使用 Redis 的 BitMap 来存储用户一个月内的签到信息。

因为 BitMap 是一个比特数组，所以每一位的两种状态刚好对应了签到的两种状态。

> key:”sign:userId:year:month”

记录一个用户一个月的签到情况最多只需要使用 32bit = 4Byte 一年也才48Byte 所以是很节省空间的。如果使用 mysql 记录就会导致存储一天的签到记录会使用 近 22 个字节

![image-20230225132723871](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230225132723871.png)

但其实实现签到的方式有很多，也可能只记录昨天/今天/后天的签到情况，这样的话使用 String 也是可以的，到时使用过期时间来删除key即可。

```java
@Override
public Result sign() {
    //1. 获取用户id
    Long userId = UserHolder.getUser().getId();
    //2. 用户当前时间年月
    LocalDateTime now = LocalDateTime.now();
    String date = now.format(DateTimeFormatter.ofPattern(":yyyy:MM"));

    //3. 拼装 key sign:userId:year:month
    String key = USER_SIGN_KEY+userId+date;
    //3.1 计算今天是该月的第几天
    int day = now.getDayOfMonth();

    //4. 判断用户是否签到
    Boolean isSign = stringRedisTemplate.opsForValue().getBit(key, day-1);
    if (BooleanUtil.isTrue(isSign)){
        //4.1 如果用户已经签到 返回已签到信息
        return Result.fail("你已经签过到了!");
    }

    //5. 如果没有签到则签到
    stringRedisTemplate.opsForValue().setBit(key,day-1,true);
    return Result.ok();
}
```

 ### 实现签到统计功能

![image-20230225133202433](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230225133202433.png)

通过获取到用户该月到该天的签到次数，然后从后往前与1做与运算（与1与运算得到是它本身，查找连续签到），如果为0跳出循环。

> 如何实现遍历，通过移位来实现。
>
> ![image-20230225133152707](https://ding-blog.oss-cn-chengdu.aliyuncs.com/images/image-20230225133152707.png)



## Redis 实现 UV 统计

> UV：全称Unique Visitor，也叫独立访客量，是指通过互联网访问、浏览这个网页的自然人。1天内同一个用户多次访问该网站，只记录1次。

> PV：全称Page View，也叫页面访问量或点击量，用户每访问网站的一个页面，记录1次PV，用户多次打开页面，则记录多次PV。往往用来衡量网站的流量。

**HyperLoglog：**

Hyperloglog(HLL) 是从 Loglog 算法派生的概率算法，用于确定非常大的集合的基数，而不需要存储其所有值。

> Redis 中的 HLL 是基于string结构实现的，单个 HLL 的内存永远小于 16kb ，内存占用低的令人发指！作为代价，其测量结果是概率性的，有小于 0.81％ 的误差。不过对于UV统计来说，这完全可以忽略。

所以使用 HyperLoglog 来统计系统每天的 UV 量是很不错的选择，因为 HyperLoglog 的一个 key 的内存大小不会超过 16kb，使用它来存储虽然有误差，但是如果千万用户访问的情况下只需要消耗 16kb 就能统计还是十分不错的，如果对数据不允许有误差还是不适合。

HyperLogLog的作用：

* 做海量数据的统计工作

HyperLogLog的优点：

* 内存占用极低性能非常好

HyperLogLog的缺点：

* 有一定的误差

> 统计后的数据一般用在管理端。



# TODO：

* 使用 rocketMQ 代替 Redis Stream 

* 循环插入 redis 的操作使用 pipeline 批量插入

* sa-token 整合做鉴权

  
