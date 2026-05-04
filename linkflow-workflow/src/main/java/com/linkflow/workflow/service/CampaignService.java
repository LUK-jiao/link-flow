package com.linkflow.workflow.service;

import com.linkflow.api.dto.campaign.CampaignStatusUpdateDTO;
import com.linkflow.api.dto.common.Result;

public interface CampaignService {

    Result<Void> updateCampaignStatus(CampaignStatusUpdateDTO dto);

}
