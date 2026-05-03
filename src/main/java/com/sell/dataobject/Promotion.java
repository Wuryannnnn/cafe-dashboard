package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.Date;

/** 营销活动 - PRD 8.7 */
@Entity
@Data
@DynamicUpdate
public class Promotion {

    @Id
    @GeneratedValue
    private Integer promotionId;

    private String promotionName;

    /** 类型: 0特价 1第二杯半价 2集点 3开卡有礼. */
    private Integer promotionType;

    /** 配置 (JSON, 不同类型不同字段). */
    @Column(length = 4096)
    private String configJson;

    private Date startTime;

    private Date endTime;

    private Boolean enabled = true;

    private Integer sortOrder = 0;

    private Date createTime;

    private Date updateTime;
}
