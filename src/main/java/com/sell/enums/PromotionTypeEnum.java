package com.sell.enums;

import lombok.Getter;

/** 营销活动类型 - PRD 8.7 */
@Getter
public enum PromotionTypeEnum implements CodeEnum {

    DISCOUNT(0, "特价/折扣"),
    SECOND_HALF(1, "第二杯半价"),
    STAMP_CARD(2, "集点 (满N杯送一杯)"),
    NEW_MEMBER_GIFT(3, "开卡有礼"),
    ;

    private Integer code;
    private String message;

    PromotionTypeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
