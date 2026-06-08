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
import com.linkflow.api.enums.CampaignTypeEnum;
import com.linkflow.campaign.event.CampaignApprovedEvent;
import com.linkflow.campaign.mapper.CampaignMapper;
import com.linkflow.campaign.model.Campaign;
import com.linkflow.campaign.service.CampaignApprovedEventPublisher;
import com.linkflow.campaign.service.UserAndWorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Campaign Dubbo 服务实现
 */
@DubboService
@Service
@Slf4j
public class CampaignApiImpl implements CampaignApi {

    private static final Pattern NAME_TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9]+|[\\u4e00-\\u9fa5]+");

    @Autowired
    private CampaignMapper campaignMapper;

    @Autowired
    private UserAndWorkflowService userAndWorkflowService;

    @Autowired
    private CampaignApprovedEventPublisher campaignApprovedEventPublisher;

    @Override
    public Result<Long> createCampaign(CampaignCreateDTO dto) {
        CampaignTypeEnum campaignType = CampaignTypeEnum.ofCode(dto.getCampaignType());
        if (campaignType == null) {
            return Result.fail("活动类型不合法，可选值：" + CampaignTypeEnum.validCodesText());
        }
        if (dto.getCreatorUserId() == null) {
            return Result.fail("创建人ID不能为空");
        }
        if (dto.getLongUrl() == null || dto.getLongUrl().isBlank()) {
            return Result.fail("长链接不能为空");
        }

        Result<UserDTO> userResult = userAndWorkflowService.getUserById(dto.getCreatorUserId());
        if (userResult == null || !userResult.isSuccess() || userResult.getData() == null) {
            return Result.fail("创建人不存在");
        }

        Campaign campaign = new Campaign();
        BeanUtils.copyProperties(dto, campaign);
        campaign.setCampaignType(campaignType.getCode());
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
        if(StringUtils.hasText(query.getName())) {
            applyNameQuery(queryWrapper, query.getName());
        }
        if(query.getStatus() != null) {
            queryWrapper.eq("status", query.getStatus());
        }
        if(StringUtils.hasText(query.getCampaignType())) {
            CampaignTypeEnum campaignType = CampaignTypeEnum.ofCode(query.getCampaignType());
            if (campaignType == null) {
                return Result.fail("活动类型不合法，可选值：" + CampaignTypeEnum.validCodesText());
            }
            queryWrapper.eq("campaign_type", campaignType.getCode());
        }
        queryWrapper.orderByDesc("create_time", "id");
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
    public Result<PageResult<CampaignDTO>> getVisibleCampaignList(CampaignVisibleQueryDTO query) {
        log.info("get visible campaign list, CampaignVisibleQueryDTO={}", JSON.toJSONString(query));
        if (query == null || query.getUserId() == null) {
            return Result.fail(400, "用户ID不能为空");
        }

        Page<Campaign> page = new Page<>(query.getPageNum(), query.getPageSize());
        QueryWrapper<Campaign> queryWrapper = new QueryWrapper<>();
        List<String> approverCampaignTypes = normalizeCampaignTypes(query.getApproverCampaignTypes());

        queryWrapper.and(wrapper -> {
            wrapper.eq("creator_user_id", query.getUserId());
            if (!approverCampaignTypes.isEmpty()) {
                wrapper.or().in("campaign_type", approverCampaignTypes);
            }
        });
        if (StringUtils.hasText(query.getName())) {
            applyNameQuery(queryWrapper, query.getName());
        }
        if (StringUtils.hasText(query.getStatus())) {
            queryWrapper.eq("status", query.getStatus());
        }
        if (StringUtils.hasText(query.getCampaignType())) {
            CampaignTypeEnum campaignType = CampaignTypeEnum.ofCode(query.getCampaignType());
            if (campaignType == null) {
                return Result.fail("活动类型不合法，可选值：" + CampaignTypeEnum.validCodesText());
            }
            queryWrapper.eq("campaign_type", campaignType.getCode());
        }
        queryWrapper.orderByDesc("create_time", "id");

        IPage<Campaign> res;
        try {
            res = campaignMapper.selectPage(page, queryWrapper);
        } catch (Exception exception) {
            log.error("get visible campaign list error", exception);
            throw new RuntimeException("get visible campaign list error");
        }

        PageResult<CampaignDTO> pageResult = new PageResult<>();
        pageResult.setRecords(res.getRecords().stream().map(this::convertToDTO).collect(Collectors.toList()));
        pageResult.setTotal(res.getTotal());
        pageResult.setPageNum(res.getCurrent());
        pageResult.setPageSize(res.getSize());
        pageResult.setPages(res.getPages());
        return Result.success(pageResult);
    }

    private List<String> normalizeCampaignTypes(List<String> campaignTypes) {
        if (campaignTypes == null || campaignTypes.isEmpty()) {
            return List.of();
        }
        return campaignTypes.stream()
                .map(CampaignTypeEnum::ofCode)
                .filter(type -> type != null)
                .map(CampaignTypeEnum::getCode)
                .distinct()
                .collect(Collectors.toList());
    }

    private void applyNameQuery(QueryWrapper<Campaign> queryWrapper, String name) {
        List<String> tokens = extractNameTokens(name);
        if (tokens.isEmpty()) {
            queryWrapper.like("name", name.trim());
            return;
        }
        queryWrapper.and(wrapper -> {
            for (String token : tokens) {
                wrapper.like("name", token);
            }
        });
    }

    private List<String> extractNameTokens(String name) {
        String normalized = name == null ? "" : name.trim()
                .replaceFirst("^(我创建的|我参与的|我审批的|创建的|参与的|审批的)", "");
        Matcher matcher = NAME_TOKEN_PATTERN.matcher(normalized);
        List<String> tokens = new ArrayList<>();
        while (matcher.find()) {
            String token = matcher.group().trim();
            if (StringUtils.hasText(token) && !"活动".equals(token)) {
                tokens.add(token);
            }
        }
        return tokens;
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
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateCampaignStatus(CampaignStatusUpdateDTO dto) {
        if (dto == null || dto.getCampaignId() == null || dto.getStatus() == null || dto.getStatus().isBlank()) {
            return Result.fail("活动状态更新参数不完整");
        }

        Campaign campaign = campaignMapper.selectByPrimaryKey(dto.getCampaignId());
        if (campaign == null) {
            return Result.fail("活动不存在");
        }

        if (!"APPROVING".equals(campaign.getStatus())) {
            return Result.fail("只有审批中的活动可以更新状态");
        }

        if (!"APPROVED".equals(dto.getStatus()) && !"REJECTED".equals(dto.getStatus())) {
            return Result.fail("仅支持更新为 APPROVED 或 REJECTED");
        }

        if ("APPROVED".equals(dto.getStatus()) && (campaign.getLongUrl() == null || campaign.getLongUrl().isBlank())) {
            return Result.fail("活动长链接为空，无法完成审批通过回调");
        }

        campaign.setStatus(dto.getStatus());
        if ("REJECTED".equals(dto.getStatus())) {
            campaign.setRejectReason(dto.getRejectReason());
        }
        campaign.setUpdateTime(new Date());

        campaignMapper.updateByPrimaryKey(campaign);

        if ("APPROVED".equals(dto.getStatus())) {
            CampaignApprovedEvent event = new CampaignApprovedEvent();
            event.setCampaignId(campaign.getId());
            event.setLongUrl(campaign.getLongUrl());
            campaignApprovedEventPublisher.publish(event);
        }

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
