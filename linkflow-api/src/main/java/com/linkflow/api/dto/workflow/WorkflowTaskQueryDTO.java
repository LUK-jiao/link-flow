package com.linkflow.api.dto.workflow;

import lombok.Data;

import java.io.Serializable;

@Data
public class WorkflowTaskQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前审批人ID
     */
    private Long approverId;

    /**
     * 业务类型，例如 CAMPAIGN_APPROVAL
     */
    private String businessType;

    /**
     * 活动类型，例如 MARKETING / PROMOTION / EVENT
     */
    private String campaignType;

    /**
     * 任务定义 Key，例如 level1Approval / level2Approval
     */
    private String taskDefinitionKey;

    /**
     * 页码，从 1 开始
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}
