package com.sell.service.impl;

import com.sell.dataobject.Coupon;
import com.sell.exception.SellException;
import com.sell.repository.CouponRepository;
import com.sell.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class CouponServiceImpl implements CouponService {

    @Autowired
    private CouponRepository repository;

    @Override
    public List<Coupon> findAll() { return repository.findAllByOrderByCouponIdDesc(); }

    @Override
    public List<Coupon> findEnabled() { return repository.findByEnabledTrue(); }

    @Override
    public Coupon findOne(Integer couponId) {
        if (couponId == null) return null;
        return repository.findById(couponId).orElse(null);
    }

    @Override
    public Coupon save(Coupon coupon) {
        if (coupon.getCouponName() == null || coupon.getCouponName().trim().isEmpty()) {
            throw new SellException(1, "券名称不能为空");
        }
        if (coupon.getCouponType() == null) {
            throw new SellException(1, "请选择券类型");
        }
        if (coupon.getFaceValue() == null) coupon.setFaceValue(BigDecimal.ZERO);
        if (coupon.getMinSpend() == null) coupon.setMinSpend(BigDecimal.ZERO);
        if (coupon.getEnabled() == null) coupon.setEnabled(true);
        if (coupon.getIssuedQuantity() == null) coupon.setIssuedQuantity(0);
        if (coupon.getRedeemedQuantity() == null) coupon.setRedeemedQuantity(0);
        Date now = new Date();
        if (coupon.getCreateTime() == null) coupon.setCreateTime(now);
        coupon.setUpdateTime(now);
        return repository.save(coupon);
    }

    @Override
    public void delete(Integer couponId) {
        repository.deleteById(couponId);
    }

    @Override
    public void toggleEnabled(Integer couponId) {
        Coupon c = findOne(couponId);
        if (c == null) throw new SellException(1, "券不存在");
        c.setEnabled(!Boolean.TRUE.equals(c.getEnabled()));
        c.setUpdateTime(new Date());
        repository.save(c);
    }
}
