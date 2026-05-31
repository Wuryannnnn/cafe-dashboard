package com.sell.controller;

import com.sell.dto.OrderDTO;
import com.sell.enums.PayTypeEnum;
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

@Controller
@RequestMapping("/pay")
@Slf4j
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
            // 必须持久化, 否则退款时仍按下单时的旧渠道退款 → 退款打到错误渠道、失败
            orderDTO = orderService.updatePayType(orderId, effectivePayType);
        }
        boolean isAlipay = effectivePayType != null && effectivePayType.equals(PayTypeEnum.ALIPAY.getCode());
        Map<String, Object> r = new HashMap<>();
        try {
            PayResponse payResponse = isAlipay ? payService.createAlipay(orderDTO) : payService.create(orderDTO);
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
        } catch (Exception e) {
            // 支付网关发起失败(最常见: 商户凭据未配置/格式错误). 返回可读 {code,msg} 而不是 500,
            // 让前端 pay 页显示具体原因(否则只会显示"后端无返回"). 详细栈进日志.
            log.error("【发起支付】失败 orderId={}, payType={}: {}", orderId, effectivePayType, e.getMessage());
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (reason.length() > 160) reason = reason.substring(0, 160);
            r.clear();
            r.put("code", -1);
            r.put("msg", "支付发起失败: " + reason);
        }
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
        try {
            payService.notify(notifyData);
            return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
        } catch (Exception e) {
            // 关键: 异常时也要回 XML 而不是让全局异常处理器返回 JSON,
            // 否则微信收不到可识别的应答会一直重试. 回 FAIL 让微信按策略重试(有限次).
            log.error("【微信支付】异步通知处理失败, 返回 FAIL 由微信重试: {}", e.getMessage());
            return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[处理失败]]></return_msg></xml>";
        }
    }

    /**
     * 支付宝异步通知
     */
    @PostMapping(value = "/alipay/notify", produces = "text/plain; charset=UTF-8")
    @ResponseBody
    public String alipayNotify(@RequestBody String notifyData) {
        try {
            payService.alipayNotify(notifyData);
            return "success";
        } catch (Exception e) {
            // 支付宝约定: 返回 "success" 表示已处理(不再通知), 其它表示失败(继续重试).
            log.error("【支付宝支付】异步通知处理失败, 返回 failure 由支付宝重试: {}", e.getMessage());
            return "failure";
        }
    }
}
