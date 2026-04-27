package com.linkFlow.user.service;

import com.linkFlow.user.dto.UserCreateDTO;
import com.linkFlow.user.model.User;

public interface UserService {
    /**
     * 创建用户
     */
    Long createUser(UserCreateDTO dto);

    /**
     * 根据ID查询用户
     */
    User getUserById(Long id);

    /**
     * 根据用户名查询用户
     */
    User getUserByUsername(String username);
}