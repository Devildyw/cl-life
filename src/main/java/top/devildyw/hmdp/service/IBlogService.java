package top.devildyw.hmdp.service;

import top.devildyw.hmdp.dto.Result;
import top.devildyw.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {

    /**
     * 查询blog和发布用户的基本信息
     * @param id
     * @return
     */
    Result queryBlogById(Long id);

    /**
     * 查询热点文章
     * @param current
     * @return
     */
    Result queryHotBlog(Integer current);

    /**
     * 通过Blog 类中 isLike 字段标识blog是否被点赞
     * 通过 Redis 的set结构保存点赞记录，如果不存在则点赞成功，如果存在则点赞数减一且删除用户
     * @param id
     * @return
     */
    Result likeBlog(Long id);

    /**
     * 借助 Redis 的 SortedSet 结构获取 blog 按照点赞时间排序得到的前五个用户的基本信息
     * @param id
     * @return
     */
    Result queryBlogLikes(Long id);
}
