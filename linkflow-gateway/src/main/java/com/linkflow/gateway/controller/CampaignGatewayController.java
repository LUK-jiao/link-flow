package com.linkflow.gateway.controller;

import com.linkflow.api.CampaignApi;
import com.linkflow.api.dto.campaign.CampaignCreateDTO;
import com.linkflow.api.dto.campaign.CampaignDTO;
import com.linkflow.api.dto.campaign.CampaignQueryDTO;
import com.linkflow.api.dto.campaign.CampaignStatusUpdateDTO;
import com.linkflow.api.dto.common.PageResult;
import com.linkflow.api.dto.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Campaign", description = "活动管理接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/campaigns")
public class CampaignGatewayController {

    private final CampaignApi campaignApi;

    @Operation(summary = "创建活动")
    @PostMapping
    public Result<Long> createCampaign(@RequestBody CampaignCreateDTO dto) {
        return campaignApi.createCampaign(dto);
    }

    @Operation(summary = "根据活动ID查询活动")
    @GetMapping("/{id}")
    public Result<CampaignDTO> getCampaignById(
            @Parameter(description = "活动ID", required = true)
            @PathVariable("id") Long id) {
        return campaignApi.getCampaignById(id);
    }

    @Operation(summary = "分页查询活动")
    @PostMapping("/query")
    public Result<PageResult<CampaignDTO>> getCampaignList(@RequestBody CampaignQueryDTO query) {
        return campaignApi.getCampaignList(query);
    }

    @Operation(summary = "提交活动审批")
    @PostMapping("/{id}/submit")
    public Result<Void> submitCampaign(
            @Parameter(description = "活动ID", required = true)
            @PathVariable("id") Long id) {
        return campaignApi.submitCampaign(id);
    }

    @Operation(summary = "更新活动状态（审批回调）")
    @PutMapping("/status")
    public Result<Void> updateCampaignStatus(@RequestBody CampaignStatusUpdateDTO dto) {
        return campaignApi.updateCampaignStatus(dto);
    }

    @Operation(summary = "绑定短链码到活动")
    @PutMapping("/{id}/short-code")
    public Result<Void> bindShortCode(
            @Parameter(description = "活动ID", required = true)
            @PathVariable("id") Long id,
            @RequestBody BindShortCodeRequest request) {
        return campaignApi.bindShortCode(id, request.shortCode());
    }

    @Operation(summary = "删除活动")
    @DeleteMapping("/{id}")
    public Result<Void> deleteCampaign(
            @Parameter(description = "活动ID", required = true)
            @PathVariable("id") Long id) {
        return campaignApi.deleteCampaign(id);
    }

    @Operation(summary = "取消活动")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelCampaign(
            @Parameter(description = "活动ID", required = true)
            @PathVariable("id") Long id) {
        return campaignApi.cancelCampaign(id);
    }

    public record BindShortCodeRequest(String shortCode) {
    }
}
