package com.linkflow.campaign.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkflow.campaign.event.CampaignApprovedEvent;
import com.linkflow.campaign.service.CampaignApprovedEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CampaignApprovedEventPublisherImpl implements CampaignApprovedEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper;

    private final String topic;

    public CampaignApprovedEventPublisherImpl(KafkaTemplate<String, String> kafkaTemplate,
                                              ObjectMapper objectMapper,
                                              @Value("${linkflow.campaign.kafka.topics.campaign-approved:campaign.approved}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publish(CampaignApprovedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            log.info("发送活动审批通过事件, topic={}, campaignId={}", topic, event.getCampaignId());
            kafkaTemplate.send(topic, String.valueOf(event.getCampaignId()), payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化审批通过事件失败", e);
        } catch (Exception e) {
            throw new IllegalStateException("发送审批通过事件失败", e);
        }
    }
}
