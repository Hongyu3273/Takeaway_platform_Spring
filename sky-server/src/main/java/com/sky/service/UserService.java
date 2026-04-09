package com.sky.service;
import com.sky.entity.User;
import com.sky.dto.UserLoginDTO;

public interface UserService {


    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    User wxLogin(UserLoginDTO userLoginDTO);
}
