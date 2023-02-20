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
