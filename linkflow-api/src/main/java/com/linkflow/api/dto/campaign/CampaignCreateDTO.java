package com.linkflow.api.dto.campaign;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 创建活动 DTO
 */
@Data
public class CampaignCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 活动名称
     */
    private String name;

    /**
     * 活动描述
     */
    private String description;

    /**
     * 活动类型：MARKETING/PROMOTION/EVENT
     */
    private String campaignType;

    /**
     * 创建人用户ID
     */
    private Long creatorUserId;

    /**
     * 活动开始时间
     */
    private Date startTime;

    /**
     * 活动结束时间
     */
    private Date endTime;

    /**
     * 预算
     */
    private BigDecimal budget;

    /**
     * 长链接（用于生成短链）
     */
    private String longUrl;
}