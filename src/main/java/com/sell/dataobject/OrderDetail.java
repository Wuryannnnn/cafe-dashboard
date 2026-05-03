package com.sell.dataobject;

import lombok.Data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;

/**
 * 2017-06-11 17:20
 */
@Entity
@Data
public class OrderDetail {

    @Id
    private String detailId;

    /** 订单id. */
    private String orderId;

    /** 商品id. */
    private String productId;

    /** 商品名称. */
    private String productName;

    /** 商品单价. */
    private BigDecimal productPrice;

    /** 商品数量. */
    private Integer productQuantity;

    /** 商品小图. */
    private String productIcon;

    /** 规格id. */
    private String skuId;

    /** 规格名称(冗余, 如"大杯/冰"). */
    private String skuName;

    /** 加料信息(JSON, 如[{"name":"加浓","price":3}]). */
    private String addons;

    /** 加料总费用. */
    private BigDecimal addonFee;
}
