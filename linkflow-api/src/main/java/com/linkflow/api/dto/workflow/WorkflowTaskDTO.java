package com.linkflow.api.dto.workflow;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class WorkflowTaskDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String taskId;

    private String taskName;

    private String taskDefinitionKey;

    private String processInstanceId;

    private Long businessKey;

    private String businessType;

    private String campaignType;

    private Long approverId;

    private Date createTime;
}
