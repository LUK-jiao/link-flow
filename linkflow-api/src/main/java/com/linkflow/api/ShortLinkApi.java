package com.linkflow.api;

import com.linkflow.api.dto.shortlink.ShortLinkCreateDTO;
import com.linkflow.api.dto.shortlink.ShortLinkResultDTO;
import com.linkflow.api.dto.common.Result;

/**
 * ShortLink 服务接口
 */
public interface ShortLinkApi {

    /**
     * 创建短链
     */
    Result<ShortLinkResultDTO> createShortLink(ShortLinkCreateDTO dto);

    /**
     * 根据短链码查询长链接
     */
    Result<String> getUrlByCode(String shortCode);

    /**
     * 检查短链是否存在
     */
    Result<Boolean> exists(String shortCode);
}