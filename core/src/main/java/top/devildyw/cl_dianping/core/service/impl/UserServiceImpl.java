package top.devildyw.cl_dianping.core.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import top.devildyw.cl_dianping.common.DTO.LoginFormDTO;
import top.devildyw.cl_dianping.common.DTO.Result;
import top.devildyw.cl_dianping.common.DTO.UserDTO;
import top.devildyw.cl_dianping.common.utils.RegexUtils;
import top.devildyw.cl_dianping.common.utils.UserHolder;
import top.devildyw.cl_dianping.core.entity.User;
import top.devildyw.cl_dianping.core.mapper.UserMapper;
import top.devildyw.cl_dianping.core.service.IUserService;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static top.devildyw.cl_dianping.common.constants.RedisConstants.*;
import static top.devildyw.cl_dianping.common.constants.SystemConstants.USER_NICK_NAME_PREFIX;


/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author Devil
 * @since 2023-01-11-15:35
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1. 验证手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            //2. 校验失败返回错误信息
            return Result.fail("手机号格式错误");
        }
        //3. 生成6位验证码
        String code = RandomUtil.randomNumbers(6);

        //4. 保存验证码到session
        //使用 redis 存储session 解决集群session共享问题 2分钟过期
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        //5. 发送验证码
        //todo：发送验证码 Mq解耦
        log.debug("验证码发送成功，验证码:{}", code);

        // 返回ok
        return Result.ok();


    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {

        //1. 验证手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            //2. 校验失败返回错误信息
            return Result.fail("手机号格式错误");
        }

        //3. 校验验证码
        String code = loginForm.getCode();
        //3.1 从redis中取出验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);

        if (cacheCode == null || !cacheCode.equals(code)) {
            //4. 验证码不一致
            return Result.fail("验证码错误");
        }

        //一致 则验证码验证成功删除redis中的key value
        stringRedisTemplate.delete(LOGIN_CODE_KEY + phone);

        //5. 一致根据手机号查询用户
        User user = baseMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));

        //6. 判断用户是否存在
        if (ObjectUtil.isNull(user)) {
            //7. 如果不存在 创建新用户并保存
            user = createUserWithPhone(phone);
        }


        //8. 保存用户登录信息到redis中 方便后续复用 剔除用户敏感信息，剔除无用信息（高并发下防止内存压力过大）
        //8.2 随机生成 token，作为登录令牌
        String token = UUID.randomUUID().toString(true);
        //8.3 将user对象转为 Hash存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        userDTO.setToken(token);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        //是否忽略空字段值
                        .setIgnoreNullValue(true)
                        //将值转换为String 防止出现直接转转不了的情况所以这里直接电泳 toString() 方法
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        //8.4 存储
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        //8.5 设置session 有效期
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.SECONDS);
        //返回ok
        return Result.ok(token);
    }

    @Override
    public List<User> queryBatch(List<Long> ids) {
        List<User> users = new ArrayList<>();
        if (!ids.isEmpty()) {
            users = baseMapper.selectBatchIds(ids);
        }
        return users;
    }

    @Override
    public List<UserDTO> queryListByOrder(List<Long> ids) {

        return baseMapper.selectBatchIdsOrderByIds(ids);
    }

    @Override
    public Result sign() {
        //1. 获取用户id
        Long userId = UserHolder.getUser().getId();
        //2. 用户当前时间年月
        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DateTimeFormatter.ofPattern(":yyyy:MM"));

        //3. 拼装 key sign:userId:year:month
        String key = USER_SIGN_KEY + userId + date;
        //3.1 计算今天是该月的第几天
        int day = now.getDayOfMonth();

        //4. 判断用户是否签到
        Boolean isSign = stringRedisTemplate.opsForValue().getBit(key, day - 1);
        if (BooleanUtil.isTrue(isSign)) {
            //4.1 如果用户已经签到 返回已签到信息
            return Result.fail("你已经签过到了!");
        }

        //5. 如果没有签到则签到
        stringRedisTemplate.opsForValue().setBit(key, day - 1, true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        //1. 获取用户id
        Long userId = UserHolder.getUser().getId();
        //2. 用户当前时间年月
        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DateTimeFormatter.ofPattern(":yyyy:MM"));

        //3. 拼装 key sign:userId:year:month
        String key = USER_SIGN_KEY + userId + date;
        //3.1 计算今天是该月的第几天
        int day = now.getDayOfMonth();

        //4. 判断今天是否签到
        Boolean isSign = stringRedisTemplate.opsForValue().getBit(key, day - 1);
        if (BooleanUtil.isFalse(isSign)) {
            //4.1 如果今天未签到则只统计从前一天开始的连续签到数
            --day;
        }
        //4.2 如果签到了就从今天开始

        //5. 获取到今天位置的签到记录的十进制格式 BITFIELD sign:userId:year:MM GET u[dayOfMonth] 0
        List<Long> result = stringRedisTemplate.opsForValue()
                .bitField(key,
                        BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(day)).valueAt(0));

        //5.1 判空
        if (result == null || result.isEmpty()) {
            //5.2 如果为空则代表该月没有签到记录返回0
            return Result.ok(0);
        }

        //6. 获取该月签到记录
        Long record = result.get(0);
        //6.1 判断记录是否有值
        if (ObjectUtil.isNull(record) || record == 0) {
            //6.2 如果记录为空或者为0 也没有记录
            return Result.ok(0);
        }

        //7. 获取用户该月的连续签到记录
        int count = 0;
        //7.1 通过位运算来计算该月用户连续签到 count += record&1; record>>>=1;
        while (true) {
            //7.2 通过1与每一位做位运算 会得到该位本身 如果为0则代表没有连续上
            if ((record & 1) == 0) {
                break;
            }
            //7.3 记录连续签到天数
            count++;
            //7.4 record无符号右移实现遍历效果
            record >>>= 1;
        }

        return Result.ok(count);
    }

    @Override
    public void logout() {
        //1. 获取用户id
        UserDTO user = UserHolder.getUser();
        //2. tokenkey
        String tokenKey = LOGIN_USER_KEY + user.getToken();
        //2. 删除用户在Redis中的session
        stringRedisTemplate.opsForHash().getOperations().delete(tokenKey);
    }


    /**
     * 根据手机号创建新用户并保存到数据库
     *
     * @param phone
     */
    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        //1. 随机昵称
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));

        //2. 保存用户 mp自动生成方法
        save(user);

        return user;
    }


}
