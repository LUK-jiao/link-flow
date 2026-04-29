package com.linkflow.campaign.service;

import com.linkflow.api.CampaignApi;
import com.linkflow.api.WorkflowApi;
import com.linkflow.api.dto.campaign.*;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.workflow.WorkflowStartDTO;
import com.linkflow.campaign.mapper.CampaignMapper;
import com.linkflow.campaign.model.Campaign;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Campaign Dubbo 服务实现
 */
@DubboService
public class CampaignServiceImpl implements CampaignApi {

    @Autowired
    private CampaignMapper campaignMapper;

    @DubboReference
    private WorkflowApi workflowApi;

    @Override
    public Result<Long> createCampaign(CampaignCreateDTO dto) {
        Campaign campaign = new Campaign();
        BeanUtils.copyProperties(dto, campaign);
        campaign.setStatus("DRAFT");
        campaign.setCreateTime(new Date());
        campaign.setUpdateTime(new Date());
        
        campaignMapper.insert(campaign);
        return Result.success(campaign.getId());
    }

    @Override
    public Result<CampaignDTO> getCampaignById(Long id) {
        Campaign campaign = campaignMapper.selectByPrimaryKey(id);
        if (campaign == null) {
            return Result.fail("活动不存在");
        }
        
        CampaignDTO dto = convertToDTO(campaign);
        return Result.success(dto);
    }

    @Override
    public Result<List<CampaignDTO>> getCampaignList(CampaignQueryDTO query) {
        // TODO: 实现分页查询，当前返回全部
        List<Campaign> campaigns = campaignMapper.selectAll();
        
        // 按条件过滤
        List<CampaignDTO> dtos = campaigns.stream()
                .filter(c -> query.getCreatorUserId() == null || c.getCreatorUserId().equals(query.getCreatorUserId()))
                .filter(c -> query.getCampaignType() == null || c.getCampaignType().equals(query.getCampaignType()))
                .filter(c -> query.getStatus() == null || c.getStatus().equals(query.getStatus()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return Result.success(dtos);
    }

    @Override
    public Result<Void> submitCampaign(Long id) {
        Campaign campaign = campaignMapper.selectByPrimaryKey(id);
        if (campaign == null) {
            return Result.fail("活动不存在");
        }
        
        if (!"DRAFT".equals(campaign.getStatus())) {
            return Result.fail("只有草稿状态的活动可以提交审批");
        }
        
        // 更新状态为审批中
        campaign.setStatus("APPROVING");
        campaign.setUpdateTime(new Date());
        campaignMapper.updateByPrimaryKey(campaign);
        
        // 启动工作流
        WorkflowStartDTO workflowDTO = new WorkflowStartDTO();
        workflowDTO.setBusinessKey(id);
        workflowDTO.setBusinessType("CAMPAIGN_APPROVAL");
        workflowDTO.setInitiatorId(campaign.getCreatorUserId());
        
        Result<String> workflowResult = workflowApi.startApprovalProcess(workflowDTO);
        if (workflowResult.getCode() != 200) {
            // 回滚状态
            campaign.setStatus("DRAFT");
            campaignMapper.updateByPrimaryKey(campaign);
            return Result.fail("启动审批流程失败: " + workflowResult.getMessage());
        }
        
        return Result.success();
    }

    @Override
    public Result<Void> updateCampaignStatus(CampaignStatusUpdateDTO dto) {
        Campaign campaign = campaignMapper.selectByPrimaryKey(dto.getCampaignId());
        if (campaign == null) {
            return Result.fail("活动不存在");
        }
        
        if (!"APPROVING".equals(campaign.getStatus())) {
            return Result.fail("只有审批中的活动可以更新状态");
        }
        
        campaign.setStatus(dto.getStatus());
        if ("REJECTED".equals(dto.getStatus())) {
            campaign.setRejectReason(dto.getRejectReason());
        }
        campaign.setUpdateTime(new Date());
        
        campaignMapper.updateByPrimaryKey(campaign);
        return Result.success();
    }

    @Override
    public Result<Void> bindShortCode(Long campaignId, String shortCode) {
        Campaign campaign = campaignMapper.selectByPrimaryKey(campaignId);
        if (campaign == null) {
            return Result.fail("活动不存在");
        }
        
        // TODO: 实际应该有 shortCode 字段，当前 Model 没有
        // 这里假设 Campaign 表有 short_code 字段，需要后续补充
        campaign.setUpdateTime(new Date());
        campaignMapper.updateByPrimaryKey(campaign);
        
        return Result.success();
    }

    @Override
    public Result<Void> deleteCampaign(Long id) {
        Campaign campaign = campaignMapper.selectByPrimaryKey(id);
        if (campaign == null) {
            return Result.fail("活动不存在");
        }
        
        if (!"DRAFT".equals(campaign.getStatus())) {
            return Result.fail("只有草稿状态的活动可以删除");
        }
        
        campaignMapper.deleteByPrimaryKey(id);
        return Result.success();
    }

    @Override
    public Result<Void> cancelCampaign(Long id) {
        Campaign campaign = campaignMapper.selectByPrimaryKey(id);
        if (campaign == null) {
            return Result.fail("活动不存在");
        }
        
        String status = campaign.getStatus();
        if (!"DRAFT".equals(status) && !"APPROVING".equals(status)) {
            return Result.fail("只有草稿或审批中的活动可以取消");
        }
        
        campaign.setStatus("CANCELLED");
        campaign.setUpdateTime(new Date());
        campaignMapper.updateByPrimaryKey(campaign);
        
        return Result.success();
    }

    /**
     * Model 转 DTO
     */
    private CampaignDTO convertToDTO(Campaign campaign) {
        CampaignDTO dto = new CampaignDTO();
        BeanUtils.copyProperties(campaign, dto);
        return dto;
    }
}