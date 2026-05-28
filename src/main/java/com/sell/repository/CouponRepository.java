package com.sell.repository;

import com.sell.dataobject.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface CouponRepository extends JpaRepository<Coupon, Integer> {

    List<Coupon> findAllByOrderByCouponIdDesc();

    List<Coupon> findByEnabledTrue();

    /**
     * 原子发放一张券: 仅当未达发行上限(totalQuantity 为空表示不限)时 issuedQuantity+1.
     * 返回受影响行数 (1=发放成功, 0=已发完).
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Coupon c SET c.issuedQuantity = COALESCE(c.issuedQuantity, 0) + 1, c.updateTime = :now "
            + "WHERE c.couponId = :couponId "
            + "AND (c.totalQuantity IS NULL OR COALESCE(c.issuedQuantity, 0) < c.totalQuantity)")
    int tryIssue(@Param("couponId") Integer couponId, @Param("now") Date now);
}
