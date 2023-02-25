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
 * @author 虎哥
 * @since 2021-12-22
 */
public interface UserMapper extends BaseMapper<User> {

    List<UserDTO> selectBatchIdsOrderByIds(List<Long> ids);
}
