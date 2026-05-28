package com.linkflow.api;

import com.linkflow.api.dto.agent.AgentChatCommand;
import com.linkflow.api.dto.agent.AgentChatDTO;
import com.linkflow.api.dto.agent.AgentActionConfirmCommand;
import com.linkflow.api.dto.agent.AgentActionConfirmDTO;
import com.linkflow.api.dto.common.Result;

public interface AgentApi {

    Result<AgentChatDTO> chat(AgentChatCommand command);

    Result<AgentActionConfirmDTO> confirmAction(AgentActionConfirmCommand command);
}
