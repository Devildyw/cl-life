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
