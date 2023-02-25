package top.devildyw.cl_dianping.core.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.devildyw.cl_dianping.core.entity.UserInfo;
import top.devildyw.cl_dianping.core.mapper.UserInfoMapper;
import top.devildyw.cl_dianping.core.service.IUserInfoService;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author Devil
 * @since 2023-01-11-15:35
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
