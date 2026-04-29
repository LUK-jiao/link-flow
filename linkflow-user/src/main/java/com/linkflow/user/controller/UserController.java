package com.linkflow.user.controller;

import com.linkflow.user.common.Result;
import com.linkflow.user.dto.UserCreateDTO;
import com.linkflow.user.model.User;
import com.linkflow.user.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 创建用户
     * POST /user/create
     */
    @PostMapping("/create")
    public Result<Long> createUser(@RequestBody UserCreateDTO dto) {
        try {
            Long userId = userService.createUser(dto);
            return Result.success(userId);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 根据ID查询用户
     * GET /user/{id}
     */
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        return Result.success(user);
    }
}