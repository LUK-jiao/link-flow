package com.linkflow.campaign.mapper;

import com.linkflow.campaign.model.Campaign;
import java.util.List;

public interface CampaignMapper {
    int deleteByPrimaryKey(Long id);

    int insert(Campaign row);

    Campaign selectByPrimaryKey(Long id);

    List<Campaign> selectAll();

    int updateByPrimaryKey(Campaign row);
}