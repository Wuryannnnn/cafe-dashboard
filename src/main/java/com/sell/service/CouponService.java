package com.sell.service;

import com.sell.dataobject.Coupon;

import java.util.List;

public interface CouponService {
    List<Coupon> findAll();
    List<Coupon> findEnabled();
    Coupon findOne(Integer couponId);
    Coupon save(Coupon coupon);
    void delete(Integer couponId);
    void toggleEnabled(Integer couponId);
}
