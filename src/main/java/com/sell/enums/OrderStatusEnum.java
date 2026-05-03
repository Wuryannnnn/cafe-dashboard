package com.sell.enums;

import lombok.Getter;

/**
 * 2017-06-11 17:12
 */
@Getter
public enum OrderStatusEnum implements CodeEnum {
    NEW(0, "新订单"),
    MAKING(1, "制作中"),
    READY(2, "待取餐"),
    FINISHED(3, "完结"),
    CANCEL(4, "已取消"),
    REFUNDED(5, "已退款"),
    ;

    private Integer code;

    private String message;

    OrderStatusEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
