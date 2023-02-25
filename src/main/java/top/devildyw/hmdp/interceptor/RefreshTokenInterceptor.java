package top.devildyw.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import top.devildyw.hmdp.dto.UserDTO;
import top.devildyw.hmdp.utils.UserHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static top.devildyw.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static top.devildyw.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * token刷新拦截器
 * @author Devil
 * @since 2023-02-20-21:22
 */

public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 不做拦截 只做token的获取刷新和获取用户信息
     * @param request current HTTP request
     * @param response current HTTP response
     * @param handler chosen handler to execute, for type and/or instance evaluation
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1. 获取请求头中的token
        String token = request.getHeader("authorization");

        if (!StringUtils.hasText(token)){
            return true;
        }
        //2. 基于Token获取redis中的用户信息
        Map<Object, Object> usermap = stringRedisTemplate.opsForHash().entries(LOGIN_USER_KEY + token);


        //3. 判断用户信息是否存在
        if (usermap.isEmpty()){
            return true;
        }

        //4. 将map转为userHolder对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(usermap, new UserDTO(), false);

        //5. 存在，保存用户信息到 ThreadLocal 方便后续方法拿到用户信息
        UserHolder.saveUser(userDTO);

        //6.刷新token 模拟session的机制
        stringRedisTemplate.expire(LOGIN_USER_KEY+token,LOGIN_USER_TTL, TimeUnit.SECONDS);

        //7. 放行
        return true;
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //及时清理 value 因为ThreadLocal的key是弱引用 一旦没有外部强引用所引用就会被回收，而value是强引用不会被回收所以我们要显式地清空value 防止内存泄露
        UserHolder.removeUser();
    }


}
