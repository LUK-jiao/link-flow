package com.linkflow.workflow.listener;

import com.linkflow.workflow.mapper.ApprovalRecordMapper;
import com.linkflow.workflow.model.ApprovalRecord;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class Level2ApprovalListener implements TaskListener {
    @Autowired
    private RuntimeService runtimeService;
    
    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;
    
    @Override
    public void notify(DelegateTask delegateTask) {
        //campaign_id是businessKey
        // 1. 获取流程实例ID
        String processInstanceId = delegateTask.getProcessInstanceId();

        // 2. 获取 businessKey（即 campaignId）
        ProcessInstance processInstance = (ProcessInstance) runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        Long campaignId = processInstance != null ?
                Long.valueOf(processInstance.getBusinessKey()) : null;

        // 3. 获取审批人信息
        Long approverId = Long.valueOf(delegateTask.getAssignee()); // 当前审批人
        String approverName = (String)delegateTask.getVariableLocal("approverName");

        // 4. 获取审批结果和意见（前端提交的表单变量）
        String action = (String) delegateTask.getVariableLocal("action");
        String comment = (String) delegateTask.getVariableLocal("comment");

        // 5. 设置流程变量（用于网关判断流程走向）
        boolean isApproved = "approve".equalsIgnoreCase(action);
        delegateTask.setVariable("level2Approved", isApproved);

        // 6. 构建并插入审批记录
        ApprovalRecord record = new ApprovalRecord();
        record.setCampaignId(campaignId);
        record.setProcessInstanceId(processInstanceId);
        record.setTaskId(delegateTask.getId());
        record.setTaskName(delegateTask.getName());
        record.setApproverId(approverId);
        record.setApproverName(approverName);
        record.setAction(action);
        record.setComment(comment);
        record.setApproveTime(new Date());
        record.setCreateTime(new Date());

        approvalRecordMapper.insert(record);
    }
}
