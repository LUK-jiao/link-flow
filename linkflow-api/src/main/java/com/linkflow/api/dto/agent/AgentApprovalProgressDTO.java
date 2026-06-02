package com.linkflow.api.dto.agent;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AgentApprovalProgressDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long campaignId;

    private String campaignName;

    private String campaignStatus;

    private String workflowStatus;

    private String currentNode;

    private List<String> currentApprovers;

    private String shortCode;

    private String shortUrl;
}
