package com.linkflow.api;

import com.linkflow.api.dto.user.ApproverDTO;
import com.linkflow.api.dto.common.Result;

import java.util.List;

/**
 * 审批人配置服务接口
 */
public interface ApproverConfigApi {

    /**
     * 根据活动类型查询审批人配置
     */
    Result<List<ApproverDTO>> getApproverByType(String campaignType);

    /**
     * 配置审批人
     */
    Result<Long> configApprover(ApproverDTO dto);

    /**
     * 删除审批人配置
     */
    Result<Void> deleteApproverConfig(Long id);
}