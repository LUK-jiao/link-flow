package com.linkflow.api.dto.workflow;

import lombok.Data;

import java.io.Serializable;

/**
 * 审批通过请求 DTO
 */
@Data
public class ApprovalRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 审批意见
     */
    private String comment;
}