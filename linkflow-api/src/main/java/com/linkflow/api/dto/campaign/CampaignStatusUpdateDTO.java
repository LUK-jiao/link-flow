package com.linkflow.api.dto.campaign;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新活动状态 DTO（审批回调使用）
 */
@Data
public class CampaignStatusUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 活动ID
     */
    private Long campaignId;

    /**
     * 新状态：APPROVED/REJECTED
     */
    private String status;

    /**
     * 拒绝原因（REJECTED 时填写）
     */
    private String rejectReason;
}