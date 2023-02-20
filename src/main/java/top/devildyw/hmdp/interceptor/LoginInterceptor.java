package top.devildyw.hmdp.interceptor;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import top.devildyw.hmdp.dto.UserDTO;
import top.devildyw.hmdp.entity.User;
import top.devildyw.hmdp.utils.UserHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 登录拦截器 后端拦截验证用户是否登录
 * @author Devil
 * @since 2023-02-20-19:53
 */
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1. 获取session
        HttpSession session = request.getSession();
        //2. 获取session中的用户信息
        Object user = session.getAttribute("user");

        //3. 判断用户信息是否存在，不存在则未登录 拦截
        if (user==null){
            //用户未登录 拦截 返回状态码401
            response.setStatus(401);
            return false;
        }

        //4. 存在，保存用户信息到 ThreadLocal 方便后续方法拿到用户信息
        UserHolder.saveUser((UserDTO) user);

        //5. 放行
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //及时清理 value 因为ThreadLocal的key是弱引用 一旦没有外部强引用所引用就会被回收，而value是强引用不会被回收所以我们要显式地清空value 防止内存泄露
        UserHolder.removeUser();
    }
}
