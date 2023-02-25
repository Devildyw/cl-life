package top.devildyw.cl_dianping.core.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.devildyw.cl_dianping.core.entity.Follow;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author Devil
 * @since 2023-01-11-15:30
 */
public interface FollowMapper extends BaseMapper<Follow> {

    List<Long> selectUserIdsByFollowUserId(Long followUserId);
}
