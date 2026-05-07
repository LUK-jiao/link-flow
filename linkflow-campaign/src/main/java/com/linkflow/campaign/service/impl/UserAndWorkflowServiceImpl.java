package com.linkflow.campaign.service.impl;

import com.alibaba.fastjson2.JSON;
import com.linkflow.api.UserApi;
import com.linkflow.api.WorkflowApi;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.user.UserDTO;
import com.linkflow.api.dto.workflow.WorkflowStartDTO;
import com.linkflow.campaign.service.UserAndWorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserAndWorkflowServiceImpl implements UserAndWorkflowService {

    @DubboReference(check = false)
    private UserApi userApi;

    @DubboReference(check = false)
    private WorkflowApi workflowApi;

    @Override
    public Result<UserDTO> getUserById(Long userId) {
        log.info("调用 UserApi.getUserById, userId={}",
                userId);

        Result<UserDTO> result = userApi.getUserById(userId);
        if (result != null && result.isSuccess() && result.getData() != null) {
            log.info("查询用户成功, userId={}, username={}", userId, result.getData().getUsername());
        } else {
            log.warn("查询用户失败, userId={}, result={}", userId, result != null ? result.getMessage() : "null");
        }

        return result;
    }

    @Override
    public Result<String> startApprovalProcess(WorkflowStartDTO dto) {
        log.info("调用 WorkflowApi.startApprovalProcess，dto = {}", JSON.toJSONString(dto));

        Result<String> result = workflowApi.startApprovalProcess(dto);
        if (result != null && result.isSuccess()) {
            log.info("启动审批流程成功, businessKey={}, processInstanceId={}", dto.getBusinessKey(), result.getData());
        } else {
            log.error("启动审批流程失败, businessKey={}, result={}",
                    dto.getBusinessKey(), result != null ? result.getMessage() : "null");
        }

        return result;
    }
}
