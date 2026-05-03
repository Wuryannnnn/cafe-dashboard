package com.sell.dataobject;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * WMS 出库同步补偿队列.
 * 当 WMS 侧不可用或返回非业务错误时, 出库请求入队, 由 WmsRetryService 按指数退避重试.
 * 库存不足 (409) 不入队 —— 那属于业务失败, 订单已经回滚.
 */
@Entity
@Table(name = "pending_wms_shipment")
@Data
public class PendingWmsShipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 业务订单号, 用于 WMS 幂等. */
    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    /** 序列化后的 details JSON (itemId/skuId/quantity). */
    @Lob
    @Column(name = "details_json", nullable = false)
    private String detailsJson;

    /** pending / done / failed (连续失败 N 次后进入). */
    @Column(nullable = false, length = 16)
    private String status = "pending";

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "next_retry_at")
    private Date nextRetryAt;

    @Column(name = "create_time", nullable = false)
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;
}
