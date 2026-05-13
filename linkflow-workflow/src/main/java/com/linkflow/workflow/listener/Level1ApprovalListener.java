package com.linkflow.workflow.listener;

import com.linkflow.workflow.mapper.ApprovalRecordMapper;
import com.linkflow.workflow.model.ApprovalRecord;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class Level1ApprovalListener implements TaskListener {

    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private RuntimeService runtimeService;

    @Override
    public void notify(DelegateTask delegateTask) {
        // campaign_id 是 businessKey
        // 1. 获取流程实例ID
        String processInstanceId = delegateTask.getProcessInstanceId();

        // 2. 获取 businessKey（即 campaignId）
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        Long campaignId = processInstance != null ?
                Long.valueOf(processInstance.getBusinessKey()) : null;

        // 3. 获取审批人信息
        Long approverId = parseLong(delegateTask.getAssignee());
        String approverName = (String) delegateTask.getVariable("approverName");
        if (approverName == null || approverName.isBlank()) {
            approverName = delegateTask.getAssignee();
        }

        // 4. 获取审批结果和意见（前端提交的表单变量）
        String action = normalizeAction((String) delegateTask.getVariable("action"));
        String comment = (String) delegateTask.getVariable("comment");

        // 5. 设置流程变量（用于网关判断流程走向和多实例提前终止）
        boolean isApproved = "approve".equals(action);
        delegateTask.setVariable("level1Approved", isApproved);

        // 6. 如果拒绝，设置拒绝原因（供 CampaignRejectedDelegate 使用）
        String rejectReason = "";
        if (!isApproved) {
            rejectReason = delegateTask.getVariable("rejectReason") != null ? (String)delegateTask.getVariable("rejectReason") : comment;
            delegateTask.setVariable("rejectReason", rejectReason);
        }

        // 7. 构建并插入审批记录
        ApprovalRecord record = new ApprovalRecord();
        record.setCampaignId(campaignId);
        record.setProcessInstanceId(processInstanceId);
        record.setTaskId(delegateTask.getId());
        record.setTaskName(delegateTask.getName());
        record.setApproverId(approverId);
        record.setApproverName(approverName);
        record.setAction(action);
        record.setComment(comment);
        record.setRejectReason(rejectReason);
        record.setApproveTime(new Date());
        record.setCreateTime(new Date());

        approvalRecordMapper.insert(record);
    }

    private String normalizeAction(String action) {
        if ("reject".equalsIgnoreCase(action)) {
            return "reject";
        }
        return "approve";
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
