package com.linkflow.api.dto.workflow;

import lombok.Data;

import java.io.Serializable;

/**
 * 发起审批流程 DTO
 */
@Data
public class WorkflowStartDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务键（如 campaignId）
     */
    private Long businessKey;

    /**
     * 业务类型：CAMPAIGN_APPROVAL
     */
    private String businessType;

    /**
     * 活动类型（用于查找审批人）
     */
    private String campaignType;

    /**
     * 发起人ID
     */
    private Long initiatorId;
}