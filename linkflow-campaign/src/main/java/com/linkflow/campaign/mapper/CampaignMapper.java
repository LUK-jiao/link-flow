package com.linkflow.campaign.mapper;

import com.linkflow.campaign.model.Campaign;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CampaignMapper {
    int deleteByPrimaryKey(Long id);

    int insert(Campaign row);

    Campaign selectByPrimaryKey(Long id);

    List<Campaign> selectAll();

    int updateByPrimaryKey(Campaign row);
}