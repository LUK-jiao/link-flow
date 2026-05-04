package com.linkflow.workflow.delegate;

import com.linkflow.api.dto.campaign.CampaignStatusUpdateDTO;
import com.linkflow.api.dto.common.Result;
import com.linkflow.workflow.service.CampaignService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 活动审批通过 - 回调 Campaign 服务
 */
@Component
public class CampaignApprovedDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CampaignApprovedDelegate.class);

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private RuntimeService runtimeService;

    @Override
    public void execute(DelegateExecution execution) {
        // 1. 获取 businessKey（campaignId）
        String processInstanceId = execution.getProcessInstanceId();
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (processInstance == null) {
            log.error("流程实例不存在: {}", processInstanceId);
            return;
        }

        Long campaignId = Long.valueOf(processInstance.getBusinessKey());

        // 2. 构建状态更新 DTO
        CampaignStatusUpdateDTO dto = new CampaignStatusUpdateDTO();
        dto.setCampaignId(campaignId);
        dto.setStatus("APPROVED");

        // 3. 调用 CampaignService 更新状态
        Result<Void> result = campaignService.updateCampaignStatus(dto);

        if (result.isSuccess()) {
            log.info("活动审批通过，campaignId: {}", campaignId);
        } else {
            log.error("更新活动状态失败: campaignId={}, error={}", campaignId, result.getMessage());
        }
    }
}