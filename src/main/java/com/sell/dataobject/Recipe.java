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
 * 菜品配方 (BOM) — 一个菜品消耗的 WMS 物料清单
 * 用于卖出菜品时自动从 WMS 出库扣减原料
 */
@Entity
@Table(name = "product_recipe", indexes = {
        @Index(name = "idx_recipe_product", columnList = "productId"),
        @Index(name = "idx_recipe_sku", columnList = "skuId")
})
@Data
@DynamicUpdate
public class Recipe {

    @Id
    @GeneratedValue
    private Long recipeId;

    /** 菜品 id (sell.product_info.product_id). */
    private String productId;

    /** 规格 id (sell.product_sku.sku_id), 可空. 空表示该菜品所有规格通用. */
    private String skuId;

    /** WMS 物料 id (wms_item.id). */
    private Long wmsItemId;

    /** WMS 物料名 (冗余, 展示用). */
    private String wmsItemName;

    /** WMS 规格 id (wms_item_sku.id), 可空. */
    private Long wmsSkuId;

    /** 单杯/单份消耗数量 (例如 18g 咖啡豆). */
    private BigDecimal quantity;

    /** 单位 (展示用, 如 g/ml/个). */
    private String unit;

    private Date createTime;

    private Date updateTime;
}
