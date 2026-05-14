package com.linkflow.gateway.controller;

import com.linkflow.api.ShortLinkApi;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.shortlink.ShortLinkCreateDTO;
import com.linkflow.api.dto.shortlink.ShortLinkResultDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ShortLink", description = "短链接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/short-links")
public class ShortLinkGatewayController {

    private final ShortLinkApi shortLinkApi;

    @Operation(summary = "创建短链")
    @PostMapping
    public Result<ShortLinkResultDTO> createShortLink(@RequestBody ShortLinkCreateDTO dto) {
        return shortLinkApi.createShortLink(dto);
    }

    @Operation(summary = "根据短链码查询长链接")
    @GetMapping("/{shortCode}/url")
    public Result<String> getUrlByCode(
            @Parameter(description = "短链码", required = true)
            @PathVariable("shortCode") String shortCode) {
        return shortLinkApi.getUrlByCode(shortCode);
    }

    @Operation(summary = "检查短链码是否存在")
    @GetMapping("/{shortCode}/exists")
    public Result<Boolean> exists(
            @Parameter(description = "短链码", required = true)
            @PathVariable("shortCode") String shortCode) {
        return shortLinkApi.exists(shortCode);
    }
}
