package com.linkflow.workflow.service.impl;

import com.linkflow.api.WorkflowApi;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.workflow.WorkflowStartDTO;
import com.linkflow.api.dto.workflow.WorkflowStatusDTO;
import com.linkflow.workflow.service.ApproverService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@DubboService
public class WorkflowApiImpl implements WorkflowApi {

    private static final Logger log = LoggerFactory.getLogger(WorkflowApiImpl.class);
    private static final String PROCESS_DEFINITION_KEY = "campaignApproval";

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ApproverService approverService;

    @Override
    public Result<String> startApprovalProcess(WorkflowStartDTO dto) {
        log.info("com.linkflow.workflow.service.impl.WorkflowApiImpl#startApprovalProcess 启动审批流程, businessKey={}, businessType={}, campaignType={}, initiatorId={}",
                dto.getBusinessKey(), dto.getBusinessType(), dto.getCampaignType(), dto.getInitiatorId());

        try {
            // 获取审批人列表
            List<Long> level1Approvers = approverService.getApproverIds(dto.getCampaignType(), 1);
            List<Long> level2Approvers = approverService.getApproverIds(dto.getCampaignType(), 2);

            if (level1Approvers.isEmpty()) {
                log.error("一级审批人为空, campaignType={}", dto.getCampaignType());
                return Result.fail("一级审批人配置为空");
            }

            // 设置流程变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("level1Approvers", level1Approvers);
            variables.put("level2Approvers", level2Approvers);
            variables.put("initiatorId", dto.getInitiatorId());
            variables.put("campaignType", dto.getCampaignType());

            // 启动流程
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    PROCESS_DEFINITION_KEY,
                    String.valueOf(dto.getBusinessKey()),
                    variables
            );

            log.info("审批流程启动成功, processInstanceId={}, businessKey={}",
                    processInstance.getId(), dto.getBusinessKey());

            return Result.success(processInstance.getId());
        } catch (Exception e) {
            log.error("启动审批流程失败, businessKey={}, error={}", dto.getBusinessKey(), e.getMessage(), e);
            return Result.fail("启动审批流程失败: " + e.getMessage());
        }
    }

    @Override
    public Result<WorkflowStatusDTO> getProcessStatus(String processInstanceId) {
        log.info("com.linkflow.workflow.service.impl.WorkflowApiImpl#getProcessStatus 查询流程状态, processInstanceId={}",
                processInstanceId);

        try {
            // 先查询运行中的流程
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (processInstance != null) {
                return Result.success(buildRunningStatusDTO(processInstance));
            }

            // 查询历史流程
            HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (historicInstance != null) {
                return Result.success(buildHistoricStatusDTO(historicInstance));
            }

            log.warn("流程实例不存在, processInstanceId={}", processInstanceId);
            return Result.fail("流程实例不存在");
        } catch (Exception e) {
            log.error("查询流程状态失败, processInstanceId={}, error={}", processInstanceId, e.getMessage(), e);
            return Result.fail("查询流程状态失败: " + e.getMessage());
        }
    }

    @Override
    public Result<WorkflowStatusDTO> getProcessStatusByBusinessKey(Long businessKey) {
        log.info("com.linkflow.workflow.service.impl.WorkflowApiImpl#getProcessStatusByBusinessKey 根据业务键查询流程状态, businessKey={}",
                businessKey);

        try {
            // 先查询运行中的流程
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceBusinessKey(String.valueOf(businessKey))
                    .singleResult();

            if (processInstance != null) {
                return Result.success(buildRunningStatusDTO(processInstance));
            }

            // 查询历史流程
            HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceBusinessKey(String.valueOf(businessKey))
                    .singleResult();

            if (historicInstance != null) {
                return Result.success(buildHistoricStatusDTO(historicInstance));
            }

            log.warn("流程实例不存在, businessKey={}", businessKey);
            return Result.fail("流程实例不存在");
        } catch (Exception e) {
            log.error("根据业务键查询流程状态失败, businessKey={}, error={}", businessKey, e.getMessage(), e);
            return Result.fail("查询流程状态失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Void> cancelProcess(String processInstanceId) {
        log.info("com.linkflow.workflow.service.impl.WorkflowApiImpl#cancelProcess 取消流程, processInstanceId={}",
                processInstanceId);

        try {
            runtimeService.deleteProcessInstance(processInstanceId, "用户取消");
            log.info("流程取消成功, processInstanceId={}", processInstanceId);
            return Result.success();
        } catch (Exception e) {
            log.error("取消流程失败, processInstanceId={}, error={}", processInstanceId, e.getMessage(), e);
            return Result.fail("取消流程失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Void> approve(String processInstanceId, String taskId, Long approverId, String comment) {
        log.info("com.linkflow.workflow.service.impl.WorkflowApiImpl#approve 审批通过, processInstanceId={}, taskId={}, approverId={}, comment={}",
                processInstanceId, taskId, approverId, comment);

        try {
            // 设置任务变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("action", "approve");
            variables.put("comment", comment);

            // 完成任务
            taskService.complete(taskId, variables);

            log.info("审批通过成功, processInstanceId={}, taskId={}, approverId={}",
                    processInstanceId, taskId, approverId);
            return Result.success();
        } catch (Exception e) {
            log.error("审批通过失败, processInstanceId={}, taskId={}, error={}",
                    processInstanceId, taskId, e.getMessage(), e);
            return Result.fail("审批通过失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Void> reject(String processInstanceId, String taskId, Long approverId, String comment, String rejectReason) {
        log.info("com.linkflow.workflow.service.impl.WorkflowApiImpl#reject 审批拒绝, processInstanceId={}, taskId={}, approverId={}, comment={}, rejectReason={}",
                processInstanceId, taskId, approverId, comment, rejectReason);

        try {
            // 设置任务变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("action", "reject");
            variables.put("comment", comment);
            variables.put("rejectReason", rejectReason);

            // 完成任务
            taskService.complete(taskId, variables);

            log.info("审批拒绝成功, processInstanceId={}, taskId={}, approverId={}",
                    processInstanceId, taskId, approverId);
            return Result.success();
        } catch (Exception e) {
            log.error("审批拒绝失败, processInstanceId={}, taskId={}, error={}",
                    processInstanceId, taskId, e.getMessage(), e);
            return Result.fail("审批拒绝失败: " + e.getMessage());
        }
    }

    /**
     * 构建运行中流程状态 DTO
     */
    private WorkflowStatusDTO buildRunningStatusDTO(ProcessInstance processInstance) {
        WorkflowStatusDTO dto = new WorkflowStatusDTO();
        dto.setProcessInstanceId(processInstance.getId());
        dto.setBusinessKey(Long.valueOf(processInstance.getBusinessKey()));
        dto.setStatus("RUNNING");
        dto.setStartTime(processInstance.getStartTime());

        // 查询当前任务
        Task currentTask = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        if (currentTask != null) {
            dto.setCurrentTask(currentTask.getName());
            dto.setCurrentAssignee(currentTask.getAssignee());
        }

        return dto;
    }

    /**
     * 构建历史流程状态 DTO
     */
    private WorkflowStatusDTO buildHistoricStatusDTO(HistoricProcessInstance historicInstance) {
        WorkflowStatusDTO dto = new WorkflowStatusDTO();
        dto.setProcessInstanceId(historicInstance.getId());
        dto.setBusinessKey(Long.valueOf(historicInstance.getBusinessKey()));
        dto.setStartTime(historicInstance.getStartTime());
        dto.setEndTime(historicInstance.getEndTime());

        if (historicInstance.getEndTime() != null) {
            dto.setStatus("COMPLETED");
            // 根据流程结束状态判断最终结果
            String deleteReason = historicInstance.getDeleteReason();
            if (deleteReason != null) {
                dto.setStatus("CANCELLED");
            } else {
                // 查询流程变量判断最终结果
                Object finalResult = historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(historicInstance.getId())
                        .variableName("level2Approved")
                        .singleResult();

                if (finalResult != null && Boolean.TRUE.equals(finalResult)) {
                    dto.setFinalResult("APPROVED");
                } else {
                    dto.setFinalResult("REJECTED");
                }
            }
        }

        return dto;
    }
}