package com.linkFlow.user.service;

import com.linkflow.api.UserApi;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.user.UserCreateDTO;
import com.linkflow.api.dto.user.UserDTO;
import com.linkFlow.user.mapper.UserMapper;
import com.linkFlow.user.model.User;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * User Dubbo 服务实现
 */
@DubboService
public class UserApiImpl implements UserApi {

    @Autowired
    private UserMapper userMapper;

    @Override
    public Result<Long> createUser(UserCreateDTO dto) {
        // 检查用户名是否已存在
        User existingUser = userMapper.selectAll()
                .stream()
                .filter(u -> u.getUsername().equals(dto.getUsername()))
                .findFirst()
                .orElse(null);

        if (existingUser != null) {
            return Result.fail("用户名已存在");
        }

        User user = new User();
        BeanUtils.copyProperties(dto, user);
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
        User user = userMapper.selectAll()
                .stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);

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
}