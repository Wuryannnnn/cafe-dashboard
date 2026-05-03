package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.Date;

/**
 * 结账方式 - PRD 7.1
 */
@Entity
@Data
@DynamicUpdate
public class PaymentMethod {

    @Id
    @GeneratedValue
    private Integer methodId;

    /** 名称, 如"微信"/"支付宝"/"现金"/"会员余额". */
    private String methodName;

    /** 标识码, 如 wechat/alipay/cash/balance. */
    private String methodCode;

    /** 是否启用. */
    private Boolean enabled = true;

    /** 是否默认. */
    private Boolean isDefault = false;

    /** 排序, 越小越靠前. */
    private Integer sortOrder = 0;

    private Date createTime;

    private Date updateTime;
}
