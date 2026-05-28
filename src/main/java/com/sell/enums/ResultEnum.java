package com.sell.enums;

import lombok.Getter;

/**
 * 2017-06-11 18:56
 */
@Getter
public enum ResultEnum {

    SUCCESS(0, "成功"),

    PARAM_ERROR(1, "参数不正确"),

    PRODUCT_NOT_EXIST(10, "商品不存在"),

    PRODUCT_STOCK_ERROR(11, "商品库存不正确"),

    ORDER_NOT_EXIST(12, "订单不存在"),

    ORDERDETAIL_NOT_EXIST(13, "订单详情不存在"),

    ORDER_STATUS_ERROR(14, "订单状态不正确"),

    ORDER_UPDATE_FAIL(15, "订单更新失败"),

    ORDER_DETAIL_EMPTY(16, "订单详情为空"),

    ORDER_PAY_STATUS_ERROR(17, "订单支付状态不正确"),

    CART_EMPTY(18, "购物车为空"),

    ORDER_OWNER_ERROR(19, "该订单不属于当前用户"),

    WECHAT_MP_ERROR(20, "微信公众账号方面错误"),

    WXPAY_NOTIFY_MONEY_VERIFY_ERROR(21, "微信支付异步通知金额校验不通过"),

    ORDER_CANCEL_SUCCESS(22, "订单取消成功"),

    ORDER_FINISH_SUCCESS(23, "订单完结成功"),

    PRODUCT_STATUS_ERROR(24, "商品状态不正确"),

    LOGIN_FAIL(25, "登录失败, 登录信息不正确"),

    LOGOUT_SUCCESS(26, "登出成功"),

    ORDER_MAKING_SUCCESS(27, "订单开始制作"),

    ORDER_READY_SUCCESS(28, "订单制作完成, 待取餐"),

    PRODUCT_SKU_NOT_EXIST(29, "商品规格不存在"),

    PRODUCT_SKU_STOCK_ERROR(30, "商品规格库存不足"),

    PAY_TYPE_ERROR(31, "支付方式不正确"),

    ALIPAY_NOTIFY_VERIFY_ERROR(32, "支付宝异步通知验证失败"),

    ORDER_REFUND_SUCCESS(33, "订单退款成功"),
    ORDER_NOT_PAID(34, "订单未支付, 不能退款"),
    DISCOUNT_RATE_ERROR(35, "折扣率必须在 0~100 之间"),
    NEW_AMOUNT_ERROR(36, "改价金额必须 >= 0"),
    PAYMENT_METHOD_NOT_EXIST(37, "结账方式不存在"),
    COUPON_NOT_EXIST(38, "优惠券不存在"),
    ORDER_REFUND_FAIL(39, "退款失败, 请稍后重试"),
    ;

    private Integer code;

    private String message;

    ResultEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
