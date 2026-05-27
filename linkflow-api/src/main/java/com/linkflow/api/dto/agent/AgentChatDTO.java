package com.linkflow.api.dto.agent;

import lombok.Data;

import java.io.Serializable;

@Data
public class AgentChatDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sessionId;

    private String messageId;

    private String type;

    private String text;
}
