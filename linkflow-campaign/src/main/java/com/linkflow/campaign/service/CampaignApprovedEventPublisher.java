package com.linkflow.campaign.service;

import com.linkflow.campaign.event.CampaignApprovedEvent;

public interface CampaignApprovedEventPublisher {

    void publish(CampaignApprovedEvent event);
}
