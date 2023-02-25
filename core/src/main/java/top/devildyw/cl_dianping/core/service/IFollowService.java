package top.devildyw.cl_dianping.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.devildyw.cl_dianping.common.DTO.Result;
import top.devildyw.cl_dianping.core.entity.Follow;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author Devil
 * @since 2023-01-11-15:35
 */
public interface IFollowService extends IService<Follow> {

    /**
     * 实现关注和取关功能
     *
     * @param followUserId
     * @param isFollow
     * @return
     */
    Result follow(Long followUserId, Boolean isFollow);

    /**
     * 判断是否关注
     *
     * @param followUserId
     * @return
     */
    Result isFollow(Long followUserId);

    /**
     * 得到与目标用户的共同关注列表
     *
     * @param followUserId
     * @return
     */
    Result followCommons(Long followUserId);

    /**
     * 获取用户粉丝的id列表
     *
     * @param id
     * @return
     */
    List<Long> getUserIdsByFollowUserId(Long id);
}
