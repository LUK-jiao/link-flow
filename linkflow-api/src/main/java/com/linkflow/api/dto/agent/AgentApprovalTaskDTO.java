package com.linkflow.api.dto.agent;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class AgentApprovalTaskDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long campaignId;

    private String campaignName;

    private String campaignStatus;

    private String campaignType;

    private String taskId;

    private String taskName;

    private String processInstanceId;

    private Long approverId;

    private Date createTime;
}
