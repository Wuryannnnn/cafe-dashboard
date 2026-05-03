package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.util.Date;

/** 报损记录 - PRD 11.2 */
@Entity
@Data
@DynamicUpdate
public class DamageRecord {

    @Id
    @GeneratedValue
    private Long recordId;

    /** 报损原因 (过期/损坏/制作失误等). */
    private String reason;

    /** 物品名. */
    private String itemName;

    private Integer quantity;

    /** 价值金额. */
    private BigDecimal amount;

    private Date occurDate;

    private String remark;

    private String operator;

    private Date createTime;
}
