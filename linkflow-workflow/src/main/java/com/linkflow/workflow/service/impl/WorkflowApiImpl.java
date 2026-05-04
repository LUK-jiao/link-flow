package com.linkflow.workflow.service.impl;

import com.linkflow.api.WorkflowApi;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.workflow.WorkflowStartDTO;
import com.linkflow.api.dto.workflow.WorkflowStatusDTO;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

@Service
@DubboService
public class WorkflowApiImpl implements WorkflowApi {
    @Override
    public Result<String> startApprovalProcess(WorkflowStartDTO dto) {
        return null;
    }

    @Override
    public Result<WorkflowStatusDTO> getProcessStatus(String processInstanceId) {
        return null;
    }

    @Override
    public Result<WorkflowStatusDTO> getProcessStatusByBusinessKey(Long businessKey) {
        return null;
    }

    @Override
    public Result<Void> cancelProcess(String processInstanceId) {
        return null;
    }

    @Override
    public Result<Void> approve(String processInstanceId, String taskId, Long approverId, String comment) {
        return null;
    }

    @Override
    public Result<Void> reject(String processInstanceId, String taskId,Long approverId,  String comment, String rejectReason) {
        return null;
    }
}
