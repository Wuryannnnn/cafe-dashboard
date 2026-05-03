package com.sell.service;

import com.sell.dataobject.BalanceRecord;
import com.sell.dataobject.Member;
import com.sell.dataobject.MemberCoupon;
import com.sell.dataobject.PointsRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 会员 - PRD 8
 */
public interface MemberService {

    Member findOne(Integer memberId);
    Member findByPhone(String phone);
    Member save(Member member);
    Page<Member> search(String keyword, Pageable pageable);
    void delete(Integer memberId);

    /** 充值 (含赠送). */
    BalanceRecord recharge(Integer memberId, BigDecimal payAmount, BigDecimal giveAmount, String operator, String remark);

    /** 余额调整. */
    BalanceRecord adjustBalance(Integer memberId, BigDecimal delta, String operator, String remark);

    /** 余额消费. */
    BalanceRecord consume(Integer memberId, BigDecimal amount, String orderId, String remark);

    /** 积分变动. */
    PointsRecord adjustPoints(Integer memberId, Integer delta, Integer recordType, String orderId, String remark);

    List<BalanceRecord> balanceHistory(Integer memberId);
    List<PointsRecord> pointsHistory(Integer memberId);
    List<MemberCoupon> couponHistory(Integer memberId);

    /** 总览数据 (营销中心 dashboard). */
    Map<String, Object> overview();

    /** 升级规则: 触发条件检查. */
    void evaluateLevel(Member member);

    /** 给会员发放优惠券. */
    MemberCoupon issueCoupon(Integer memberId, Integer couponId);
}
