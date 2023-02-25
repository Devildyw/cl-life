package top.devildyw.cl_dianping.core.interceptor;

import cn.hutool.core.util.ObjectUtil;
import org.springframework.web.servlet.HandlerInterceptor;
import top.devildyw.cl_dianping.common.utils.UserHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器 后端拦截验证用户是否登录
 *
 * @author Devil
 * @since 2023-02-20-19:53
 */

public class LoginInterceptor implements HandlerInterceptor {


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1. 判断是否需要拦截（ThreadLocal 中是否有用户）
        if (ObjectUtil.isNull(UserHolder.getUser())) {
            //1.1 未登录，拦截
            response.setStatus(401);
            return false;
        }
        //todo: 消息队列异步UV统计

        return true;
    }
}
