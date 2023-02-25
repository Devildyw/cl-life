package top.devildyw.hmdp.mapper;

import top.devildyw.hmdp.dto.UserDTO;
import top.devildyw.hmdp.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Devil
 * @since 2023-01-11-15:30
 */
public interface UserMapper extends BaseMapper<User> {

    List<UserDTO> selectBatchIdsOrderByIds(List<Long> ids);
}
