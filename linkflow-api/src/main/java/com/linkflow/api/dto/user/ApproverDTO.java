package com.linkflow.api.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 审批人 DTO
 */
@Data
public class ApproverDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 配置ID
     */
    private Long id;

    /**
     * 活动类型，取值见 com.linkflow.api.enums.CampaignTypeEnum
     */
    private String campaignType;

    /**
     * 审批人用户ID
     */
    private Long approverId;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 审批级别
     */
    private Integer approverLevel;
}
