package com.linkflow.api.dto.agent;

import com.linkflow.api.dto.workflow.ApprovalRecordDTO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AgentChatDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sessionId;

    private String messageId;

    private String assistantMessageId;

    private String type;

    private String text;

    private String actionId;

    private String actionType;

    private String intent;

    private Object extractedSlots;

    private Object missingSlots;

    private AgentApprovalProgressDTO approvalProgress;

    private List<AgentApprovalTaskDTO> approvalTasks;

    private List<ApprovalRecordDTO> approvalRecords;

    private Object campaignCandidates;
}
