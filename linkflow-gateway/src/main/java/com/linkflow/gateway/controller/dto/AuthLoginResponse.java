package com.linkflow.gateway.controller.dto;

public record AuthLoginResponse(
        String token,
        Long userId,
        String username,
        String role
) {
}
