package com.linkflow.campaign.event;

import lombok.Data;

import java.io.Serializable;

@Data
public class CampaignApprovedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long campaignId;

    private String longUrl;
}
