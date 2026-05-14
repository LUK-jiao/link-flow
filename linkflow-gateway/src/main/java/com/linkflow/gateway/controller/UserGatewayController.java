package com.linkflow.gateway.controller;

import com.linkflow.api.UserApi;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.user.UserCreateDTO;
import com.linkflow.api.dto.user.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "用户相关接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserGatewayController {

    private final UserApi userApi;

    @Operation(summary = "创建用户")
    @PostMapping
    public Result<Long> createUser(@RequestBody UserCreateDTO dto) {
        return userApi.createUser(dto);
    }

    @Operation(summary = "根据用户ID查询用户")
    @GetMapping("/{id}")
    public Result<UserDTO> getUserById(
            @Parameter(description = "用户ID", required = true)
            @PathVariable("id") Long id) {
        return userApi.getUserById(id);
    }

    @Operation(summary = "根据用户名查询用户")
    @GetMapping("/by-username")
    public Result<UserDTO> getUserByUsername(
            @Parameter(description = "用户名", required = true)
            @RequestParam("username") String username) {
        return userApi.getUserByUsername(username);
    }

    @Operation(summary = "更新用户状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateUserStatus(
            @Parameter(description = "用户ID", required = true)
            @PathVariable("id") Long id,
            @Parameter(description = "状态值", required = true)
            @RequestParam("status") Byte status) {
        return userApi.updateUserStatus(id, status);
    }
}
