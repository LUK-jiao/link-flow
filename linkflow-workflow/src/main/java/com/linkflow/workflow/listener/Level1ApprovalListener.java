package com.linkflow.workflow.listener;

import com.linkflow.workflow.mapper.ApprovalRecordMapper;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Level1ApprovalListener implements TaskListener {
    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    @Override
    public void notify(DelegateTask delegateTask) {
        //todo 获取approval_record 插入一条审批记录所需信息，进行插入，其中campaign_id是businessKey
    }
}