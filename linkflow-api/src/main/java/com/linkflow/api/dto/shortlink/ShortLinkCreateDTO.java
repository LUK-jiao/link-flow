package com.linkflow.api.dto.shortlink;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建短链 DTO
 */
@Data
public class ShortLinkCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 长链接
     */
    private String longUrl;

    /**
     * 过期时间（可选）
     */
    private String expireTime;
}