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
