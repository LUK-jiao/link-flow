package com.linkflow.workflow.model;

import lombok.Data;

import java.util.Date;

@Data
public class ApprovalRecord {
    private Long id;

    private Long campaignId;

    private String processInstanceId;

    private String taskId;

    private String taskName;

    private Long approverId;

    private String approverName;

    private String action;

    private String comment;

    private Date approveTime;

    private Date createTime;
}