package com.linkflow.gateway.controller;

import com.linkflow.api.WorkflowApi;
import com.linkflow.api.dto.common.PageResult;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.workflow.ApprovalRecordDTO;
import com.linkflow.api.dto.workflow.ApprovalRequestDTO;
import com.linkflow.api.dto.workflow.RejectRequestDTO;
import com.linkflow.api.dto.workflow.WorkflowStartDTO;
import com.linkflow.api.dto.workflow.WorkflowStatusDTO;
import com.linkflow.api.dto.workflow.WorkflowTaskDTO;
import com.linkflow.api.dto.workflow.WorkflowTaskQueryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Workflow", description = "工作流审批接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workflows")
public class WorkflowGatewayController {

    private final WorkflowApi workflowApi;

    @Operation(summary = "发起审批流程")
    @PostMapping("/start")
    public Result<String> startApprovalProcess(@RequestBody WorkflowStartDTO dto) {
        return workflowApi.startApprovalProcess(dto);
    }

    @Operation(summary = "按流程实例ID查询流程状态")
    @GetMapping("/{processInstanceId}/status")
    public Result<WorkflowStatusDTO> getProcessStatus(
            @Parameter(description = "流程实例ID", required = true)
            @PathVariable("processInstanceId") String processInstanceId) {
        return workflowApi.getProcessStatus(processInstanceId);
    }

    @Operation(summary = "查询待审批任务（分页）")
    @PostMapping("/tasks/pending/query")
    public Result<PageResult<WorkflowTaskDTO>> getPendingTasks(@RequestBody WorkflowTaskQueryDTO query) {
        return workflowApi.getPendingTasks(query);
    }

    @Operation(summary = "按活动ID查询审批记录")
    @GetMapping("/campaigns/{campaignId}/approval-records")
    public Result<List<ApprovalRecordDTO>> getApprovalRecordsByCampaignId(
            @Parameter(description = "活动ID", required = true)
            @PathVariable("campaignId") Long campaignId) {
        return workflowApi.getApprovalRecordsByCampaignId(campaignId);
    }

    @Operation(summary = "按业务键查询流程状态")
    @GetMapping("/business/{businessKey}/status")
    public Result<WorkflowStatusDTO> getProcessStatusByBusinessKey(
            @Parameter(description = "业务键(如 campaignId)", required = true)
            @PathVariable("businessKey") Long businessKey) {
        return workflowApi.getProcessStatusByBusinessKey(businessKey);
    }

    @Operation(summary = "取消流程")
    @PostMapping("/{processInstanceId}/cancel")
    public Result<Void> cancelProcess(
            @Parameter(description = "流程实例ID", required = true)
            @PathVariable("processInstanceId") String processInstanceId) {
        return workflowApi.cancelProcess(processInstanceId);
    }

    @Operation(summary = "审批通过")
    @PostMapping("/approve")
    public Result<Void> approve(@RequestBody ApprovalRequestDTO dto) {
        return workflowApi.approve(dto);
    }

    @Operation(summary = "审批拒绝")
    @PostMapping("/reject")
    public Result<Void> reject(@RequestBody RejectRequestDTO dto) {
        return workflowApi.reject(dto);
    }
}
