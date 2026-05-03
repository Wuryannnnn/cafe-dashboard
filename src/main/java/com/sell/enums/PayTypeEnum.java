package com.sell.enums;

import lombok.Getter;

@Getter
public enum PayTypeEnum implements CodeEnum {

    WECHAT(0, "微信支付"),
    ALIPAY(1, "支付宝"),
    ;

    private Integer code;

    private String message;

    PayTypeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
