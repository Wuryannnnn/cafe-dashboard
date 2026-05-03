package com.sell.form;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

/**
 * 2017-06-18 23:31
 */
@Data
public class OrderForm {

    /**
     * 买家姓名
     */
    @NotEmpty(message = "姓名必填")
    private String name;

    /**
     * 买家手机号
     */
    @NotEmpty(message = "手机号必填")
    private String phone;

    /**
     * 买家地址(外带可为空)
     */
    private String address;

    /**
     * 买家微信openid
     */
    @NotEmpty(message = "openid必填")
    private String openid;

    /**
     * 购物车
     */
    @NotEmpty(message = "购物车不能为空")
    private String items;

    /**
     * 就餐方式: 0堂食, 1外带
     */
    private Integer diningType;

    /**
     * 桌号(堂食时使用)
     */
    private String tableNumber;

    /**
     * 桌台id (堂食扫码点餐时优先使用, 用于绑定桌台)
     */
    private Integer tableId;

    /**
     * 支付方式: 0微信支付, 1支付宝
     */
    private Integer payType;

    /**
     * 订单备注(少冰/不要香菜等)
     */
    private String remark;
}
