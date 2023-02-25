package top.devildyw.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.devildyw.hmdp.dto.LoginFormDTO;
import top.devildyw.hmdp.dto.Result;
import top.devildyw.hmdp.dto.UserDTO;
import top.devildyw.hmdp.entity.User;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

    /**
     * 发送短信验证码，验证手机号，生成验证码 再将验证码存入session
     * @param phone
     * @param session
     * @return
     */
    Result sendCode(String phone, HttpSession session);

    /**
     * 根据手机号和验证码登录 校验手机号，再校验验证码，最后将用户登录信息存入session中
     * @param loginForm
     * @param session
     * @return
     */
    Result login(LoginFormDTO loginForm, HttpSession session);

    /**
     * 批量查询
     * @param ids
     * @return
     */
    List<User> queryBatch(List<Long> ids);

    /**
     * 通过给出的 id 顺序返回列表
     * @param ids
     * @return
     */
    List<UserDTO> queryListByOrder(List<Long> ids);

    /**
     * 借助 Redis 的 BitMap 实现用户签到
     * @return
     */
    Result sign();

    /**
     * 借助 Redis 的 BitMap 结构实现对用户签到的统计
     * @return
     */
    Result signCount();

}
