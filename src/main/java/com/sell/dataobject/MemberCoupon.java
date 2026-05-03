package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.Date;

/** 会员持有的卡券 - PRD 8.6 */
@Entity
@Table(name = "member_coupon", indexes = {
        @Index(name = "idx_mc_member", columnList = "memberId"),
        @Index(name = "idx_mc_coupon", columnList = "couponId")
})
@Data
@DynamicUpdate
public class MemberCoupon {

    @Id
    @GeneratedValue
    private Long memberCouponId;

    private Integer memberId;

    private Integer couponId;

    /** 状态: 0=未使用 1=已使用 2=已过期. */
    private Integer status = 0;

    private Date obtainedTime;

    private Date usedTime;

    private String orderId;
}
