package com.linkflow.campaign.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linkflow.campaign.model.Campaign;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;
import java.util.List;

@Mapper
public interface CampaignMapper extends BaseMapper<Campaign> {
    int deleteByPrimaryKey(Long id);

    int insert(Campaign row);

    Campaign selectByPrimaryKey(Long id);

    List<Campaign> selectAll();

    int updateByPrimaryKey(Campaign row);

    int updateShortCodeIfEmpty(@Param("campaignId") Long campaignId,
                               @Param("shortCode") String shortCode,
                               @Param("updateTime") Date updateTime);
}
