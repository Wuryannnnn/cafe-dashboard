package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.Date;

/** 时段菜单 - PRD 3.4 */
@Entity
@Data
@DynamicUpdate
public class TimeSlot {

    @Id
    @GeneratedValue
    private Integer slotId;

    private String slotName;

    /** HH:mm 格式. */
    private String startTime;

    /** HH:mm. */
    private String endTime;

    /** 适用菜品 productId 列表 (逗号分隔, 简化模型). */
    private String productIds;

    private Boolean enabled = true;

    private Integer sortOrder = 0;

    private Date createTime;

    private Date updateTime;
}
