package com.linkflow.campaign.service;

import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.shortlink.ShortLinkResultDTO;

public interface ShortLinkGatewayService {

    Result<ShortLinkResultDTO> createShortLink(String longUrl);
}
