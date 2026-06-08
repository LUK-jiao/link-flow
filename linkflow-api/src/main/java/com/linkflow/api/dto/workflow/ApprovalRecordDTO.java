package com.linkflow.api.dto.workflow;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ApprovalRecordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long campaignId;

    private String processInstanceId;

    private String taskId;

    private String taskName;

    private Long approverId;

    private String approverName;

    private String action;

    private String comment;

    private String rejectReason;

    private Date approveTime;

    private Date createTime;
}
