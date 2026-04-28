package com.linkflow.api.dto.workflow;

import lombok.Data;

import java.io.Serializable;

/**
 * 审批回调 DTO
 */
@Data
public class ApprovalCallbackDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务键（campaignId）
     */
    private Long businessKey;

    /**
     * 审批结果：APPROVED/REJECTED
     */
    private String approvalResult;

    /**
     * 拒绝原因
     */
    private String rejectReason;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批人姓名
     */
    private String approverName;
}