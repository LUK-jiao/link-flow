package com.linkflow.workflow.mapper;

import com.linkflow.workflow.model.ApprovalRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ApprovalRecordMapper {
    int insert(ApprovalRecord record);

    ApprovalRecord selectByPrimaryKey(Long id);

    int updateByPrimaryKey(ApprovalRecord record);

    int deleteByPrimaryKey(Long id);

    List<ApprovalRecord> selectAll();
}