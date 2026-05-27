package com.linkflow.gateway.controller;

import com.linkflow.api.UserApi;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.user.UserLoginDTO;
import com.linkflow.api.dto.user.UserLoginResultDTO;
import com.linkflow.gateway.auth.JwtTokenService;
import com.linkflow.gateway.controller.dto.AuthLoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthGatewayController {

    private final UserApi userApi;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/login")
    public Result<AuthLoginResponse> login(@RequestBody UserLoginDTO dto) {
        Result<UserLoginResultDTO> loginResult = userApi.login(dto);
        if (!loginResult.isSuccess()) {
            return Result.fail(loginResult.getCode(), loginResult.getMessage());
        }

        UserLoginResultDTO user = loginResult.getData();
        String token = jwtTokenService.generateToken(user);
        AuthLoginResponse response = new AuthLoginResponse(
                token,
                user.getUserId(),
                user.getUsername(),
                user.getRole()
        );
        return Result.success(response);
    }
}
