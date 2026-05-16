package com.linkflow.campaign.service.impl;

import com.alibaba.fastjson2.JSON;
import com.linkflow.api.ShortLinkApi;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.shortlink.ShortLinkCreateDTO;
import com.linkflow.api.dto.shortlink.ShortLinkResultDTO;
import com.linkflow.campaign.service.ShortLinkGatewayService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "linkflow.campaign.shortlink.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class ShortLinkGatewayServiceImpl implements ShortLinkGatewayService {

    @DubboReference(check = false)
    private ShortLinkApi shortLinkApi;

    @Override
    public Result<ShortLinkResultDTO> createShortLink(String longUrl) {
        ShortLinkCreateDTO dto = new ShortLinkCreateDTO();
        dto.setLongUrl(longUrl);

        log.info("调用 ShortLinkApi.createShortLink, dto={}", JSON.toJSONString(dto));
        Result<ShortLinkResultDTO> result = shortLinkApi.createShortLink(dto);
        log.info("ShortLinkApi.createShortLink 返回, success={}, message={}, shortCode={}",
                result != null && result.isSuccess(),
                result != null ? result.getMessage() : "null",
                result != null && result.getData() != null ? result.getData().getShortCode() : null);
        return result;
    }
}
