package com.linkflow.workflow.model;

import lombok.Data;

import java.util.Date;

/**
 * 审批记录
 */
@Data
public class ApprovalRecord {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 活动ID
     */
    private Long campaignId;

    /**
     * 工作流实例ID
     */
    private Long workflowInstanceId;

    /**
     * Flowable 任务ID
     */
    private String taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 动作：APPROVED/REJECTED
     */
    private String action;

    /**
     * 审批意见
     */
    private String comment;

    /**
     * 审批时间
     */
    private Date approveTime;

    /**
     * 创建时间
     */
    private Date createTime;
}