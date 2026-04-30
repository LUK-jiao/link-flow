package com.linkflow.workflow.service.impl;

import com.linkflow.api.ApproverConfigApi;
import com.linkflow.api.CampaignApi;
import com.linkflow.api.dto.campaign.CampaignStatusUpdateDTO;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.user.ApproverDTO;
import com.linkflow.api.dto.workflow.WorkflowStartDTO;
import com.linkflow.api.dto.workflow.WorkflowStatusDTO;
import com.linkflow.workflow.mapper.ApprovalRecordMapper;
import com.linkflow.workflow.model.ApprovalRecord;
import com.linkflow.workflow.service.WorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.flowable.engine.*;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 工作流服务实现
 */
@Slf4j
@Service
public class WorkflowServiceImpl implements WorkflowService {

    private static final String PROCESS_DEFINITION_KEY = "campaign_approval";
    private static final String BUSINESS_TYPE_CAMPAIGN = "CAMPAIGN_APPROVAL";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final RepositoryService repositoryService;
    private final ApprovalRecordMapper approvalRecordMapper;

    @DubboReference(check = false)
    private ApproverConfigApi approverConfigApi;

    @DubboReference(check = false)
    private CampaignApi campaignApi;

    public WorkflowServiceImpl(RuntimeService runtimeService,
                                TaskService taskService,
                                HistoryService historyService,
                                RepositoryService repositoryService,
                                ApprovalRecordMapper approvalRecordMapper) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
        this.repositoryService = repositoryService;
        this.approvalRecordMapper = approvalRecordMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String startProcess(WorkflowStartDTO dto) {
        Long businessKey = dto.getBusinessKey();
        String campaignType = dto.getCampaignType();
        Long initiatorId = dto.getInitiatorId();

        log.info("启动审批流程: businessKey={}, campaignType={}, initiatorId={}", 
                 businessKey, campaignType, initiatorId);

        // 1. 获取审批人配置
        Result<List<ApproverDTO>> approverResult = approverConfigApi.getApproverByType(campaignType);
        if (approverResult == null || !approverResult.isSuccess() || approverResult.getData() == null) {
            throw new RuntimeException("获取审批人配置失败");
        }

        List<ApproverDTO> approvers = approverResult.getData();
        if (approvers.isEmpty()) {
            throw new RuntimeException("未配置审批人");
        }

        // 2. 按级别分组审批人
        List<Long> level1Approvers = new ArrayList<>();
        List<Long> level2Approvers = new ArrayList<>();

        for (ApproverDTO approver : approvers) {
            Integer level = approver.getApproverLevel() != null ? approver.getApproverLevel() : 1;
            if (level == 1) {
                level1Approvers.add(approver.getApproverId());
            } else if (level == 2) {
                level2Approvers.add(approver.getApproverId());
            }
        }

        if (level1Approvers.isEmpty()) {
            throw new RuntimeException("未配置 Level1 审批人");
        }
        if (level2Approvers.isEmpty()) {
            throw new RuntimeException("未配置 Level2 审批人");
        }

        // 3. 准备流程变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("businessKey", businessKey);
        variables.put("campaignType", campaignType);
        variables.put("initiatorId", initiatorId);
        variables.put("level1Approvers", level1Approvers);
        variables.put("level2Approvers", level2Approvers);
        variables.put("level1Approved", false);
        variables.put("level1Rejected", false);
        variables.put("level2Approved", false);
        variables.put("level2Rejected", false);

        // 4. 启动流程实例
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                PROCESS_DEFINITION_KEY,
                String.valueOf(businessKey),
                variables
        );

        log.info("流程启动成功: processInstanceId={}, businessKey={}", 
                 processInstance.getId(), businessKey);

        return processInstance.getId();
    }

    @Override
    public WorkflowStatusDTO getProcessStatus(String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (processInstance != null) {
            // 流程还在运行中
            WorkflowStatusDTO dto = new WorkflowStatusDTO();
            dto.setProcessInstanceId(processInstanceId);
            dto.setBusinessKey(Long.parseLong(processInstance.getBusinessKey()));
            dto.setStatus("RUNNING");

            // 查询当前任务
            List<Task> tasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .list();

            if (!tasks.isEmpty()) {
                Task currentTask = tasks.get(0);
                dto.setCurrentTask(currentTask.getName());
                dto.setCurrentAssignee(currentTask.getAssignee());
            }

            return dto;
        }

        // 流程已结束，查询历史
        var historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (historicProcessInstance != null) {
            WorkflowStatusDTO dto = new WorkflowStatusDTO();
            dto.setProcessInstanceId(processInstanceId);
            dto.setBusinessKey(Long.parseLong(historicProcessInstance.getBusinessKey()));
            dto.setStatus("COMPLETED");
            dto.setEndTime(historicProcessInstance.getEndTime());

            // 查询流程变量获取最终结果
            var variables = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .list();

            for (var variable : variables) {
                if ("finalResult".equals(variable.getVariableName())) {
                    dto.setFinalResult((String) variable.getValue());
                }
            }

            return dto;
        }

        return null;
    }

    @Override
    public WorkflowStatusDTO getProcessStatusByBusinessKey(Long businessKey) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(String.valueOf(businessKey))
                .singleResult();

        if (processInstance != null) {
            return getProcessStatus(processInstance.getId());
        }

        // 查询历史
        var historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(String.valueOf(businessKey))
                .singleResult();

        if (historicProcessInstance != null) {
            return getProcessStatus(historicProcessInstance.getId());
        }

        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelProcess(String processInstanceId) {
        log.info("取消流程: processInstanceId={}", processInstanceId);
        runtimeService.deleteProcessInstance(processInstanceId, "用户取消");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(String taskId, Long approverId, String comment) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已完成");
        }

        String processInstanceId = task.getProcessInstanceId();
        String taskName = task.getName();
        String taskDefinitionKey = task.getTaskDefinitionKey();
        Long campaignId = Long.parseLong(task.getProcessInstanceBusinessKey());

        log.info("审批通过: taskId={}, approverId={}, taskName={}", taskId, approverId, taskName);

        // 记录审批
        saveApprovalRecord(campaignId, processInstanceId, taskId, taskName, approverId, "APPROVED", comment);

        // 设置审批通过变量
        Map<String, Object> variables = new HashMap<>();
        
        if (taskDefinitionKey.contains("level1")) {
            variables.put("level1Approved", true);
            // Level1 通过，继续到 Level2（不需要回调）
        } else if (taskDefinitionKey.contains("level2")) {
            variables.put("level2Approved", true);
            variables.put("finalResult", STATUS_APPROVED);
            // Level2 通过，流程结束，回调 Campaign
            callbackCampaign(campaignId, STATUS_APPROVED, null, approverId);
        }

        taskService.complete(taskId, variables);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(String taskId, Long approverId, String comment, String rejectReason) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已完成");
        }

        String processInstanceId = task.getProcessInstanceId();
        String taskName = task.getName();
        String taskDefinitionKey = task.getTaskDefinitionKey();
        Long campaignId = Long.parseLong(task.getProcessInstanceBusinessKey());

        log.info("审批拒绝: taskId={}, approverId={}, taskName={}, rejectReason={}", 
                 taskId, approverId, taskName, rejectReason);

        // 记录审批
        saveApprovalRecord(campaignId, processInstanceId, taskId, taskName, approverId, 
                          "REJECTED", comment + " | 原因: " + rejectReason);

        // 设置审批拒绝变量
        Map<String, Object> variables = new HashMap<>();
        
        if (taskDefinitionKey.contains("level1")) {
            variables.put("level1Rejected", true);
        } else if (taskDefinitionKey.contains("level2")) {
            variables.put("level2Rejected", true);
        }
        variables.put("finalResult", STATUS_REJECTED);

        // 拒绝时回调 Campaign
        callbackCampaign(campaignId, STATUS_REJECTED, rejectReason, approverId);

        taskService.complete(taskId, variables);
    }

    /**
     * 保存审批记录
     */
    private void saveApprovalRecord(Long campaignId, String processInstanceId, String taskId, 
                                     String taskName, Long approverId, 
                                     String action, String comment) {
        ApprovalRecord record = new ApprovalRecord();
        record.setCampaignId(campaignId);
        // workflowInstanceId 使用 campaignId 作为简化关联（或单独维护 workflow_instance 表）
        record.setWorkflowInstanceId(campaignId);
        record.setTaskId(taskId);
        record.setTaskName(taskName);
        record.setApproverId(approverId);
        record.setAction(action);
        record.setComment(comment);
        record.setApproveTime(new Date());
        record.setCreateTime(new Date());
        approvalRecordMapper.insert(record);
    }

    /**
     * 回调 Campaign 服务
     */
    private void callbackCampaign(Long campaignId, String status, 
                                   String rejectReason, Long approverId) {
        log.info("回调 Campaign: campaignId={}, status={}, approverId={}", 
                 campaignId, status, approverId);

        CampaignStatusUpdateDTO dto = new CampaignStatusUpdateDTO();
        dto.setCampaignId(campaignId);
        dto.setStatus(status);
        dto.setRejectReason(rejectReason);

        Result<Void> result = campaignApi.updateCampaignStatus(dto);
        if (result == null || !result.isSuccess()) {
            log.error("回调 Campaign 失败: {}", result != null ? result.getMessage() : "null");
            throw new RuntimeException("更新活动状态失败");
        }
    }
}