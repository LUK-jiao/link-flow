package com.linkflow.api;

import com.linkflow.api.dto.user.UserDTO;
import com.linkflow.api.dto.user.UserCreateDTO;
import com.linkflow.api.dto.common.Result;

/**
 * User 服务接口
 */
public interface UserApi {

    /**
     * 创建用户
     */
    Result<Long> createUser(UserCreateDTO dto);

    /**
     * 根据ID查询用户
     */
    Result<UserDTO> getUserById(Long id);

    /**
     * 根据用户名查询用户
     */
    Result<UserDTO> getUserByUsername(String username);

    /**
     * 更新用户状态
     */
    Result<Void> updateUserStatus(Long id, Byte status);
}