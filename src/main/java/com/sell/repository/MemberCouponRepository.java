package com.sell.repository;

import com.sell.dataobject.MemberCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {
    List<MemberCoupon> findByMemberIdOrderByMemberCouponIdDesc(Integer memberId);
    long countByCouponId(Integer couponId);
}
