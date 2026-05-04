package com.linkflow.workflow.service.impl;

import com.linkflow.api.ApproverConfigApi;
import com.linkflow.api.UserApi;
import com.linkflow.workflow.service.ApproverService;
import lombok.Data;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.List;

@DubboService
@Service
public class ApproverServiceImpl implements ApproverService {
    @DubboReference
    private ApproverConfigApi approverConfigApi;

    @Override
    public List<Long> getApproverIds(String campaign_type, int approver_level) {
        return List.of();
    }
}
