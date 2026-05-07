package com.linkflow.campaign.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linkflow.api.CampaignApi;
import com.linkflow.api.dto.campaign.*;
import com.linkflow.api.dto.common.PageResult;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.user.UserDTO;
import com.linkflow.api.dto.workflow.WorkflowStartDTO;
import com.linkflow.campaign.mapper.CampaignMapper;
import com.linkflow.campaign.model.Campaign;
import com.linkflow.campaign.service.UserAndWorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Campaign Dubbo 服务实现
 */
@DubboService
@Service
@Slf4j
public class CampaignApiImpl implements CampaignApi {

    @Autowired
    private CampaignMapper campaignMapper;

    @Autowired
    private UserAndWorkflowService userAndWorkflowService;

    @Override
    public Result<Long> createCampaign(CampaignCreateDTO dto) {
        if (dto.getCreatorUserId() == null) {
            return Result.fail("创建人ID不能为空");
        }

        Result<UserDTO> userResult = userAndWorkflowService.getUserById(dto.getCreatorUserId());
        if (userResult == null || !userResult.isSuccess() || userResult.getData() == null) {
            return Result.fail("创建人不存在");
        }

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
    public Result<PageResult<CampaignDTO>> getCampaignList(CampaignQueryDTO query) {
        log.info("get campaign list,CampaignQueryDto =  {}", JSON.toJSONString(query));
        Page<Campaign> page = new Page<>(query.getPageNum(),  query.getPageSize());
        QueryWrapper<Campaign> queryWrapper = new QueryWrapper<>();
        if(query.getCreatorUserId() != null ) {
            queryWrapper.eq("creator_user_id", query.getCreatorUserId());
        }
        if(query.getStatus() != null) {
            queryWrapper.eq("status", query.getStatus());
        }
        if(query.getCampaignType() != null) {
            queryWrapper.eq("campaign_type", query.getCampaignType());
        }
        IPage<Campaign> res;
        try{
            res = campaignMapper.selectPage(page,queryWrapper);
        }catch (Exception e){
            log.error("get campaign list error", e);
            throw new RuntimeException("get campaign list error");
        }

        List<CampaignDTO> campaignDTOList = res.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        PageResult<CampaignDTO> pageResult = new PageResult<>();
        pageResult.setRecords(campaignDTOList);
        pageResult.setTotal(res.getTotal());
        pageResult.setPageNum(res.getCurrent());
        pageResult.setPageSize(res.getSize());
        pageResult.setPages(res.getPages());

        return Result.success(pageResult);
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
        workflowDTO.setCampaignType(campaign.getCampaignType());
        workflowDTO.setInitiatorId(campaign.getCreatorUserId());
        
        Result<String> workflowResult = userAndWorkflowService.startApprovalProcess(workflowDTO);
        if (workflowResult == null || !workflowResult.isSuccess()) {
            // 回滚状态
            campaign.setStatus("DRAFT");
            campaignMapper.updateByPrimaryKey(campaign);
            return Result.fail("启动审批流程失败: " + (workflowResult != null ? workflowResult.getMessage() : "远程调用无响应"));
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
        
        campaign.setShortCode(shortCode);
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
