package com.linkflow.api.dto.agent;

import lombok.Data;

import java.io.Serializable;

@Data
public class AgentActionConfirmCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String role;

    private String actionId;

    private Boolean confirmed;
}
