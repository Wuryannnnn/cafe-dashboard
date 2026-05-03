package com.sell.controller;

import com.sell.dataobject.PaymentMethod;
import com.sell.exception.SellException;
import com.sell.form.PaymentMethodForm;
import com.sell.service.PaymentMethodService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import jakarta.validation.Valid;
import java.util.Map;

/** 结账方式 - PRD 7.1 */
@Controller
@RequestMapping("/seller/payment")
public class SellerPaymentMethodController {

    @Autowired
    private PaymentMethodService service;

    @GetMapping("/list")
    public ModelAndView list(Map<String, Object> map) {
        map.put("paymentList", service.findAll());
        return new ModelAndView("payment/list", map);
    }

    @GetMapping("/index")
    public ModelAndView index(@RequestParam(value = "methodId", required = false) Integer methodId,
                              Map<String, Object> map) {
        if (methodId != null) map.put("payment", service.findOne(methodId));
        return new ModelAndView("payment/index", map);
    }

    @PostMapping("/save")
    public ModelAndView save(@Valid PaymentMethodForm form,
                             BindingResult bindingResult, Map<String, Object> map) {
        if (bindingResult.hasErrors()) {
            map.put("msg", bindingResult.getFieldError().getDefaultMessage());
            map.put("url", "/sell/seller/payment/index");
            return new ModelAndView("common/error", map);
        }
        try {
            PaymentMethod m = form.getMethodId() != null
                    ? service.findOne(form.getMethodId()) : new PaymentMethod();
            if (m == null) m = new PaymentMethod();
            BeanUtils.copyProperties(form, m);
            service.save(m);
        } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/payment/list");
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/payment/list");
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/delete")
    public ModelAndView delete(@RequestParam("methodId") Integer methodId,
                               Map<String, Object> map) {
        try {
            service.delete(methodId);
        } catch (Exception e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/payment/list");
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/payment/list");
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/setDefault")
    public ModelAndView setDefault(@RequestParam("methodId") Integer methodId,
                                   Map<String, Object> map) {
        try {
            service.setDefault(methodId);
        } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/payment/list");
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/payment/list");
        return new ModelAndView("common/success", map);
    }
}
