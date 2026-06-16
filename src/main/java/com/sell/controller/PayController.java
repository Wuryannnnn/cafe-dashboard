package com.sell.controller;

import com.sell.dto.OrderDTO;
import com.sell.enums.ResultEnum;
import com.sell.exception.SellException;
import com.sell.service.OrderService;
import com.sell.service.PayService;
import com.lly835.bestpay.model.PayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 公众号 best-pay 微信支付端点 (顾客端逐步迁小程序后趋于停用; 退款分支仍供历史单).
 * 支付宝已下线.
 */
@Controller
@RequestMapping("/pay")
@Slf4j
public class PayController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PayService payService;

    /** 发起微信支付(公众号 JSAPI) - JSON 端点. 失败返回可读 {code,msg} 而非 500. */
    @GetMapping("/create")
    @ResponseBody
    public Map<String, Object> create(@RequestParam("orderId") String orderId,
                                      @RequestParam("returnUrl") String returnUrl) {
        OrderDTO orderDTO = orderService.findOne(orderId);
        if (orderDTO == null) {
            throw new SellException(ResultEnum.ORDER_NOT_EXIST);
        }
        Map<String, Object> r = new HashMap<>();
        try {
            PayResponse payResponse = payService.create(orderDTO);
            r.put("payType", "wechat");
            r.put("appId", payResponse.getAppId());
            r.put("timeStamp", payResponse.getTimeStamp());
            r.put("nonceStr", payResponse.getNonceStr());
            r.put("packAge", payResponse.getPackAge());
            r.put("paySign", payResponse.getPaySign());
            r.put("returnUrl", returnUrl);
            try { r.put("mwebUrl", payResponse.getMwebUrl()); } catch (Throwable ignored) {}
            try { r.put("codeUrl", payResponse.getCodeUrl()); } catch (Throwable ignored) {}
        } catch (Exception e) {
            log.error("【发起支付】失败 orderId={}: {}", orderId, e.getMessage());
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (reason.length() > 160) reason = reason.substring(0, 160);
            r.clear();
            r.put("code", -1);
            r.put("msg", "支付发起失败: " + reason);
        }
        return r;
    }

    /** 微信异步通知 — 必须返回固定 XML 给微信服务器. */
    @PostMapping(value = "/notify", produces = "application/xml; charset=UTF-8")
    @ResponseBody
    public String notify(@RequestBody String notifyData) {
        try {
            payService.notify(notifyData);
            return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
        } catch (Exception e) {
            log.error("【微信支付】异步通知处理失败, 返回 FAIL 由微信重试: {}", e.getMessage());
            return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[处理失败]]></return_msg></xml>";
        }
    }
}
