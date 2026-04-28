package com.linkflow.api.dto.shortlink;

import lombok.Data;

import java.io.Serializable;

/**
 * 短链结果 DTO
 */
@Data
public class ShortLinkResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 短链码
     */
    private String shortCode;

    /**
     * 长链接
     */
    private String longUrl;

    /**
     * 创建成功
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;
}