package com.linkflow.user.service.impl;

import com.linkflow.api.UserApi;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.user.UserCreateDTO;
import com.linkflow.api.dto.user.UserDTO;
import com.linkflow.api.dto.user.UserLoginDTO;
import com.linkflow.api.dto.user.UserLoginResultDTO;
import com.linkflow.user.mapper.UserMapper;
import com.linkflow.user.model.User;
import com.linkflow.user.service.PasswordService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * User Dubbo 服务实现
 */
@DubboService
@Service
public class UserApiImpl implements UserApi {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordService passwordService;

    @Override
    public Result<UserLoginResultDTO> login(UserLoginDTO dto) {
        if (dto == null || isBlank(dto.getUsername()) || isBlank(dto.getPassword())) {
            return Result.fail(401, "用户名或密码错误");
        }

        User user = userMapper.selectByUsername(dto.getUsername());
        if (user == null) {
            return Result.fail(401, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            return Result.fail(403, "用户已禁用");
        }
        if (!passwordService.matches(dto.getPassword(), user.getPassword())) {
            return Result.fail(401, "用户名或密码错误");
        }
        if (!passwordService.isEncoded(user.getPassword())) {
            user.setPassword(passwordService.encode(dto.getPassword()));
            user.setUpdateTime(new Date());
            userMapper.updateByPrimaryKey(user);
        }

        UserLoginResultDTO result = new UserLoginResultDTO();
        result.setUserId(user.getId());
        result.setUsername(user.getUsername());
        result.setRole(user.getRole());
        result.setStatus(user.getStatus());
        return Result.success(result);
    }

    @Override
    public Result<Long> createUser(UserCreateDTO dto) {
        // 检查用户名是否已存在（避免全表扫描）
        User existingUser = userMapper.selectByUsername(dto.getUsername());
        if (existingUser != null) {
            return Result.fail("用户名已存在");
        }

        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setPassword(passwordService.encode(dto.getPassword()));
        user.setRole(dto.getRole() != null ? dto.getRole() : "USER");
        user.setStatus((byte) 1);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());

        userMapper.insert(user);
        return Result.success(user.getId());
    }

    @Override
    public Result<UserDTO> getUserById(Long id) {
        User user = userMapper.selectByPrimaryKey(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        
        UserDTO dto = convertToDTO(user);
        return Result.success(dto);
    }

    @Override
    public Result<UserDTO> getUserByUsername(String username) {
        User user = userMapper.selectByUsername(username);

        if (user == null) {
            return Result.fail("用户不存在");
        }

        UserDTO dto = convertToDTO(user);
        return Result.success(dto);
    }

    @Override
    public Result<Void> updateUserStatus(Long id, Byte status) {
        User user = userMapper.selectByPrimaryKey(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        user.setStatus(status);
        user.setUpdateTime(new Date());
        userMapper.updateByPrimaryKey(user);
        
        return Result.success();
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
