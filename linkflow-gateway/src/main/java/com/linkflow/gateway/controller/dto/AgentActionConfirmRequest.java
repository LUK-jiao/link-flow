package com.linkflow.gateway.controller.dto;

public record AgentActionConfirmRequest(
        String actionId,
        Boolean confirmed
) {
}
