package com.sell.repository;

import com.sell.dataobject.MemberCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {
    List<MemberCoupon> findByMemberIdOrderByMemberCouponIdDesc(Integer memberId);
    long countByCouponId(Integer couponId);

    /** 某券当前被"持有且未使用"(status=0)的会员id; 定向发放时据此去重, 避免对同一会员重复发放未用券. */
    @Query("SELECT mc.memberId FROM MemberCoupon mc WHERE mc.couponId = :couponId AND mc.status = 0")
    List<Integer> findHoldingMemberIds(@Param("couponId") Integer couponId);
}
