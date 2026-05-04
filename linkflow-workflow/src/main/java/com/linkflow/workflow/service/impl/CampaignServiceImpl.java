package com.linkflow.workflow.service.impl;


import com.linkflow.api.CampaignApi;
import com.linkflow.api.dto.campaign.CampaignStatusUpdateDTO;
import com.linkflow.api.dto.common.Result;
import com.linkflow.workflow.service.CampaignService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CampaignServiceImpl implements CampaignService {

    private static final Logger log = LoggerFactory.getLogger(CampaignServiceImpl.class);

    @DubboReference
    private CampaignApi campaignApi;

    @Override
    public Result<Void> updateCampaignStatus(CampaignStatusUpdateDTO dto) {
        log.info("com.linkflow.workflow.service.impl.CampaignServiceImpl#updateCampaignStatus 调用 CampaignApi.updateCampaignStatus, campaignId={}, status={}", 
                dto.getCampaignId(), dto.getStatus());
        
        Result<Void> result = campaignApi.updateCampaignStatus(dto);
        
        if (result != null && result.isSuccess()) {
            log.info("更新活动状态成功: campaignId={}, status={}", dto.getCampaignId(), dto.getStatus());
        } else {
            log.error("更新活动状态失败: campaignId={}, status={}, error={}", 
                    dto.getCampaignId(), dto.getStatus(), result != null ? result.getMessage() : "null");
        }
        
        return result;
    }
}
