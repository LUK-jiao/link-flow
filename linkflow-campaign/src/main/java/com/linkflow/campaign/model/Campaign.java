package com.linkflow.campaign.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Campaign {
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

    private Date createTime;

    private Date updateTime;
}