package com.linkFlow.user.controller;

import com.linkFlow.user.common.Result;
import com.linkFlow.user.dto.ApproverConfigDTO;
import com.linkFlow.user.model.ApproverConfig;
import com.linkFlow.user.service.ApproverConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/approver")
public class ApproverConfigController {

    private final ApproverConfigService approverConfigService;

    public ApproverConfigController(ApproverConfigService approverConfigService) {
        this.approverConfigService = approverConfigService;
    }

    /**
     * 配置审批人
     * POST /approver/config
     */
    @PostMapping("/config")
    public Result<Long> configApprover(@RequestBody ApproverConfigDTO dto) {
        try {
            Long configId = approverConfigService.configApprover(dto);
            return Result.success(configId);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 查询某活动类型的审批人配置
     * GET /approver/config/{campaignType}
     */
    @GetMapping("/config/{campaignType}")
    public Result<List<ApproverConfig>> getByCampaignType(@PathVariable String campaignType) {
        List<ApproverConfig> configs = approverConfigService.getByCampaignType(campaignType);
        return Result.success(configs);
    }
}