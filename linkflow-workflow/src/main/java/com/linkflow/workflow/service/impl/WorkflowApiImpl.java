package com.linkflow.workflow.service.impl;

import com.linkflow.api.WorkflowApi;
import com.linkflow.api.dto.common.PageResult;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.workflow.*;
import com.linkflow.workflow.service.ApproverService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
            variables.put("businessType", dto.getBusinessType());
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
    public Result<PageResult<WorkflowTaskDTO>> getPendingTasks(WorkflowTaskQueryDTO query) {
        log.info("com.linkflow.workflow.service.impl.WorkflowApiImpl#getPendingTasks 查询待审批任务, approverId={}, businessType={}, campaignType={}, taskDefinitionKey={}, pageNum={}, pageSize={}",
                query != null ? query.getApproverId() : null,
                query != null ? query.getBusinessType() : null,
                query != null ? query.getCampaignType() : null,
                query != null ? query.getTaskDefinitionKey() : null,
                query != null ? query.getPageNum() : null,
                query != null ? query.getPageSize() : null);

        if (query == null || query.getApproverId() == null) {
            return Result.fail(400, "审批人ID不能为空");
        }

        try {
            int pageNum = normalizePageNum(query.getPageNum());
            int pageSize = normalizePageSize(query.getPageSize());
            int firstResult = (pageNum - 1) * pageSize;

            TaskQuery taskQuery = buildPendingTaskQuery(query);
            long total = taskQuery.count();

            List<Task> tasks = buildPendingTaskQuery(query)
                    .orderByTaskCreateTime()
                    .desc()
                    .listPage(firstResult, pageSize);

            List<WorkflowTaskDTO> records = tasks.stream()
                    .map(this::convertToTaskDTO)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            PageResult<WorkflowTaskDTO> pageResult = new PageResult<>();
            pageResult.setRecords(records);
            pageResult.setTotal(total);
            pageResult.setPageNum((long) pageNum);
            pageResult.setPageSize((long) pageSize);
            pageResult.setPages((total + pageSize - 1) / pageSize);

            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("查询待审批任务失败, approverId={}, error={}",
                    query.getApproverId(), e.getMessage(), e);
            return Result.fail("查询待审批任务失败: " + e.getMessage());
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
    public Result<Void> approve(ApprovalRequestDTO dto) {
        log.info("com.linkflow.workflow.service.impl.WorkflowApiImpl#approve 审批通过, processInstanceId={}, taskId={}, approverId={}, approverName={}, comment={}",
                dto.getProcessInstanceId(), dto.getTaskId(), dto.getApproverId(), dto.getApproverName(), dto.getComment());

        try {
            Result<Void> validation = validatePendingTask(dto.getTaskId(), dto.getProcessInstanceId(), dto.getApproverId());
            if (validation != null) {
                return validation;
            }
            // 设置任务变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("action", "approve");
            variables.put("comment", dto.getComment());
            variables.put("approverName", dto.getApproverName());

            // 完成任务
            taskService.complete(dto.getTaskId(), variables);

            log.info("审批通过成功, processInstanceId={}, taskId={}, approverId={}",
                    dto.getProcessInstanceId(), dto.getTaskId(), dto.getApproverId());
            return Result.success();
        } catch (Exception e) {
            log.error("审批通过失败, processInstanceId={}, taskId={}, error={}",
                    dto.getProcessInstanceId(), dto.getTaskId(), e.getMessage(), e);
            return Result.fail("审批通过失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Void> reject(RejectRequestDTO dto) {
        log.info("com.linkflow.workflow.service.impl.WorkflowApiImpl#reject 审批拒绝, processInstanceId={}, taskId={}, approverId={}, approverName={}, comment={}, rejectReason={}",
                dto.getProcessInstanceId(), dto.getTaskId(), dto.getApproverId(), dto.getApproverName(), dto.getComment(), dto.getRejectReason());

        try {
            Result<Void> validation = validatePendingTask(dto.getTaskId(), dto.getProcessInstanceId(), dto.getApproverId());
            if (validation != null) {
                return validation;
            }
            // 设置任务变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("action", "reject");
            variables.put("comment", dto.getComment());
            variables.put("rejectReason", dto.getRejectReason());
            variables.put("approverName", dto.getApproverName());

            // 完成任务
            taskService.complete(dto.getTaskId(), variables);

            log.info("审批拒绝成功, processInstanceId={}, taskId={}, approverId={}",
                    dto.getProcessInstanceId(), dto.getTaskId(), dto.getApproverId());
            return Result.success();
        } catch (Exception e) {
            log.error("审批拒绝失败, processInstanceId={}, taskId={}, error={}",
                    dto.getProcessInstanceId(), dto.getTaskId(), e.getMessage(), e);
            return Result.fail("审批拒绝失败: " + e.getMessage());
        }
    }

    private TaskQuery buildPendingTaskQuery(WorkflowTaskQueryDTO query) {
        TaskQuery taskQuery = taskService.createTaskQuery()
                .taskAssignee(String.valueOf(query.getApproverId()))
                .active();

        if (hasText(query.getBusinessType())) {
            taskQuery.processVariableValueEquals("businessType", query.getBusinessType());
        }
        if (hasText(query.getCampaignType())) {
            taskQuery.processVariableValueEquals("campaignType", query.getCampaignType());
        }
        if (hasText(query.getTaskDefinitionKey())) {
            taskQuery.taskDefinitionKey(query.getTaskDefinitionKey());
        }

        return taskQuery;
    }

    private WorkflowTaskDTO convertToTaskDTO(Task task) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult();
        if (processInstance == null) {
            log.warn("待办任务对应的流程实例不存在, taskId={}, processInstanceId={}",
                    task.getId(), task.getProcessInstanceId());
            return null;
        }

        WorkflowTaskDTO dto = new WorkflowTaskDTO();
        dto.setTaskId(task.getId());
        dto.setTaskName(task.getName());
        dto.setTaskDefinitionKey(task.getTaskDefinitionKey());
        dto.setProcessInstanceId(task.getProcessInstanceId());
        dto.setBusinessKey(Long.valueOf(processInstance.getBusinessKey()));
        dto.setApproverId(Long.valueOf(task.getAssignee()));
        dto.setCreateTime(task.getCreateTime());
        dto.setBusinessType((String) runtimeService.getVariable(task.getProcessInstanceId(), "businessType"));
        dto.setCampaignType((String) runtimeService.getVariable(task.getProcessInstanceId(), "campaignType"));
        return dto;
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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
            String deleteReason = historicInstance.getDeleteReason();
            if (deleteReason != null && !deleteReason.isBlank()) {
                dto.setStatus("CANCELLED");
                dto.setFinalResult("CANCELLED");
                return dto;
            }

            dto.setStatus("COMPLETED");
            Boolean level2Approved = getHistoricBooleanVar(historicInstance.getId(), "level2Approved");
            if (Boolean.TRUE.equals(level2Approved)) {
                dto.setFinalResult("APPROVED");
                return dto;
            }

            Boolean level1Approved = getHistoricBooleanVar(historicInstance.getId(), "level1Approved");
            if (Boolean.FALSE.equals(level1Approved) || Boolean.FALSE.equals(level2Approved)) {
                dto.setFinalResult("REJECTED");
            } else {
                dto.setFinalResult("UNKNOWN");
            }
        }

        return dto;
    }

    private Result<Void> validatePendingTask(String taskId, String processInstanceId,Long approverId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            log.warn("任务不存在或已处理, taskId={}", taskId);
            return Result.fail(409, "任务不存在或已处理，请勿重复审批");
        }
        if (processInstanceId != null && !processInstanceId.equals(task.getProcessInstanceId())) {
            log.warn("任务与流程实例不匹配, taskId={}, requestProcessInstanceId={}, actualProcessInstanceId={}",
                    taskId, processInstanceId, task.getProcessInstanceId());
            return Result.fail(400, "任务与流程实例不匹配");
        }
        if(approverId == null || !approverId.equals(Long.valueOf(task.getAssignee()))) {
            log.warn("任务指定审批人和当前任务提交的审批人不一致，请重新提交，approverId={}, task.assignee={}", approverId, task.getAssignee());
            return Result.fail(401,"审批人不一致，请检查后重新提交");
        }
        return null;
    }

    private Boolean getHistoricBooleanVar(String processInstanceId, String variableName) {
        HistoricVariableInstance var = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName(variableName)
                .singleResult();
        if (var == null) return null;
        Object value = var.getValue();
        return value instanceof Boolean ? (Boolean) value : null;
    }
}
