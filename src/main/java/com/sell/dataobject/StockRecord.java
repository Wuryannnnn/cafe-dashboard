package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.Date;

/** 库存变动记录 */
@Entity
@Table(name = "stock_record",
        indexes = {
                @Index(name = "idx_sr_product", columnList = "productId"),
                @Index(name = "idx_sr_sku", columnList = "skuId")
        })
@Data
@DynamicUpdate
public class StockRecord {

    @Id
    @GeneratedValue
    private Long recordId;

    /** 类型: 1=入库 2=出库 (订单消耗) 3=报损 4=盘点调整. */
    private Integer recordType;

    private String productId;

    private String productName;

    /** 规格id, 可空 (商品级库存调整时为空). */
    private String skuId;

    private String skuName;

    /** 变动数量 (正/负). */
    private Integer delta;

    /** 变动后剩余库存. */
    private Integer stockAfter;

    /** 关联订单号 (消耗时). */
    private String orderId;

    private String remark;

    private String operator;

    private Date createTime;
}
