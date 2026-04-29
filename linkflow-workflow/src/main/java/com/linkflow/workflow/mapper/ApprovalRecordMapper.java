package com.linkflow.workflow.mapper;

import com.linkflow.workflow.model.ApprovalRecord;
import java.util.List;

public interface ApprovalRecordMapper {
    int deleteByPrimaryKey(Long id);

    int insert(ApprovalRecord row);

    ApprovalRecord selectByPrimaryKey(Long id);

    List<ApprovalRecord> selectAll();

    int updateByPrimaryKey(ApprovalRecord row);
}