package com.sell.enums;

import lombok.Getter;

@Getter
public enum DiningTypeEnum implements CodeEnum {

    DINE_IN(0, "堂食"),
    TAKEAWAY(1, "外带"),
    ;

    private Integer code;

    private String message;

    DiningTypeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
