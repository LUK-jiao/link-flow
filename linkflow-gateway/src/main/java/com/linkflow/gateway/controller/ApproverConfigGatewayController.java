package com.linkflow.gateway.controller;

import com.linkflow.api.ApproverConfigApi;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.user.ApproverDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "ApproverConfig", description = "审批人配置接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/approvers")
public class ApproverConfigGatewayController {

    private final ApproverConfigApi approverConfigApi;

    @Operation(summary = "按活动类型查询审批人配置")
    @GetMapping
    public Result<List<ApproverDTO>> getApproverByType(
            @Parameter(description = "活动类型", required = true)
            @RequestParam("campaignType") String campaignType) {
        return approverConfigApi.getApproverByType(campaignType);
    }

    @Operation(summary = "按活动类型和级别查询审批人配置")
    @GetMapping("/by-level")
    public Result<List<ApproverDTO>> getApproverByTypeAndLevel(
            @Parameter(description = "活动类型", required = true)
            @RequestParam("campaignType") String campaignType,
            @Parameter(description = "审批级别(1/2)", required = true)
            @RequestParam("level") Integer level) {
        return approverConfigApi.getApproverByTypeAndLevel(campaignType, level);
    }

    @Operation(summary = "配置审批人")
    @PostMapping
    public Result<Long> configApprover(@RequestBody ApproverDTO dto) {
        return approverConfigApi.configApprover(dto);
    }

    @Operation(summary = "删除审批人配置")
    @DeleteMapping("/{id}")
    public Result<Void> deleteApproverConfig(
            @Parameter(description = "配置ID", required = true)
            @PathVariable("id") Long id) {
        return approverConfigApi.deleteApproverConfig(id);
    }
}
