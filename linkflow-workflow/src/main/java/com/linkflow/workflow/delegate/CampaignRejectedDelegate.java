package com.linkflow.workflow.delegate;

import com.linkflow.api.CampaignApi;
import com.linkflow.api.dto.campaign.CampaignStatusUpdateDTO;
import com.linkflow.api.dto.common.Result;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.runtime.ProcessInstance;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 活动审批拒绝 - 回调 Campaign 服务
 */
@Component
public class CampaignRejectedDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CampaignRejectedDelegate.class);

    @DubboReference(check = false)
    private CampaignApi campaignApi;

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

        // 2. 获取拒绝原因（从流程变量中获取，由 Listener 设置）
        String rejectReason = (String) execution.getVariable("rejectReason");
        if (rejectReason == null || rejectReason.isEmpty()) {
            rejectReason = "审批不通过";
        }

        // 3. 构建状态更新 DTO
        CampaignStatusUpdateDTO dto = new CampaignStatusUpdateDTO();
        dto.setCampaignId(campaignId);
        dto.setStatus("REJECTED");
        dto.setRejectReason(rejectReason);

        // 4. 调用 Campaign 服务更新状态
        Result<Void> result = campaignApi.updateCampaignStatus(dto);

        if (result.getCode() == 200) {
            log.info("活动审批拒绝，campaignId: {}, reason: {}", campaignId, rejectReason);
        } else {
            log.error("更新活动状态失败: campaignId={}, error={}", campaignId, result.getMessage());
        }
    }
}