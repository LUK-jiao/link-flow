package com.linkflow.workflow.service;

import com.linkflow.api.WorkflowApi;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.workflow.WorkflowStartDTO;
import com.linkflow.api.dto.workflow.WorkflowStatusDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;

/**
 * Workflow Dubbo 服务实现
 */
@Slf4j
@DubboService
@Service
public class WorkflowApiImpl implements WorkflowApi {

    private final WorkflowService workflowService;
    private final TaskService taskService;

    public WorkflowApiImpl(WorkflowService workflowService, TaskService taskService) {
        this.workflowService = workflowService;
        this.taskService = taskService;
    }

    @Override
    public Result<String> startApprovalProcess(WorkflowStartDTO dto) {
        try {
            String processInstanceId = workflowService.startProcess(dto);
            return Result.success(processInstanceId);
        } catch (Exception e) {
            log.error("启动审批流程失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @Override
    public Result<WorkflowStatusDTO> getProcessStatus(String processInstanceId) {
        try {
            WorkflowStatusDTO status = workflowService.getProcessStatus(processInstanceId);
            return Result.success(status);
        } catch (Exception e) {
            log.error("查询流程状态失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @Override
    public Result<WorkflowStatusDTO> getProcessStatusByBusinessKey(Long businessKey) {
        try {
            WorkflowStatusDTO status = workflowService.getProcessStatusByBusinessKey(businessKey);
            return Result.success(status);
        } catch (Exception e) {
            log.error("查询流程状态失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @Override
    public Result<Void> cancelProcess(String processInstanceId) {
        try {
            workflowService.cancelProcess(processInstanceId);
            return Result.success();
        } catch (Exception e) {
            log.error("取消流程失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @Override
    public Result<Void> approve(String processInstanceId, Long approverId, String comment) {
        try {
            // 根据流程实例ID和审批人ID查找待办任务
            Task task = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .taskAssignee(String.valueOf(approverId))
                    .singleResult();

            if (task == null) {
                return Result.fail("未找到待办任务");
            }

            workflowService.approve(task.getId(), approverId, comment);
            return Result.success();
        } catch (Exception e) {
            log.error("审批通过失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @Override
    public Result<Void> reject(String processInstanceId, Long approverId, 
                                String comment, String rejectReason) {
        try {
            // 根据流程实例ID和审批人ID查找待办任务
            Task task = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .taskAssignee(String.valueOf(approverId))
                    .singleResult();

            if (task == null) {
                return Result.fail("未找到待办任务");
            }

            workflowService.reject(task.getId(), approverId, comment, rejectReason);
            return Result.success();
        } catch (Exception e) {
            log.error("审批拒绝失败", e);
            return Result.fail(e.getMessage());
        }
    }
}