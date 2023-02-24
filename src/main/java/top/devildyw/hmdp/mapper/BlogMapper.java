package top.devildyw.hmdp.mapper;

import top.devildyw.hmdp.dto.UserDTO;
import top.devildyw.hmdp.entity.Blog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.devildyw.hmdp.entity.User;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface BlogMapper extends BaseMapper<Blog> {

    List<Blog> selectBlogsBatchOrderByBlogsId(ArrayList<Long> blogIds);
}
