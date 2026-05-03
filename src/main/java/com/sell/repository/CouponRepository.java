package com.sell.repository;

import com.sell.dataobject.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CouponRepository extends JpaRepository<Coupon, Integer> {

    List<Coupon> findAllByOrderByCouponIdDesc();

    List<Coupon> findByEnabledTrue();
}
