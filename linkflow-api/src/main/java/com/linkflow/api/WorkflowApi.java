package com.linkflow.api;

import com.linkflow.api.dto.workflow.ApprovalCallbackDTO;
import com.linkflow.api.dto.workflow.WorkflowStartDTO;
import com.linkflow.api.dto.workflow.WorkflowStatusDTO;
import com.linkflow.api.dto.common.Result;

/**
 * Workflow 服务接口
 */
public interface WorkflowApi {

    /**
     * 发起审批流程
     */
    Result<String> startApprovalProcess(WorkflowStartDTO dto);

    /**
     * 查询流程状态
     */
    Result<WorkflowStatusDTO> getProcessStatus(String processInstanceId);

    /**
     * 根据业务键查询流程状态
     */
    Result<WorkflowStatusDTO> getProcessStatusByBusinessKey(Long businessKey);

    /**
     * 取消流程
     */
    Result<Void> cancelProcess(String processInstanceId);

    /**
     * 审批通过
     */
    Result<Void> approve(String processInstanceId, Long approverId, String comment);

    /**
     * 审批拒绝
     */
    Result<Void> reject(String processInstanceId, Long approverId, String comment, String rejectReason);
}