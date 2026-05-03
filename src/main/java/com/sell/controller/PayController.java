package com.sell.controller;

import com.sell.dto.OrderDTO;
import com.sell.enums.PayTypeEnum;
import com.sell.enums.ResultEnum;
import com.sell.exception.SellException;
import com.sell.service.OrderService;
import com.sell.service.PayService;
import com.lly835.bestpay.model.PayResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/pay")
public class PayController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PayService payService;

    /**
     * 发起支付 - JSON 端点
     * 返回微信 JSAPI 签名参数 (或支付宝跳转 URL), 供 React /pay 页面调用 WeixinJSBridge.invoke 用.
     */
    @GetMapping("/create")
    @ResponseBody
    public Map<String, Object> create(@RequestParam("orderId") String orderId,
                                       @RequestParam("returnUrl") String returnUrl,
                                       @RequestParam(value = "payType", required = false) Integer payType) {
        OrderDTO orderDTO = orderService.findOne(orderId);
        if (orderDTO == null) {
            throw new SellException(ResultEnum.ORDER_NOT_EXIST);
        }
        // payType 参数优先于订单创建时存的字段, 让用户在支付页临时改主意 (微信 ↔ 支付宝)
        Integer effectivePayType = payType != null ? payType : orderDTO.getPayType();
        if (effectivePayType != null && !effectivePayType.equals(orderDTO.getPayType())) {
            orderDTO.setPayType(effectivePayType);
        }
        boolean isAlipay = effectivePayType != null && effectivePayType.equals(PayTypeEnum.ALIPAY.getCode());
        PayResponse payResponse = isAlipay ? payService.createAlipay(orderDTO) : payService.create(orderDTO);
        Map<String, Object> r = new HashMap<>();
        r.put("payType", isAlipay ? "alipay" : "wechat");
        r.put("appId", payResponse.getAppId());
        r.put("timeStamp", payResponse.getTimeStamp());
        r.put("nonceStr", payResponse.getNonceStr());
        r.put("packAge", payResponse.getPackAge());
        r.put("paySign", payResponse.getPaySign());
        r.put("returnUrl", returnUrl);
        // 支付宝 WAP 跳转地址在 payUri / body, 微信 H5 在 mwebUrl
        try { r.put("mwebUrl", payResponse.getMwebUrl()); } catch (Throwable ignored) {}
        try { r.put("codeUrl", payResponse.getCodeUrl()); } catch (Throwable ignored) {}
        try {
            java.net.URI u = payResponse.getPayUri();
            if (u != null) r.put("payUri", patchSandboxUrl(u.toString()));
        } catch (Throwable ignored) {}
        try { r.put("body", patchSandboxUrl(payResponse.getBody())); } catch (Throwable ignored) {}
        return r;
    }

    /**
     * bestpay 1.3.7 内置的支付宝沙箱地址 openapi.alipaydev.com 已下线 (502).
     * 替换成新地址 openapi-sandbox.dl.alipaydev.com.
     * 同时把 SDK bug 产生的双斜杠 // 修掉.
     */
    static String patchSandboxUrl(String s) {
        if (s == null) return null;
        return s
                .replace("openapi.alipaydev.com//", "openapi-sandbox.dl.alipaydev.com/")
                .replace("openapi.alipaydev.com/", "openapi-sandbox.dl.alipaydev.com/")
                .replace("openapi.alipaydev.com", "openapi-sandbox.dl.alipaydev.com");
    }

    /**
     * 微信异步通知 — 必须返回固定 XML 给微信服务器
     */
    @PostMapping(value = "/notify", produces = "application/xml; charset=UTF-8")
    @org.springframework.web.bind.annotation.ResponseBody
    public String notify(@RequestBody String notifyData) {
        payService.notify(notifyData);
        return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
    }

    /**
     * 支付宝异步通知
     */
    @PostMapping(value = "/alipay/notify", produces = "text/plain; charset=UTF-8")
    @ResponseBody
    public String alipayNotify(@RequestBody String notifyData) {
        payService.alipayNotify(notifyData);
        return "success";
    }
}
