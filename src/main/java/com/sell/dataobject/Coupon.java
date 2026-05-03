package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 优惠券 - PRD 7.2 / 8.6
 */
@Entity
@Data
@DynamicUpdate
public class Coupon {

    @Id
    @GeneratedValue
    private Integer couponId;

    /** 券名称. */
    private String couponName;

    /** 券类型: 0满减 1折扣 2免费 3兑换. */
    private Integer couponType;

    /** 面额 (满减:减免金额; 折扣:折扣率0~100; 免费:无意义可填0; 兑换:兑换商品价值). */
    private BigDecimal faceValue;

    /** 满 X 元可用. */
    private BigDecimal minSpend;

    /** 有效期开始. */
    private Date validFrom;

    /** 有效期截止. */
    private Date validTo;

    /** 是否启用. */
    private Boolean enabled = true;

    /** 总发行量, null 表示不限. */
    private Integer totalQuantity;

    /** 已发放量. */
    private Integer issuedQuantity = 0;

    /** 已核销量. */
    private Integer redeemedQuantity = 0;

    private Date createTime;

    private Date updateTime;
}
