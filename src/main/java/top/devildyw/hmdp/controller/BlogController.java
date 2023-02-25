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
 * @author Devil
 * @since 2023-01-11-15:27
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
        return blogService.saveBlog(blog);
    }

    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        return blogService.likeBlog(id);
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
        return blogService.queryHotBlog(current);
    }

    @GetMapping("{id}")
    public Result queryBlog(@PathVariable("id") Long id){
        return blogService.queryBlogById(id);
    }

    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable("id") Long id){
        return blogService.queryBlogLikes(id);
    }

    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("id") Long id) {
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", id).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    /**
     * 查询用户关注用户发布的blog 因为发布者已经通过feed的方式将blog的id推送到了粉丝的信箱中 所以我们只需要从中获取即可
     * @param max
     * @param offset
     * @return
     */
    @GetMapping("/of/follow")
    public Result queryBlogFollow(
                @RequestParam("lastId") Long max, @RequestParam(value = "offset",defaultValue = "0") Integer offset){
        return blogService.queryBlogOfFollow(max,offset);
    }
}
