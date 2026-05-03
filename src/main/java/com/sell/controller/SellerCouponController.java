package com.sell.controller;

import com.sell.dataobject.Coupon;
import com.sell.enums.CouponTypeEnum;
import com.sell.exception.SellException;
import com.sell.form.CouponForm;
import com.sell.service.CouponService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import jakarta.validation.Valid;
import java.util.Map;

/** 券类管理 - PRD 7.2 / 8.6 */
@Controller
@RequestMapping("/seller/coupon")
public class SellerCouponController {

    @Autowired
    private CouponService service;

    @GetMapping("/list")
    public ModelAndView list(Map<String, Object> map) {
        map.put("couponList", service.findAll());
        map.put("couponTypes", CouponTypeEnum.values());
        return new ModelAndView("coupon/list", map);
    }

    @GetMapping("/index")
    public ModelAndView index(@RequestParam(value = "couponId", required = false) Integer couponId,
                              Map<String, Object> map) {
        if (couponId != null) map.put("coupon", service.findOne(couponId));
        map.put("couponTypes", CouponTypeEnum.values());
        return new ModelAndView("coupon/index", map);
    }

    @PostMapping("/save")
    public ModelAndView save(@Valid CouponForm form,
                             BindingResult bindingResult,
                             Map<String, Object> map) {
        if (bindingResult.hasErrors()) {
            map.put("msg", bindingResult.getFieldError().getDefaultMessage());
            map.put("url", "/sell/seller/coupon/index");
            return new ModelAndView("common/error", map);
        }
        try {
            Coupon c = form.getCouponId() != null ? service.findOne(form.getCouponId()) : new Coupon();
            if (c == null) c = new Coupon();
            BeanUtils.copyProperties(form, c);
            service.save(c);
        } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/coupon/list");
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/coupon/list");
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/delete")
    public ModelAndView delete(@RequestParam("couponId") Integer couponId, Map<String, Object> map) {
        try { service.delete(couponId); } catch (Exception e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/coupon/list");
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/coupon/list");
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/toggle")
    public ModelAndView toggle(@RequestParam("couponId") Integer couponId, Map<String, Object> map) {
        try { service.toggleEnabled(couponId); } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/coupon/list");
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/coupon/list");
        return new ModelAndView("common/success", map);
    }
}
