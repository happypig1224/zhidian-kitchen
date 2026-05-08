package com.zhidian.interceptor;

import com.zhidian.constant.JwtClaimsConstant;
import com.zhidian.context.BaseContext;
import com.zhidian.properties.JwtProperties;
import com.zhidian.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;



/**
 * jwt令牌校验的拦截器
 */
@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        BaseContext.removeCurrentId();
    }

    /**
     * 校验jwt
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String token = request.getHeader(jwtProperties.getAdminTokenName());
        try{
            log.info("校验token{}",token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
            System.out.println(claims);
            Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
            System.out.println(empId);
            log.info("当前用户id{}",empId);
            BaseContext.setCurrentId(empId);
            return true;
        }catch (Exception e){
            response.setStatus(401);
            return false;
        }
    }
}
