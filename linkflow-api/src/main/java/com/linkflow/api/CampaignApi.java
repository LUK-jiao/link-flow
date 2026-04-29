package com.linkflow.api;

import com.linkflow.api.dto.campaign.CampaignCreateDTO;
import com.linkflow.api.dto.campaign.CampaignDTO;
import com.linkflow.api.dto.campaign.CampaignQueryDTO;
import com.linkflow.api.dto.campaign.CampaignStatusUpdateDTO;
import com.linkflow.api.dto.common.Result;

import java.util.List;

/**
 * Campaign 提供服务接口
 */
public interface CampaignApi {

    /**
     * 创建活动
     */
    Result<Long> createCampaign(CampaignCreateDTO dto);

    /**
     * 根据ID查询活动
     */
    Result<CampaignDTO> getCampaignById(Long id);

    /**
     * 查询活动列表
     */
    Result<List<CampaignDTO>> getCampaignList(CampaignQueryDTO query);

    /**
     * 提交审批
     */
    Result<Void> submitCampaign(Long id);

    /**
     * 更新活动状态（审批回调）
     */
    Result<Void> updateCampaignStatus(CampaignStatusUpdateDTO dto);

    /**
     * 绑定短链码
     */
    Result<Void> bindShortCode(Long campaignId, String shortCode);

    /**
     * 删除活动
     */
    Result<Void> deleteCampaign(Long id);

    /**
     * 取消活动
     */
    Result<Void> cancelCampaign(Long id);
}