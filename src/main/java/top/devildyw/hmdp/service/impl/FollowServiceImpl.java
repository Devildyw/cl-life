package top.devildyw.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import top.devildyw.hmdp.dto.Result;
import top.devildyw.hmdp.dto.UserDTO;
import top.devildyw.hmdp.entity.Follow;
import top.devildyw.hmdp.mapper.FollowMapper;
import top.devildyw.hmdp.service.IFollowService;
import top.devildyw.hmdp.service.IUserService;
import top.devildyw.hmdp.utils.UserHolder;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static top.devildyw.hmdp.utils.RedisConstants.FOLLOW_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author Devildyw
 * @since 2022-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;


    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        //1. 获取当前登录用户 id
        Long userId = UserHolder.getUser().getId();

        //key
        String followKey = FOLLOW_KEY + userId;

        //2. 判断是关注还是取关
        if (isFollow) {
            //2.1 关注新增数据
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean success = save(follow);
            //2.2 数据保存成功后 将关注信息保存到 Redis 的 set 集合中 方便做共同关注
            if (success) {
                stringRedisTemplate.opsForSet().add(followKey, followUserId.toString());
            }
        } else {
            //3. 取关，需要将该用户关注的用户那条数据删除
            boolean success = remove(new LambdaQueryWrapper<Follow>().eq(Follow::getUserId, userId).eq(Follow::getFollowUserId, followUserId));
            if (success) {
                //3.1 数据库中删除了，不要忘了删除 Redis 中的数据 保证数据一致性
                stringRedisTemplate.opsForSet().remove(followKey, followUserId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        //1. 获取当前登录用户id
        Long userId = UserHolder.getUser().getId();

        //key
        String followKey = FOLLOW_KEY + userId;

        //2. 查询当前登录用户是否对 followUserId 的用户关注
        Boolean success = stringRedisTemplate.opsForSet().isMember(followKey, followUserId.toString());

        return Result.ok(BooleanUtil.isTrue(success));

    }

    @Override
    public Result followCommons(Long followUserId) {
        //1. 获取当前用户 id
        Long userId = UserHolder.getUser().getId();

        //2. key
        String followKey1 = FOLLOW_KEY + userId;
        String followKey2 = FOLLOW_KEY + followUserId;

        //3. 两个集合做交集得到共同关注用户的id
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(followKey1, followKey2);

        if (intersect == null || intersect.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        //4. 解析id集合
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());

        //5. 查询共同关注用户的基本信息
        List<UserDTO> userDTOS = userService.queryListByOrder(ids);

        //6. 返回
        return Result.ok(userDTOS);


    }
}
