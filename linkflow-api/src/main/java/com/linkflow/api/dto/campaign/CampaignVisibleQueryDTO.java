package com.linkflow.api.dto.campaign;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 当前用户可见活动查询 DTO。
 */
@Data
public class CampaignVisibleQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前用户 ID。
     */
    private Long userId;

    /**
     * 当前用户作为审批人的活动类型列表。
     */
    private List<String> approverCampaignTypes;

    /**
     * 活动名称，支持模糊查询。
     */
    private String name;

    /**
     * 活动类型，取值见 com.linkflow.api.enums.CampaignTypeEnum。
     */
    private String campaignType;

    /**
     * 活动状态。
     */
    private String status;

    /**
     * 页码。
     */
    private Integer pageNum = 1;

    /**
     * 每页数量。
     */
    private Integer pageSize = 10;
}
