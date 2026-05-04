package com.linkflow.workflow.service.impl;

import com.linkflow.api.ApproverConfigApi;
import com.linkflow.api.dto.user.ApproverDTO;
import com.linkflow.api.dto.common.Result;
import com.linkflow.workflow.service.ApproverService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApproverServiceImpl implements ApproverService {

    private static final Logger log = LoggerFactory.getLogger(ApproverServiceImpl.class);

    @DubboReference
    private ApproverConfigApi approverConfigApi;

    @Override
    public List<Long> getApproverIds(String campaignType, int approverLevel) {
        log.info("com.linkflow.workflow.service.impl.ApproverServiceImpl#getApproverIds 调用 ApproverConfigApi.getApproverByTypeAndLevel, campaignType={}, level={}", 
                campaignType, approverLevel);
        
        Result<List<ApproverDTO>> result = approverConfigApi.getApproverByTypeAndLevel(campaignType, approverLevel);
        
        if (result == null || !result.isSuccess() || result.getData() == null) {
            log.warn("获取审批人失败: campaignType={}, level={}, result={}", 
                    campaignType, approverLevel, result != null ? result.getMessage() : "null");
            return Collections.emptyList();
        }
        
        List<Long> approverIds = result.getData().stream()
                .map(ApproverDTO::getApproverId)
                .collect(Collectors.toList());
        
        log.info("获取审批人成功: campaignType={}, level={}, approverIds={}", 
                campaignType, approverLevel, approverIds);
        
        return approverIds;
    }
}
