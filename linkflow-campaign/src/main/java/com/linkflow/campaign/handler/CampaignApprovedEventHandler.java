package com.linkflow.campaign.handler;

import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.shortlink.ShortLinkResultDTO;
import com.linkflow.campaign.event.CampaignApprovedEvent;
import com.linkflow.campaign.mapper.CampaignMapper;
import com.linkflow.campaign.model.Campaign;
import com.linkflow.campaign.service.ShortLinkGatewayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Slf4j
public class CampaignApprovedEventHandler {

    private final CampaignMapper campaignMapper;

    private final ShortLinkGatewayService shortLinkGatewayService;

    public CampaignApprovedEventHandler(CampaignMapper campaignMapper,
                                        ShortLinkGatewayService shortLinkGatewayService) {
        this.campaignMapper = campaignMapper;
        this.shortLinkGatewayService = shortLinkGatewayService;
    }

    public void handle(CampaignApprovedEvent event) {
        if (event == null || event.getCampaignId() == null) {
            log.warn("忽略空的审批通过事件: {}", event);
            return;
        }

        Campaign campaign = campaignMapper.selectByPrimaryKey(event.getCampaignId());
        if (campaign == null) {
            log.warn("审批通过事件对应活动不存在, campaignId={}", event.getCampaignId());
            return;
        }

        if (!"APPROVED".equals(campaign.getStatus())) {
            log.info("活动当前非 APPROVED，跳过建短链, campaignId={}, status={}",
                    campaign.getId(), campaign.getStatus());
            return;
        }

        if (hasText(campaign.getShortCode())) {
            log.info("活动已绑定短链，跳过重复处理, campaignId={}, shortCode={}",
                    campaign.getId(), campaign.getShortCode());
            return;
        }

        String longUrl = hasText(campaign.getLongUrl()) ? campaign.getLongUrl() : event.getLongUrl();
        if (!hasText(longUrl)) {
            throw new IllegalStateException("活动长链接为空，无法创建短链, campaignId=" + campaign.getId());
        }

        Result<ShortLinkResultDTO> createResult = shortLinkGatewayService.createShortLink(longUrl);
        if (createResult == null || !createResult.isSuccess() || createResult.getData() == null
                || !hasText(createResult.getData().getShortCode())) {
            String message = createResult == null ? "null result" : createResult.getMessage();
            throw new IllegalStateException("创建短链失败, campaignId=" + campaign.getId() + ", message=" + message);
        }

        String shortCode = createResult.getData().getShortCode();
        int updated = campaignMapper.updateShortCodeIfEmpty(campaign.getId(), shortCode, new Date());
        if (updated == 1) {
            log.info("活动绑定短链成功, campaignId={}, shortCode={}", campaign.getId(), shortCode);
            return;
        }

        Campaign latest = campaignMapper.selectByPrimaryKey(campaign.getId());
        if (latest != null && hasText(latest.getShortCode())) {
            log.info("活动短链已由并发/重试完成绑定，视为成功, campaignId={}, shortCode={}",
                    latest.getId(), latest.getShortCode());
            return;
        }

        if (latest != null && !"APPROVED".equals(latest.getStatus())) {
            log.info("活动状态已变化，跳过绑定, campaignId={}, status={}", latest.getId(), latest.getStatus());
            return;
        }

        throw new IllegalStateException("绑定短链失败且未命中幂等条件, campaignId=" + campaign.getId());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
