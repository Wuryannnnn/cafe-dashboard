package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;
import java.util.Date;

/** 小程序订单发货上报重试队列 (独立于 WMS 出库补偿队列 pending_wms_shipment). */
@Entity
@Table(name = "pending_shipping", indexes = { @Index(name = "idx_ps_status", columnList = "status") })
@Data
@DynamicUpdate
public class PendingShipping {

    @Id
    @GeneratedValue
    private Long id;

    private String orderId;

    private String transactionId;

    private String openid;

    /** pending / done / failed. */
    private String status;

    private Integer attempts;

    private Date nextRetryAt;

    private String lastError;

    private Date createTime;

    private Date updateTime;
}
