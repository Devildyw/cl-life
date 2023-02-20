package top.devildyw.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import top.devildyw.hmdp.dto.LoginFormDTO;
import top.devildyw.hmdp.dto.Result;
import top.devildyw.hmdp.dto.UserDTO;
import top.devildyw.hmdp.entity.User;
import top.devildyw.hmdp.mapper.UserMapper;
import top.devildyw.hmdp.service.IUserService;
import org.springframework.stereotype.Service;
import top.devildyw.hmdp.utils.RegexUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static top.devildyw.hmdp.utils.RedisConstants.*;
import static top.devildyw.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1. 验证手机号
        if (RegexUtils.isPhoneInvalid(phone)){
            //2. 校验失败返回错误信息
            return Result.fail("手机号格式错误");
        }
        //3. 生成6位验证码
        String code = RandomUtil.randomNumbers(6);

        //4. 保存验证码到session
        //使用 redis 存储session 解决集群session共享问题 2分钟过期
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

        //5. 发送验证码
        //todo：发送验证码
        log.debug("验证码发送成功，验证码:{}",code);

        // 返回ok
        return Result.ok();


    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {

        //1. 验证手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)){
            //2. 校验失败返回错误信息
            return Result.fail("手机号格式错误");
        }

        //3. 校验验证码
        String code = loginForm.getCode();
        //3.1 从redis中取出验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);

        if (cacheCode==null||!cacheCode.equals(code)){
            //4. 验证码不一致
            return Result.fail("验证码错误");
        }

        //一致 则验证码验证成功删除redis中的key value
        stringRedisTemplate.delete(LOGIN_CODE_KEY+phone);

        //5. 一致根据手机号查询用户
        User user = baseMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));

        //6. 判断用户是否存在
        if (ObjectUtil.isNull(user)){
            //7. 如果不存在 创建新用户并保存
            user = createUserWithPhone(phone);
        }


        //8. 保存用户登录信息到redis中 方便后续复用 剔除用户敏感信息，剔除无用信息（高并发下防止内存压力过大）
        //8.2 随机生成 token，作为登录令牌
        String token = UUID.randomUUID().toString(true);
        //8.3 将user对象转为 Hash存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        //是否忽略空字段值
                        .setIgnoreNullValue(true)
                        //将值转换为String 防止出现直接转转不了的情况所以这里直接电泳 toString() 方法
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        //8.4 存储
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);
        //8.5 设置session 有效期
        stringRedisTemplate.expire(tokenKey,LOGIN_USER_TTL,TimeUnit.SECONDS);
        //返回ok
        return Result.ok(token);
    }

    @Override
    public List<User> queryBatch(List<Long> ids) {
        List<User> users = new ArrayList<>();
        if (!ids.isEmpty()){
            users = baseMapper.selectBatchIds(ids);
        }
        return users;
    }


    /**
     * 根据手机号创建新用户并保存到数据库
     * @param phone
     */
    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        //1. 随机昵称
        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(10));

        //2. 保存用户 mp自动生成方法
        save(user);

        return user;
    }
}
