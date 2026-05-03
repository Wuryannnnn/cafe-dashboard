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

/**
 * 订单支付记录 - 支持组合支付 (PRD 7.3)
 * 一笔订单可拆分多笔支付 (例如: 部分会员卡 + 部分现金)
 */
@Entity
@Table(name = "order_payment_record",
        indexes = @Index(name = "idx_opr_order_id", columnList = "orderId"))
@Data
@DynamicUpdate
public class OrderPaymentRecord {

    @Id
    @GeneratedValue
    private Long paymentId;

    private String orderId;

    /** 结账方式id (PaymentMethod). */
    private Integer methodId;

    /** 结账方式名 (冗余, 报表友好). */
    private String methodName;

    /** 本笔金额. */
    private BigDecimal amount;

    /** 操作员账号. */
    private String operator;

    private Date createTime;
}
