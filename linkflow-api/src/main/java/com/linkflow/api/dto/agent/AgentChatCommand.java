package com.linkflow.api.dto.agent;

import lombok.Data;

import java.io.Serializable;

@Data
public class AgentChatCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String role;

    private String sessionId;

    private String message;
}
