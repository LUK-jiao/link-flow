package com.linkflow.api.dto.agent;

import lombok.Data;

import java.io.Serializable;

@Data
public class AgentActionConfirmDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String actionId;

    private String status;

    private String resultText;

    private Long campaignId;
}
