package com.sell.enums;

import lombok.Getter;

/** 券类型 - PRD 7.2 / 8.6 */
@Getter
public enum CouponTypeEnum implements CodeEnum {

    FULL_REDUCE(0, "满减券"),
    DISCOUNT(1, "折扣券"),
    FREE(2, "免费券"),
    EXCHANGE(3, "兑换券"),
    ;

    private Integer code;
    private String message;

    CouponTypeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
