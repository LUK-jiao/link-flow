package com.linkflow.gateway.controller;

import com.linkflow.api.AgentApi;
import com.linkflow.api.dto.agent.AgentChatCommand;
import com.linkflow.api.dto.agent.AgentChatDTO;
import com.linkflow.api.dto.common.Result;
import com.linkflow.gateway.controller.dto.AgentChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/agent")
public class AgentGatewayController {

    private final AgentApi agentApi;

    @PostMapping("/chat")
    public Result<AgentChatDTO> chat(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Username") String username,
            @RequestHeader("X-User-Role") String role,
            @RequestBody AgentChatRequest request
    ) {
        AgentChatCommand command = new AgentChatCommand();
        command.setUserId(userId);
        command.setUsername(username);
        command.setRole(role);
        command.setSessionId(request.sessionId());
        command.setMessage(request.message());
        return agentApi.chat(command);
    }
}
