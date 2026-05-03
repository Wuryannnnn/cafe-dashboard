package com.sell.enums;

import lombok.Getter;

/**
 * 打印工位
 */
@Getter
public enum PrintStationEnum implements CodeEnum {

    BAR(0, "吧台"),
    KITCHEN(1, "后厨"),
    ;

    private Integer code;

    private String message;

    PrintStationEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
