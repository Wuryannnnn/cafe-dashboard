package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.Date;

/** 积分记录 - PRD 8.3 详情 */
@Entity
@Table(name = "points_record", indexes = @Index(name = "idx_pr_member", columnList = "memberId"))
@Data
@DynamicUpdate
public class PointsRecord {

    @Id
    @GeneratedValue
    private Long recordId;

    private Integer memberId;

    /** 类型: 1=消费获得 2=兑换扣减 3=活动赠送 4=调整. */
    private Integer recordType;

    private Integer points;

    private Integer pointsAfter;

    private String orderId;

    private String remark;

    private Date createTime;
}
