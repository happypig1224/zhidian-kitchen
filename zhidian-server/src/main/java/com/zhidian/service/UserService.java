package com.zhidian.service;

import com.zhidian.dto.UserLoginDTO;
import com.zhidian.entity.User;

public interface UserService {
    User wxLogin(UserLoginDTO userLoginDTO);
}
