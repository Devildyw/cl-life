package top.devildyw.hmdp.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import top.devildyw.hmdp.dto.Result;
import top.devildyw.hmdp.dto.ScrollResult;
import top.devildyw.hmdp.dto.UserDTO;
import top.devildyw.hmdp.entity.Blog;
import top.devildyw.hmdp.entity.User;
import top.devildyw.hmdp.mapper.BlogMapper;
import top.devildyw.hmdp.service.IBlogService;
import top.devildyw.hmdp.service.IFollowService;
import top.devildyw.hmdp.service.IUserService;
import top.devildyw.hmdp.utils.SystemConstants;
import top.devildyw.hmdp.utils.UserHolder;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static top.devildyw.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;
import static top.devildyw.hmdp.utils.RedisConstants.FEED_KEY;
import static top.devildyw.hmdp.utils.SystemConstants.DEFAULT_PAGE_SIZE;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IFollowService followService;

    @Override
    public Result queryBlogById(Long id) {
        //1. 查询 blog
        Blog blog = getById(id);

        if (ObjectUtil.isNull(blog)) {
            return Result.fail("blog 不存在");
        }

        //2. 查询 blog 有关的用户信息
        queryBlogUser(blog);

        //3. 查询用户是否点赞
        isBlogLiked(blog);

        return Result.ok(blog);
    }

    /**
     * 查询 blog 有关的用户信息
     * @param blog
     */
    private void queryBlogUser(Blog blog) {
        Long authorId = blog.getUserId();
        User user = userService.getById(authorId);
        blog.setIcon(user.getIcon());
        blog.setName(user.getNickName());
    }

    @Override
    public Result queryHotBlog(Integer current) {

        // 1. 按照喜欢度分页查询出blog
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        //2.  获取当前页数据
        List<Blog> records = page.getRecords();
        //3. 批量查询用户 防止循环查表提升系统吞吐量
        List<Long> ids = records.stream().map(Blog::getUserId).distinct().collect(Collectors.toList());
        List<User> users = userService.queryBatch(ids);

        //4. 当用户列表不为空时才拼装
        if (!users.isEmpty()){
            Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, (u) -> u));
            records = records.stream().map((item) -> {
                User user = userMap.get(item.getUserId());
                if (ObjectUtil.isNotNull(user)){
                    //封装blog对于用户相关数据
                    item.setName(user.getNickName());
                    item.setIcon(user.getIcon());
                    //5. 判断用户是否点赞
                    isBlogLiked(item);
                }
                return item;
            }).collect(Collectors.toList());
        }


        return Result.ok(records);
    }

    private void isBlogLiked(Blog blog){
        //1.获取用户id
        UserDTO user = UserHolder.getUser();
        if (ObjectUtil.isNull(user)){
            //2. 如果用户未登录 直接返回
            return;
        }
        //3. 拼接 key
        String likeKey = BLOG_LIKED_KEY+blog.getId();
        //4. 判断用户是否点赞
        Double score = stringRedisTemplate.opsForZSet().score(likeKey, user.getId().toString());
        blog.setIsLike(ObjectUtil.isNotNull(score));
        return;
    }

    /**
     * todo: 使用消息队列实现异步将点赞数更新到数据库
     * @param id
     * @return
     */
    @Override
    public Result likeBlog(Long id) {

        //1. 获取登录用户
        Long userId = UserHolder.getUser().getId();
        //2. 判断当前用户是否已经点赞
        String likeKey = BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(likeKey, userId.toString());
        //3. 如果未点赞，可以点赞
        if (ObjectUtil.isNull(score)){
            //3.1 数据库点赞数 +1
            boolean success = update().setSql("liked = liked+1").eq("id", id).update();
            //3.2 保存用户到 Redis 的set集合
            if (success){
                stringRedisTemplate.opsForZSet().add(likeKey,userId.toString(),System.currentTimeMillis());
            }
        }else{
            //4. 如果已经点赞
            //4.1 数据库点赞数减一
            boolean success = update().setSql("liked = liked-1").eq("id", id).update();
            //4.2 将用户从 Redis set 集合中删除
            if (success){
                stringRedisTemplate.opsForZSet().remove(likeKey,userId.toString());
            }
        }

        //5. 返回结果
        return Result.ok();
    }

    /**
     * 获取按点赞时间排序得到最先点赞的前五个用户
     * @param id
     * @return
     */
    @Override
    public Result queryBlogLikes(Long id) {
        String likeKey = BLOG_LIKED_KEY+id;
        //1. 查询top5的点赞用户 zrang key 0 4
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(likeKey, 0, 4);

        if (top5==null||top5.isEmpty()){
            return Result.ok(Collections.emptyList());
        }

        //2. 将set中的用户id 封装成一个 Long 型的 list
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        //3. 按照id顺序查询得到用户列表
        List<UserDTO> userDTOs = userService.queryListByOrder(ids);
        //4. 返回
        return Result.ok(userDTOs);
    }

    @Override
    public Result saveBlog(Blog blog) {
        //1. 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        //2. 保存探店博文
        boolean isSuccess = save(blog);

        //3. 保存失败
        if (!isSuccess) {
            return Result.fail("发布博文失败");
        }

        //4. 保存成功
        //4.1 获取时间戳
        long current = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        //5. 获取用户的粉丝id列表 select id from tb_follow where follow_user_id = ?
        List<Long> userIds = followService.getUserIdsByFollowUserId(user.getId());

        //todo:这里的推送可以使用 消息队列进行异步发送
        //5.1 判断粉丝列表是否为空
        if (userIds == null || userIds.isEmpty()) {
            //5.2 为空则不发送直接返回
            return Result.ok(blog.getId());
        }

        //6. 使用 Redis 的 ZSet 实现 feed 流用户的收件箱
        //6.1 将 blog 的 id 推送给粉丝
        userIds.forEach((id) -> {
            String key = FEED_KEY + id;
            //用户id为key 博文id为值 发布博文的时间戳为score
            stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), current);
        });


        // 返回id
        return Result.ok(blog.getId());
    }


    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        //1. 获取用户id
        Long userId = UserHolder.getUser().getId();

        //2. 查询收件箱 ZREVRANGEBYSCORE key Max Min LIMIT offset count 根据时间戳降序获取
        String key = FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .rangeByScoreWithScores(key, 0, max, offset, DEFAULT_PAGE_SIZE);

        //3. 判空
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        //4. 解析数据得到 blogId min(这次获取数据中的最小时间戳 方便下次获取作为起始位置) offset 下次获取时的偏移量 之所以变化是为了跳过时间戳相同的且上次获取过的数据
        long minTime = 0;
        int newOffset = 1;
        ArrayList<Long> blogIds = new ArrayList<>(typedTuples.size());
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
        List<Blog> blogs = getBlogsByOrderByBlogIds(blogIds);

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

    private List<Blog> getBlogsByOrderByBlogIds(ArrayList<Long> blogIds) {
        return baseMapper.selectBlogsBatchOrderByBlogsId(blogIds);
    }
}
