package com.linkflow.workflow.service;

import com.linkflow.api.dto.workflow.WorkflowStartDTO;
import com.linkflow.api.dto.workflow.WorkflowStatusDTO;

/**
 * 工作流服务接口
 */
public interface WorkflowService {

    /**
     * 启动审批流程
     * @param dto 流程启动参数
     * @return 流程实例ID
     */
    String startProcess(WorkflowStartDTO dto);

    /**
     * 获取流程状态
     * @param processInstanceId 流程实例ID
     * @return 流程状态
     */
    WorkflowStatusDTO getProcessStatus(String processInstanceId);

    /**
     * 根据业务键获取流程状态
     * @param businessKey 业务键
     * @return 流程状态
     */
    WorkflowStatusDTO getProcessStatusByBusinessKey(Long businessKey);

    /**
     * 取消流程
     * @param processInstanceId 流程实例ID
     */
    void cancelProcess(String processInstanceId);

    /**
     * 审批通过
     * @param taskId 任务ID
     * @param approverId 审批人ID
     * @param comment 审批意见
     */
    void approve(String taskId, Long approverId, String comment);

    /**
     * 审批拒绝
     * @param taskId 任务ID
     * @param approverId 审批人ID
     * @param comment 审批意见
     * @param rejectReason 拒绝原因
     */
    void reject(String taskId, Long approverId, String comment, String rejectReason);
}