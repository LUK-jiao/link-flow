package com.linkflow.user.mapper;

import com.linkflow.user.model.ApproverConfig;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ApproverConfigMapper {
    int deleteByPrimaryKey(Long id);

    int insert(ApproverConfig row);

    ApproverConfig selectByPrimaryKey(Long id);

    List<ApproverConfig> selectAll();

    int updateByPrimaryKey(ApproverConfig row);
}