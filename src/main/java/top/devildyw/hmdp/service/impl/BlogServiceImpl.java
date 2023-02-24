package top.devildyw.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.data.redis.core.StringRedisTemplate;
import top.devildyw.hmdp.dto.Result;
import top.devildyw.hmdp.dto.UserDTO;
import top.devildyw.hmdp.entity.Blog;
import top.devildyw.hmdp.entity.User;
import top.devildyw.hmdp.mapper.BlogMapper;
import top.devildyw.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.devildyw.hmdp.service.IUserService;
import top.devildyw.hmdp.utils.SystemConstants;
import top.devildyw.hmdp.utils.UserHolder;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static top.devildyw.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;

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

    @Override
    public Result queryBlogById(Long id) {
        //1. 查询 blog
        Blog blog = getById(id);

        if (ObjectUtil.isNull(blog)){
            return Result.fail("blog 不存在");
        }

        //2. 查询 blog 有关的用户信息
        Long authorId = blog.getUserId();
        User user = userService.getById(authorId);
        blog.setIcon(user.getIcon());
        blog.setName(user.getNickName());

        //3. 查询用户是否点赞
        isBlogLiked(blog);

        return Result.ok(blog);
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
}
