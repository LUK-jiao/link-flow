package com.linkflow.api;

import com.linkflow.api.dto.agent.AgentChatCommand;
import com.linkflow.api.dto.agent.AgentChatDTO;
import com.linkflow.api.dto.common.Result;

public interface AgentApi {

    Result<AgentChatDTO> chat(AgentChatCommand command);
}
