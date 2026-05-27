package com.linkflow.gateway.controller.dto;

public record AgentChatRequest(
        String sessionId,
        String message
) {
}
