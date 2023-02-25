package top.devildyw.hmdp.service;

import top.devildyw.hmdp.dto.Result;
import top.devildyw.hmdp.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Devil
 * @since 2023-01-11-15:35
 */
public interface IFollowService extends IService<Follow> {

    /**
     * 实现关注和取关功能
     * @param followUserId
     * @param isFollow
     * @return
     */
    Result follow(Long followUserId, Boolean isFollow);

    /**
     * 判断是否关注
     * @param followUserId
     * @return
     */
    Result isFollow(Long followUserId);

    /**
     * 得到与目标用户的共同关注列表
     * @param followUserId
     * @return
     */
    Result followCommons(Long followUserId);

    /**
     * 获取用户粉丝的id列表
     * @param id
     * @return
     */
    List<Long> getUserIdsByFollowUserId(Long id);
}
