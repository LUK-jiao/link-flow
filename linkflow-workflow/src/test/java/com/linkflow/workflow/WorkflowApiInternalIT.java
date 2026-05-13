package com.linkflow.workflow;

import com.linkflow.api.WorkflowApi;
import com.linkflow.api.dto.campaign.CampaignStatusUpdateDTO;
import com.linkflow.api.dto.common.PageResult;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.workflow.ApprovalRequestDTO;
import com.linkflow.api.dto.workflow.RejectRequestDTO;
import com.linkflow.api.dto.workflow.WorkflowStartDTO;
import com.linkflow.api.dto.workflow.WorkflowStatusDTO;
import com.linkflow.api.dto.workflow.WorkflowTaskDTO;
import com.linkflow.api.dto.workflow.WorkflowTaskQueryDTO;
import com.linkflow.workflow.mapper.ApprovalRecordMapper;
import com.linkflow.workflow.model.ApprovalRecord;
import com.linkflow.workflow.service.ApproverService;
import com.linkflow.workflow.service.CampaignService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = WorkflowApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.main.web-application-type=none",
                "dubbo.registry.address=N/A",
                "dubbo.protocol.port=-1",
                "dubbo.application.qos-enable=false"
        }
)
class WorkflowApiInternalIT {

    @Autowired
    private WorkflowApi workflowApi;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    @MockitoBean
    private ApproverService approverService;

    @MockitoBean
    private CampaignService campaignService;

    @BeforeEach
    void setUp() {
        reset(approverService, campaignService);
        when(campaignService.updateCampaignStatus(any(CampaignStatusUpdateDTO.class)))
                .thenReturn(Result.success());
    }

    @Test
    void approvePathShouldMoveFromLevel1ToLevel2AndCallbackApproved() {
        String campaignType = uniqueName("MARKETING");
        Long campaignId = uniqueId();

        when(approverService.getApproverIds(eq(campaignType), eq(1)))
                .thenReturn(List.of(101L, 102L));
        when(approverService.getApproverIds(eq(campaignType), eq(2)))
                .thenReturn(List.of(201L));

        Result<String> startResult = workflowApi.startApprovalProcess(startDTO(campaignId, campaignType));
        assertThat(startResult.isSuccess()).isTrue();
        String processInstanceId = startResult.getData();

        Result<PageResult<WorkflowTaskDTO>> level1PendingResult = workflowApi.getPendingTasks(taskQuery(101L, campaignType));
        assertThat(level1PendingResult.isSuccess()).isTrue();
        assertThat(level1PendingResult.getData().getTotal()).isEqualTo(1L);
        WorkflowTaskDTO level1PendingTask = level1PendingResult.getData().getRecords().get(0);
        assertThat(level1PendingTask.getProcessInstanceId()).isEqualTo(processInstanceId);
        assertThat(level1PendingTask.getBusinessKey()).isEqualTo(campaignId);
        assertThat(level1PendingTask.getBusinessType()).isEqualTo("CAMPAIGN_APPROVAL");
        assertThat(level1PendingTask.getCampaignType()).isEqualTo(campaignType);
        assertThat(level1PendingTask.getTaskDefinitionKey()).isEqualTo("level1Approval");

        List<Task> level1Tasks = tasks(processInstanceId, "level1Approval");
        assertThat(level1Tasks)
                .extracting(Task::getAssignee)
                .containsExactlyInAnyOrder("101", "102");

        Result<Void> level1ApproveResult = workflowApi.approve(approvalDTO(processInstanceId, level1Tasks.get(0), "一级通过"));
        assertThat(level1ApproveResult.isSuccess()).isTrue();

        List<Task> level2Tasks = tasks(processInstanceId, "level2Approval");
        assertThat(level2Tasks)
                .extracting(Task::getAssignee)
                .containsExactly("201");

        Result<PageResult<WorkflowTaskDTO>> level2PendingResult = workflowApi.getPendingTasks(taskQuery(201L, campaignType));
        assertThat(level2PendingResult.isSuccess()).isTrue();
        assertThat(level2PendingResult.getData().getTotal()).isEqualTo(1L);
        assertThat(level2PendingResult.getData().getRecords().get(0).getTaskDefinitionKey())
                .isEqualTo("level2Approval");

        Result<Void> level2ApproveResult = workflowApi.approve(approvalDTO(processInstanceId, level2Tasks.get(0), "二级通过"));
        assertThat(level2ApproveResult.isSuccess()).isTrue();

        assertThat(workflowApi.getPendingTasks(taskQuery(201L, campaignType)).getData().getTotal())
                .isZero();

        ArgumentCaptor<CampaignStatusUpdateDTO> captor = ArgumentCaptor.forClass(CampaignStatusUpdateDTO.class);
        verify(campaignService, atLeastOnce()).updateCampaignStatus(captor.capture());
        CampaignStatusUpdateDTO callbackDTO = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(callbackDTO.getCampaignId()).isEqualTo(campaignId);
        assertThat(callbackDTO.getStatus()).isEqualTo("APPROVED");

        Result<WorkflowStatusDTO> statusResult = workflowApi.getProcessStatus(processInstanceId);
        assertThat(statusResult.isSuccess()).isTrue();
        assertThat(statusResult.getData().getStatus()).isEqualTo("COMPLETED");
        assertThat(statusResult.getData().getFinalResult()).isEqualTo("APPROVED");

        assertThat(approvalRecordMapper.selectAll())
                .filteredOn(record -> processInstanceId.equals(record.getProcessInstanceId()))
                .extracting(ApprovalRecord::getAction)
                .containsExactly("approve", "approve");
    }

    @Test
    void level1RejectShouldFinishProcessAndCallbackRejected() {
        String campaignType = uniqueName("PROMOTION");
        Long campaignId = uniqueId();

        when(approverService.getApproverIds(eq(campaignType), eq(1)))
                .thenReturn(List.of(301L));
        when(approverService.getApproverIds(eq(campaignType), eq(2)))
                .thenReturn(List.of(401L));

        Result<String> startResult = workflowApi.startApprovalProcess(startDTO(campaignId, campaignType));
        assertThat(startResult.isSuccess()).isTrue();
        String processInstanceId = startResult.getData();

        Result<PageResult<WorkflowTaskDTO>> pendingResult = workflowApi.getPendingTasks(taskQuery(301L, campaignType));
        assertThat(pendingResult.isSuccess()).isTrue();
        assertThat(pendingResult.getData().getTotal()).isEqualTo(1L);

        Task level1Task = tasks(processInstanceId, "level1Approval").get(0);
        Result<Void> rejectResult = workflowApi.reject(rejectDTO(processInstanceId, level1Task));
        assertThat(rejectResult.isSuccess()).isTrue();

        assertThat(tasks(processInstanceId, "level2Approval")).isEmpty();
        assertThat(workflowApi.getPendingTasks(taskQuery(301L, campaignType)).getData().getTotal())
                .isZero();

        ArgumentCaptor<CampaignStatusUpdateDTO> captor = ArgumentCaptor.forClass(CampaignStatusUpdateDTO.class);
        verify(campaignService, atLeastOnce()).updateCampaignStatus(captor.capture());
        CampaignStatusUpdateDTO callbackDTO = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(callbackDTO.getCampaignId()).isEqualTo(campaignId);
        assertThat(callbackDTO.getStatus()).isEqualTo("REJECTED");
        assertThat(callbackDTO.getRejectReason()).isEqualTo("预算不通过");

        Result<WorkflowStatusDTO> statusResult = workflowApi.getProcessStatus(processInstanceId);
        assertThat(statusResult.isSuccess()).isTrue();
        assertThat(statusResult.getData().getStatus()).isEqualTo("COMPLETED");
        assertThat(statusResult.getData().getFinalResult()).isEqualTo("REJECTED");

        assertThat(approvalRecordMapper.selectAll())
                .filteredOn(record -> processInstanceId.equals(record.getProcessInstanceId()))
                .singleElement()
                .satisfies(record -> {
                    assertThat(record.getAction()).isEqualTo("reject");
                    assertThat(record.getRejectReason()).isEqualTo("预算不通过");
                });
    }

    private WorkflowStartDTO startDTO(Long campaignId, String campaignType) {
        WorkflowStartDTO dto = new WorkflowStartDTO();
        dto.setBusinessKey(campaignId);
        dto.setBusinessType("CAMPAIGN_APPROVAL");
        dto.setCampaignType(campaignType);
        dto.setInitiatorId(1L);
        return dto;
    }

    private WorkflowTaskQueryDTO taskQuery(Long approverId, String campaignType) {
        WorkflowTaskQueryDTO queryDTO = new WorkflowTaskQueryDTO();
        queryDTO.setApproverId(approverId);
        queryDTO.setBusinessType("CAMPAIGN_APPROVAL");
        queryDTO.setCampaignType(campaignType);
        queryDTO.setPageNum(1);
        queryDTO.setPageSize(10);
        return queryDTO;
    }

    private ApprovalRequestDTO approvalDTO(String processInstanceId, Task task, String comment) {
        ApprovalRequestDTO dto = new ApprovalRequestDTO();
        dto.setProcessInstanceId(processInstanceId);
        dto.setTaskId(task.getId());
        dto.setApproverId(Long.valueOf(task.getAssignee()));
        dto.setApproverName("approver-" + task.getAssignee());
        dto.setComment(comment);
        return dto;
    }

    private RejectRequestDTO rejectDTO(String processInstanceId, Task task) {
        RejectRequestDTO dto = new RejectRequestDTO();
        dto.setProcessInstanceId(processInstanceId);
        dto.setTaskId(task.getId());
        dto.setApproverId(Long.valueOf(task.getAssignee()));
        dto.setApproverName("approver-" + task.getAssignee());
        dto.setComment("一级拒绝");
        dto.setRejectReason("预算不通过");
        return dto;
    }

    private List<Task> tasks(String processInstanceId, String taskDefinitionKey) {
        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(taskDefinitionKey)
                .orderByTaskCreateTime()
                .asc()
                .list();
    }

    private Long uniqueId() {
        return Math.abs(UUID.randomUUID().getMostSignificantBits());
    }

    private String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
