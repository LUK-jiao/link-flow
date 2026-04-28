package com.linkflow.api.dto.workflow;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程状态 DTO
 */
@Data
public class WorkflowStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 业务键
     */
    private Long businessKey;

    /**
     * 状态：RUNNING/COMPLETED/CANCELLED
     */
    private String status;

    /**
     * 当前处理人
     */
    private String currentAssignee;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;
}