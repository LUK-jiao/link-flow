package com.linkflow.campaign.service;

import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.user.UserDTO;
import com.linkflow.api.dto.workflow.WorkflowStartDTO;

public interface UserAndWorkflowService {

    Result<UserDTO> getUserById(Long userId);

    Result<String> startApprovalProcess(WorkflowStartDTO dto);
}
