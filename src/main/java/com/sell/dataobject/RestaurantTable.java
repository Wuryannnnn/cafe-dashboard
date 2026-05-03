package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Date;

/**
 * 桌台 - PRD 5.2
 */
@Entity
@Table(name = "restaurant_table",
        uniqueConstraints = @UniqueConstraint(name = "uk_table_code", columnNames = "table_code"))
@Data
@DynamicUpdate
public class RestaurantTable {

    @Id
    @GeneratedValue
    private Integer tableId;

    /** 所属区域id. */
    private Integer areaId;

    /** 桌号(对外展示与扫码识别), 如 A1. */
    private String tableCode;

    /** 座位数, 默认 4. */
    private Integer seatCount = 4;

    /** 显示排序, 越小越靠前. */
    private Integer sortOrder = 0;

    /** 是否启用. */
    private Boolean enabled = true;

    private Date createTime;

    private Date updateTime;
}
