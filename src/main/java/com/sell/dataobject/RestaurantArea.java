package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.Date;

/**
 * 桌台区域 - PRD 5.1
 */
@Entity
@Data
@DynamicUpdate
public class RestaurantArea {

    @Id
    @GeneratedValue
    private Integer areaId;

    /** 区域名 (如: 吧台区/大厅A区/户外区/VIP区). */
    private String areaName;

    /** 显示排序, 越小越靠前. */
    private Integer sortOrder = 0;

    private Date createTime;

    private Date updateTime;
}
