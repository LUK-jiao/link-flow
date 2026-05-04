package com.linkflow.workflow.service.impl;


import com.linkflow.api.dto.campaign.CampaignStatusUpdateDTO;
import com.linkflow.api.dto.common.Result;
import com.linkflow.workflow.service.CampaignService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

@DubboService
@Service
public class CampaignServiceImpl implements CampaignService {

    @Override
    public Result<Void> updateCampaignStatus(CampaignStatusUpdateDTO dto) {
        return null;
    }
}
