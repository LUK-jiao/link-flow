package com.linkFlow.user.service;

import com.linkFlow.user.dto.ApproverConfigDTO;
import com.linkFlow.user.model.ApproverConfig;

import java.util.List;

public interface ApproverConfigService {
    /**
     * 配置审批人
     */
    Long configApprover(ApproverConfigDTO dto);

    /**
     * 根据活动类型查询审批人配置
     */
    List<ApproverConfig> getByCampaignType(String campaignType);
}