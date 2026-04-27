package com.linkFlow.user.service.impl;

import com.linkFlow.user.dto.ApproverConfigDTO;
import com.linkFlow.user.mapper.ApproverConfigMapper;
import com.linkFlow.user.mapper.UserMapper;
import com.linkFlow.user.model.ApproverConfig;
import com.linkFlow.user.model.User;
import com.linkFlow.user.service.ApproverConfigService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApproverConfigServiceImpl implements ApproverConfigService {

    private final ApproverConfigMapper approverConfigMapper;
    private final UserMapper userMapper;

    public ApproverConfigServiceImpl(ApproverConfigMapper approverConfigMapper, UserMapper userMapper) {
        this.approverConfigMapper = approverConfigMapper;
        this.userMapper = userMapper;
    }

    @Override
    public Long configApprover(ApproverConfigDTO dto) {
        // 检查审批人是否存在
        User approver = userMapper.selectByPrimaryKey(dto.getApproverId());
        if (approver == null) {
            throw new RuntimeException("审批人不存在");
        }

        // 检查审批人角色是否为 APPROVER 或 ADMIN
        if (!"APPROVER".equals(approver.getRole()) && !"ADMIN".equals(approver.getRole())) {
            throw new RuntimeException("该用户不是审批人");
        }

        ApproverConfig config = new ApproverConfig();
        config.setCampaignType(dto.getCampaignType());
        config.setApproverId(dto.getApproverId());
        config.setApproverLevel(dto.getApproverLevel() != null ? dto.getApproverLevel() : 1);

        approverConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    public List<ApproverConfig> getByCampaignType(String campaignType) {
        return approverConfigMapper.selectAll()
                .stream()
                .filter(c -> c.getCampaignType().equals(campaignType))
                .collect(Collectors.toList());
    }
}