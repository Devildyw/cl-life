package top.devildyw.cl_dianping.core.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.devildyw.cl_dianping.common.DTO.UserDTO;
import top.devildyw.cl_dianping.core.entity.User;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author Devil
 * @since 2023-01-11-15:30
 */
public interface UserMapper extends BaseMapper<User> {

    List<UserDTO> selectBatchIdsOrderByIds(List<Long> ids);
}
