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

/** 会员 - PRD 8.3 */
@Entity
@Table(name = "member", indexes = {
        @Index(name = "idx_member_phone", columnList = "phone"),
        @Index(name = "idx_member_openid", columnList = "openid")
})
@Data
@DynamicUpdate
public class Member {

    @Id
    @GeneratedValue
    private Integer memberId;

    private String phone;

    private String nickname;

    private String openid;

    /** 等级id, 关联 MemberLevel. */
    private Integer levelId;

    /** 储值余额. */
    private BigDecimal balance = BigDecimal.ZERO;

    /** 积分. */
    private Integer points = 0;

    /** 累计消费金额. */
    private BigDecimal totalSpend = BigDecimal.ZERO;

    /** 累计消费次数. */
    private Integer spendCount = 0;

    /** 标签 (逗号分隔). */
    private String tags;

    /** 备注. */
    private String remark;

    /** 获客渠道. */
    private String channel;

    private Date registerTime;

    private Date lastSpendTime;

    private Date updateTime;
}
