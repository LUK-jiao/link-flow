package com.linkflow.api.dto.campaign;

import lombok.Data;

import java.io.Serializable;

/**
 * 活动查询 DTO
 */
@Data
public class CampaignQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 创建人ID
     */
    private Long creatorUserId;

    /**
     * 活动类型
     */
    private String campaignType;

    /**
     * 状态
     */
    private String status;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}