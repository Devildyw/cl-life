package top.devildyw.cl_dianping.core.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.devildyw.cl_dianping.core.entity.Blog;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author Devil
 * @since 2023-01-11-15:30
 */
public interface BlogMapper extends BaseMapper<Blog> {

    List<Blog> selectBatchIdsOrderByIds(List<Long> blogIds);
}
