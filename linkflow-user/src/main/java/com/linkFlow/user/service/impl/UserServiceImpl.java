package com.linkFlow.user.service.impl;

import com.linkFlow.user.dto.UserCreateDTO;
import com.linkFlow.user.mapper.UserMapper;
import com.linkFlow.user.model.User;
import com.linkFlow.user.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public Long createUser(UserCreateDTO dto) {
        // 检查用户名是否已存在
        User existingUser = userMapper.selectAll()
                .stream()
                .filter(u -> u.getUsername().equals(dto.getUsername()))
                .findFirst()
                .orElse(null);

        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword()); // 实际项目应该加密
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRole(dto.getRole() != null ? dto.getRole() : "USER");
        user.setStatus((byte) 1);

        userMapper.insert(user);
        return user.getId();
    }

    @Override
    public User getUserById(Long id) {
        return userMapper.selectByPrimaryKey(id);
    }

    @Override
    public User getUserByUsername(String username) {
        return userMapper.selectAll()
                .stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }
}