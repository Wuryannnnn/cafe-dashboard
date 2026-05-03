package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.util.Date;

/** 充值方案 - PRD 8.5 */
@Entity
@Data
@DynamicUpdate
public class RechargePlan {

    @Id
    @GeneratedValue
    private Integer planId;

    private String planName;

    /** 实付金额. */
    private BigDecimal payAmount;

    /** 赠送金额. */
    private BigDecimal giveAmount;

    /** 是否启用. */
    private Boolean enabled = true;

    private Integer sortOrder = 0;

    private Date createTime;

    private Date updateTime;
}
