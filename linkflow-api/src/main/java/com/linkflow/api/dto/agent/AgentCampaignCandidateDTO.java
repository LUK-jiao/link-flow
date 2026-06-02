package com.linkflow.api.dto.agent;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class AgentCampaignCandidateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long campaignId;

    private String campaignName;

    private String campaignStatus;

    private Date createTime;
}
