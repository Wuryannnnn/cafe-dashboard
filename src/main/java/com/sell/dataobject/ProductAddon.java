package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品加料选项（加浓、换燕麦奶、加糖浆等）
 */
@Entity
@Data
@DynamicUpdate
public class ProductAddon {

    @Id
    private String addonId;

    /** 加料名称. */
    private String addonName;

    /** 加料价格. */
    private BigDecimal addonPrice;

    /** 适用的类目编号, 为null表示适用所有类目. */
    private Integer categoryType;

    /** 所属属性组id (PRD 3.3, 可空). */
    private Integer groupId;

    /** 排序. */
    private Integer sortOrder = 0;

    private Date createTime;

    private Date updateTime;
}
