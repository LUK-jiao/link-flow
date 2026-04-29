package com.linkflow.user.service;

import com.linkflow.api.ApproverConfigApi;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.user.ApproverDTO;
import com.linkflow.user.mapper.ApproverConfigMapper;
import com.linkflow.user.mapper.UserMapper;
import com.linkflow.user.model.ApproverConfig;
import com.linkflow.user.model.User;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ApproverConfig Dubbo 服务实现
 */
@DubboService
@Service
public class ApproverConfigApiImpl implements ApproverConfigApi {

    @Autowired
    private ApproverConfigMapper approverConfigMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Result<List<ApproverDTO>> getApproverByType(String campaignType) {
        List<ApproverConfig> configs = approverConfigMapper.selectAll()
                .stream()
                .filter(c -> c.getCampaignType().equals(campaignType))
                .collect(Collectors.toList());

        List<ApproverDTO> dtos = configs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return Result.success(dtos);
    }

    @Override
    public Result<Long> configApprover(ApproverDTO dto) {
        // 检查审批人是否存在
        User approver = userMapper.selectByPrimaryKey(dto.getApproverId());
        if (approver == null) {
            return Result.fail("审批人不存在");
        }

        // 检查审批人角色
        if (!"APPROVER".equals(approver.getRole()) && !"ADMIN".equals(approver.getRole())) {
            return Result.fail("该用户不是审批人");
        }

        ApproverConfig config = new ApproverConfig();
        config.setCampaignType(dto.getCampaignType());
        config.setApproverId(dto.getApproverId());
        config.setApproverLevel(dto.getApproverLevel() != null ? dto.getApproverLevel() : 1);
        config.setCreateTime(new Date());
        config.setUpdateTime(new Date());

        approverConfigMapper.insert(config);
        return Result.success(config.getId());
    }

    @Override
    public Result<Void> deleteApproverConfig(Long id) {
        ApproverConfig config = approverConfigMapper.selectByPrimaryKey(id);
        if (config == null) {
            return Result.fail("配置不存在");
        }

        approverConfigMapper.deleteByPrimaryKey(id);
        return Result.success();
    }

    private ApproverDTO convertToDTO(ApproverConfig config) {
        ApproverDTO dto = new ApproverDTO();
        dto.setId(config.getId());
        dto.setCampaignType(config.getCampaignType());
        dto.setApproverId(config.getApproverId());
        dto.setApproverLevel(config.getApproverLevel());

        // 查询审批人姓名
        User user = userMapper.selectByPrimaryKey(config.getApproverId());
        if (user != null) {
            dto.setApproverName(user.getUsername());
        }

        return dto;
    }
}