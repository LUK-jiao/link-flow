package com.linkFlow.user.mapper;

import com.linkFlow.user.model.ApproverConfig;
import java.util.List;

public interface ApproverConfigMapper {
    int deleteByPrimaryKey(Long id);

    int insert(ApproverConfig row);

    ApproverConfig selectByPrimaryKey(Long id);

    List<ApproverConfig> selectAll();

    int updateByPrimaryKey(ApproverConfig row);
}