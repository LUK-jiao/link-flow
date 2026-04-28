package com.linkflow.api.dto.campaign;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 活动 DTO
 */
@Data
public class CampaignDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String description;

    private String campaignType;

    private String status;

    private Long creatorUserId;

    private Date startTime;

    private Date endTime;

    private BigDecimal budget;

    private String rejectReason;

    private String shortCode;

    private String longUrl;

    private Date createTime;

    private Date updateTime;
}