package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;

/** 储值记录 - PRD 8.5 */
@Entity
@Table(name = "balance_record", indexes = @Index(name = "idx_br_member", columnList = "memberId"))
@Data
@DynamicUpdate
public class BalanceRecord {

    @Id
    @GeneratedValue
    private Long recordId;

    private Integer memberId;

    /** 类型: 1=充值 2=消费 3=赠送 4=调整. */
    private Integer recordType;

    /** 变动金额 (充值/赠送/调整为正, 消费为负). */
    private BigDecimal amount;

    /** 变动后余额. */
    private BigDecimal balanceAfter;

    /** 关联订单 (消费时). */
    private String orderId;

    private String remark;

    private String operator;

    private Date createTime;
}
