package top.devildyw.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import top.devildyw.hmdp.dto.UserDTO;
import top.devildyw.hmdp.entity.User;
import top.devildyw.hmdp.utils.UserHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static top.devildyw.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static top.devildyw.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * 登录拦截器 后端拦截验证用户是否登录
 * @author Devil
 * @since 2023-02-20-19:53
 */

public class LoginInterceptor implements HandlerInterceptor {



    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1. 判断是否需要拦截（ThreadLocal 中是否有用户）
        if (ObjectUtil.isNull(UserHolder.getUser())){
            //1.1 未登录，拦截
            response.setStatus(401);
            return false;
        }
        return true;
    }
}
