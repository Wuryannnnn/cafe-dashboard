package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.util.Date;

/** 会员等级 - PRD 8.4 */
@Entity
@Data
@DynamicUpdate
public class MemberLevel {

    @Id
    @GeneratedValue
    private Integer levelId;

    /** 等级名 (普通会员/银卡/金卡/黑金). */
    private String levelName;

    /** 升级所需累计消费金额. */
    private BigDecimal upgradeAmount;

    /** 或升级所需累计消费次数. */
    private Integer upgradeCount;

    /** 折扣率 (0~100, 100=不打折, 90=9折, 85=85折). */
    private Integer discountRate;

    /** 排序 (一般等级越高排序越大). */
    private Integer sortOrder = 0;

    private Date createTime;

    private Date updateTime;
}
