package com.linkflow.campaign.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkflow.campaign.event.CampaignApprovedEvent;
import com.linkflow.campaign.handler.CampaignApprovedEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CampaignApprovedEventConsumer {

    private final ObjectMapper objectMapper;

    private final CampaignApprovedEventHandler eventHandler;

    public CampaignApprovedEventConsumer(ObjectMapper objectMapper,
                                         CampaignApprovedEventHandler eventHandler) {
        this.objectMapper = objectMapper;
        this.eventHandler = eventHandler;
    }

    @KafkaListener(
            topics = "${linkflow.campaign.kafka.topics.campaign-approved:campaign.approved}",
            groupId = "${linkflow.campaign.kafka.consumer-group:linkflow-campaign-shortlink}"
    )
    public void consume(String payload) {
        try {
            CampaignApprovedEvent event = objectMapper.readValue(payload, CampaignApprovedEvent.class);
            log.info("收到活动审批通过事件, campaignId={}", event.getCampaignId());
            eventHandler.handle(event);
        } catch (Exception e) {
            log.error("处理活动审批通过事件失败, payload={}", payload, e);
            throw new IllegalStateException("处理活动审批通过事件失败", e);
        }
    }
}
