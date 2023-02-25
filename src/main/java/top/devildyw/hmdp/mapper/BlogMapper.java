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
 * @author Devil
 * @since 2023-01-11-15:30
 */
public interface BlogMapper extends BaseMapper<Blog> {

    List<Blog> selectBatchIdsOrderByIds(List<Long> blogIds);
}
