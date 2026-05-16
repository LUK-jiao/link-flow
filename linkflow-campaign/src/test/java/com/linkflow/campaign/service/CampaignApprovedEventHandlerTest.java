package com.linkflow.campaign.service;

import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.shortlink.ShortLinkResultDTO;
import com.linkflow.campaign.event.CampaignApprovedEvent;
import com.linkflow.campaign.handler.CampaignApprovedEventHandler;
import com.linkflow.campaign.mapper.CampaignMapper;
import com.linkflow.campaign.model.Campaign;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignApprovedEventHandlerTest {

    @Mock
    private CampaignMapper campaignMapper;

    @Mock
    private ShortLinkGatewayService shortLinkGatewayService;

    @InjectMocks
    private CampaignApprovedEventHandler eventHandler;

    @Test
    void shouldSkipWhenShortCodeAlreadyExists() {
        Campaign campaign = approvedCampaign(1L, "https://example.com/a");
        campaign.setShortCode("already-bound");
        when(campaignMapper.selectByPrimaryKey(1L)).thenReturn(campaign);

        CampaignApprovedEvent event = new CampaignApprovedEvent();
        event.setCampaignId(1L);
        event.setLongUrl("https://example.com/a");
        eventHandler.handle(event);

        verify(shortLinkGatewayService, never()).createShortLink(ArgumentMatchers.anyString());
        verify(campaignMapper, never()).updateShortCodeIfEmpty(
                ArgumentMatchers.anyLong(), ArgumentMatchers.anyString(), ArgumentMatchers.any());
    }

    @Test
    void shouldCreateAndBindShortCode() {
        Campaign campaign = approvedCampaign(2L, "https://example.com/b");
        when(campaignMapper.selectByPrimaryKey(2L)).thenReturn(campaign);

        ShortLinkResultDTO resultDTO = new ShortLinkResultDTO();
        resultDTO.setShortCode("abc123");
        when(shortLinkGatewayService.createShortLink("https://example.com/b"))
                .thenReturn(Result.success(resultDTO));
        when(campaignMapper.updateShortCodeIfEmpty(ArgumentMatchers.eq(2L), ArgumentMatchers.eq("abc123"), ArgumentMatchers.any()))
                .thenReturn(1);

        CampaignApprovedEvent event = new CampaignApprovedEvent();
        event.setCampaignId(2L);
        event.setLongUrl("https://example.com/b");
        eventHandler.handle(event);

        verify(shortLinkGatewayService, times(1)).createShortLink("https://example.com/b");
        verify(campaignMapper, times(1)).updateShortCodeIfEmpty(
                ArgumentMatchers.eq(2L), ArgumentMatchers.eq("abc123"), ArgumentMatchers.any());
    }

    @Test
    void shouldTreatAsSuccessWhenConcurrentBindingAlreadyDone() {
        Campaign campaign = approvedCampaign(3L, "https://example.com/c");
        Campaign latestCampaign = approvedCampaign(3L, "https://example.com/c");
        latestCampaign.setShortCode("from-concurrent");

        when(campaignMapper.selectByPrimaryKey(3L)).thenReturn(campaign, latestCampaign);
        ShortLinkResultDTO resultDTO = new ShortLinkResultDTO();
        resultDTO.setShortCode("candidate-code");
        when(shortLinkGatewayService.createShortLink("https://example.com/c"))
                .thenReturn(Result.success(resultDTO));
        when(campaignMapper.updateShortCodeIfEmpty(ArgumentMatchers.eq(3L), ArgumentMatchers.eq("candidate-code"), ArgumentMatchers.any()))
                .thenReturn(0);

        CampaignApprovedEvent event = new CampaignApprovedEvent();
        event.setCampaignId(3L);
        event.setLongUrl("https://example.com/c");
        eventHandler.handle(event);

        verify(shortLinkGatewayService, times(1)).createShortLink("https://example.com/c");
        verify(campaignMapper, times(1)).updateShortCodeIfEmpty(
                ArgumentMatchers.eq(3L), ArgumentMatchers.eq("candidate-code"), ArgumentMatchers.any());
    }

    private Campaign approvedCampaign(Long id, String longUrl) {
        Campaign campaign = new Campaign();
        campaign.setId(id);
        campaign.setStatus("APPROVED");
        campaign.setLongUrl(longUrl);
        return campaign;
    }
}
