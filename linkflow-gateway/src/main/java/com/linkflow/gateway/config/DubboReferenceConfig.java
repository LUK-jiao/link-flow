package com.linkflow.gateway.config;

import com.linkflow.api.ApproverConfigApi;
import com.linkflow.api.AgentApi;
import com.linkflow.api.CampaignApi;
import com.linkflow.api.ShortLinkApi;
import com.linkflow.api.UserApi;
import com.linkflow.api.WorkflowApi;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DubboReferenceConfig {

    @Bean
    @DubboReference(interfaceClass = AgentApi.class, check = false, timeout = 180000, retries = 0)
    public ReferenceBean<AgentApi> agentApi() {
        return new ReferenceBean<>();
    }

    @Bean
    @DubboReference(interfaceClass = UserApi.class, check = false)
    public ReferenceBean<UserApi> userApi() {
        return new ReferenceBean<>();
    }

    @Bean
    @DubboReference(interfaceClass = ApproverConfigApi.class, check = false)
    public ReferenceBean<ApproverConfigApi> approverConfigApi() {
        return new ReferenceBean<>();
    }

    @Bean
    @DubboReference(interfaceClass = CampaignApi.class, check = false)
    public ReferenceBean<CampaignApi> campaignApi() {
        return new ReferenceBean<>();
    }

    @Bean
    @DubboReference(interfaceClass = WorkflowApi.class, check = false)
    public ReferenceBean<WorkflowApi> workflowApi() {
        return new ReferenceBean<>();
    }

    @Bean
    @DubboReference(interfaceClass = ShortLinkApi.class, check = false)
    public ReferenceBean<ShortLinkApi> shortLinkApi() {
        return new ReferenceBean<>();
    }
}
