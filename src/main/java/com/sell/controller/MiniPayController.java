package com.sell.controller;

import com.sell.dto.OrderDTO;
import com.sell.interceptor.MiniAuthInterceptor;
import com.sell.service.MiniPayService;
import com.sell.service.OrderService;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/pay/mini")
@Slf4j
public class MiniPayController {

    @Autowired private MiniPayService miniPayService;
    @Autowired private OrderService orderService;

    /** 发起小程序支付: 返回 wx.requestPayment 需要的 5 字段; 失败返回可读错误(不抛 500). */
    @PostMapping("/create")
    public Map<String, Object> create(@RequestParam("orderId") String orderId, HttpServletRequest request) {
        String openid = (String) request.getAttribute(MiniAuthInterceptor.ATTR_OPENID);
        Map<String, Object> r = new LinkedHashMap<>();
        try {
            OrderDTO order = orderService.findOne(orderId);
            PrepayWithRequestPaymentResponse p = miniPayService.create(order, openid);
            r.put("timeStamp", p.getTimeStamp());
            r.put("nonceStr", p.getNonceStr());
            r.put("package", p.getPackageVal());
            r.put("signType", p.getSignType());
            r.put("paySign", p.getPaySign());
        } catch (Exception e) {
            log.error("【小程序支付】发起失败 orderId={}: {}", orderId, e.getMessage());
            r.put("code", -1);
            r.put("msg", "支付发起失败: " + e.getMessage());
        }
        return r;
    }

    /** APIv3 支付结果回调: 验签解密+幂等由 service 处理; 失败抛异常→非200 让微信重试. */
    @PostMapping("/notify")
    public Map<String, String> notify(@RequestBody String body,
                                      @RequestHeader("Wechatpay-Serial") String serial,
                                      @RequestHeader("Wechatpay-Nonce") String nonce,
                                      @RequestHeader("Wechatpay-Signature") String signature,
                                      @RequestHeader("Wechatpay-Timestamp") String timestamp) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Wechatpay-Serial", serial);
        headers.put("Wechatpay-Nonce", nonce);
        headers.put("Wechatpay-Signature", signature);
        headers.put("Wechatpay-Timestamp", timestamp);
        try {
            miniPayService.notify(headers, body);
        } catch (Exception e) {
            log.error("【小程序支付回调】处理失败: {}", e.getMessage());
            throw new RuntimeException(e); // 非 200 让微信重试
        }
        Map<String, String> resp = new HashMap<>();
        resp.put("code", "SUCCESS");
        resp.put("message", "成功");
        return resp;
    }
}
