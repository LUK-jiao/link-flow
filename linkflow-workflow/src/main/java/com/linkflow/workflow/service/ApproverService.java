package com.linkflow.workflow.service;

import java.util.List;

public interface ApproverService {

    /**
     * 从user获取配置的审批人名单
     * @return List
     */
    List<Long> getApproverIds(String campaign_type,int approver_level);
}
