package com.linkflow.api.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 活动类型。
 */
@Getter
public enum CampaignTypeEnum {

    MARKETING("MARKETING", "营销活动"),
    PROMOTION("PROMOTION", "促销活动"),
    EVENT("EVENT", "事件活动");

    private final String code;

    private final String description;

    CampaignTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static CampaignTypeEnum ofCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        String normalized = code.trim();
        return Arrays.stream(values())
                .filter(type -> type.code.equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);
    }

    public static boolean isValid(String code) {
        return ofCode(code) != null;
    }

    public static List<String> codes() {
        return Arrays.stream(values())
                .map(CampaignTypeEnum::getCode)
                .collect(Collectors.toList());
    }

    public static String validCodesText() {
        return String.join("、", codes());
    }
}
