package com.linkflow.gateway.auth;

public record CurrentUser(
        Long userId,
        String username,
        String role
) {
}
