package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品规格（大杯/中杯/小杯、冰/热等）
 */
@Entity
@Data
@DynamicUpdate
public class ProductSku {

    @Id
    private String skuId;

    /** 所属商品id. */
    private String productId;

    /** 规格名称, 如"大杯/冰". */
    private String skuName;

    /** 该规格的价格. */
    private BigDecimal skuPrice;

    /** 库存. */
    private Integer skuStock;

    private Date createTime;

    private Date updateTime;
}
