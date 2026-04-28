package com.linkFlow.workflow.mapper;

import com.linkFlow.workflow.model.ApprovalRecord;
import java.util.List;

public interface ApprovalRecordMapper {
    int deleteByPrimaryKey(Long id);

    int insert(ApprovalRecord row);

    ApprovalRecord selectByPrimaryKey(Long id);

    List<ApprovalRecord> selectAll();

    int updateByPrimaryKey(ApprovalRecord row);
}