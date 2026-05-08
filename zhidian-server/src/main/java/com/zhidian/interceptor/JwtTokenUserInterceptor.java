package com.zhidian.interceptor;

import com.zhidian.constant.JwtClaimsConstant;
import com.zhidian.context.BaseContext;
import com.zhidian.properties.JwtProperties;
import com.zhidian.utils.JwtUtil;
import com.zhidian.utils.MemoryCacheUtil;
import com.zhidian.utils.RedisConstant;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.concurrent.TimeUnit;

/**
 * jwt令牌校验的拦截器
 */
@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private MemoryCacheUtil localCache;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        BaseContext.removeCurrentId();
    }

    /**
     * 校验 jwt
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String userIdHeader = request.getHeader("userId");
        log.info("当前用户 id{}", userIdHeader);
        BaseContext.setCurrentId(Long.valueOf(userIdHeader));
        // 秒杀测试
        /*if (request.getRequestURI().contains("/user/seckillVoucher")) {
            String userIdHeader = request.getHeader("userId");
            if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
                response.setStatus(400);
                return false;
            }
            try {
                Long userId = Long.valueOf(userIdHeader);
                BaseContext.setCurrentId(userId);
                return true;
            } catch (NumberFormatException e) {
                response.setStatus(400);
                return false;
            }
        }
        String token = request.getHeader(jwtProperties.getUserTokenName());
        try {
            log.info("校验 token{}", token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            System.out.println(claims);
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            log.info("当前用户 id{}", userId);
            BaseContext.setCurrentId(userId);
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }*/
        return true;
    }
}