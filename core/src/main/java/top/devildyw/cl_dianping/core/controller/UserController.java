package top.devildyw.cl_dianping.core.controller;


import cn.hutool.core.bean.BeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.devildyw.cl_dianping.common.DTO.LoginFormDTO;
import top.devildyw.cl_dianping.common.DTO.Result;
import top.devildyw.cl_dianping.common.DTO.UserDTO;
import top.devildyw.cl_dianping.common.utils.UserHolder;
import top.devildyw.cl_dianping.core.entity.User;
import top.devildyw.cl_dianping.core.entity.UserInfo;
import top.devildyw.cl_dianping.core.service.IUserInfoService;
import top.devildyw.cl_dianping.core.service.IUserService;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author Devil
 * @since 2023-01-11-15:27
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        //发送短信验证码并保存验证码
        return userService.sendCode(phone, session);
    }

    /**
     * 登录功能
     *
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result login(@Validated @RequestBody LoginFormDTO loginForm, HttpSession session) {
        //实现登录功能
        return userService.login(loginForm, session);
    }

    /**
     * 登出功能
     *
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout() {
        userService.logout();
        return Result.ok();
    }

    @GetMapping("/me")
    public Result me() {
        // 获取当前登录的用户并返回
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId) {
        // 查询详情
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 返回
        return Result.ok(userDTO);
    }


    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId) {
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }

    /**
     * 用户签到
     *
     * @return
     */
    @PostMapping("/sign")
    public Result sign() {
        return userService.sign();
    }


    @GetMapping("/sign/count")
    public Result signCount() {
        return userService.signCount();
    }
}
