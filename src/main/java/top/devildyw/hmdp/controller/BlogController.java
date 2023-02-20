package top.devildyw.hmdp.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.data.redis.core.StringRedisTemplate;
import top.devildyw.hmdp.dto.Result;
import top.devildyw.hmdp.dto.UserDTO;
import top.devildyw.hmdp.entity.Blog;
import top.devildyw.hmdp.entity.User;
import top.devildyw.hmdp.utils.SystemConstants;
import top.devildyw.hmdp.utils.UserHolder;
import top.devildyw.hmdp.service.IBlogService;
import top.devildyw.hmdp.service.IUserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IBlogService blogService;
    @Resource
    private IUserService userService;


    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        blogService.save(blog);
        // 返回id
        return Result.ok(blog.getId());
    }

    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        // 修改点赞数量
        blogService.update()
                .setSql("liked = liked + 1").eq("id", id).update();
        return Result.ok();
    }

    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 1. 按照喜欢度分页查询出blog
        Page<Blog> page = blogService.query()
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
                }
                return item;
            }).collect(Collectors.toList());
        }


        return Result.ok(records);
    }
}
