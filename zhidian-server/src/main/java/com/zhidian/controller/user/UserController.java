package com.zhidian.controller.user;

import com.zhidian.constant.JwtClaimsConstant;
import com.zhidian.dto.UserLoginDTO;
import com.zhidian.entity.User;
import com.zhidian.properties.JwtProperties;
import com.zhidian.result.Result;
import com.zhidian.service.UserService;
import com.zhidian.utils.JwtUtil;
import com.zhidian.vo.UserLoginVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/user/user")
@Tag(name = "用户相关接口")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtProperties jwtProperties;

    @PostMapping("/login")
    public Result<UserLoginVO> wxLogin(@RequestBody UserLoginDTO userLoginDTO){
        log.info("微信登录:{}",userLoginDTO.getCode());
        //微信登录
        User user = userService.wxLogin(userLoginDTO);
        Map<String,Object> claims=new HashMap<>();
        //为微信用户生成jwt令牌
        claims.put(JwtClaimsConstant.USER_ID,user.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(), claims);
        UserLoginVO userLoginVO=UserLoginVO.builder()
                .id(user.getId())
                .openid(user.getOpenid())
                .token(token).build();
        return Result.success(userLoginVO);
    }
}
