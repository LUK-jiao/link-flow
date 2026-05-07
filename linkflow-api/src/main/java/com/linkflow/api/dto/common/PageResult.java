package com.linkflow.api.dto.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应结果
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> records;

    private Long total;

    private Long pageNum;

    private Long pageSize;

    private Long pages;
}
