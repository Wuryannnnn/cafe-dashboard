package com.sell.service;

import com.sell.dto.OrderDTO;
import com.lly835.bestpay.model.PayResponse;
import com.lly835.bestpay.model.RefundResponse;

public interface PayService {

    /** 微信支付. */
    PayResponse create(OrderDTO orderDTO);

    /** 支付宝支付. */
    PayResponse createAlipay(OrderDTO orderDTO);

    /** 微信支付异步通知. */
    PayResponse notify(String notifyData);

    /** 支付宝异步通知. */
    PayResponse alipayNotify(String notifyData);

    /** 退款(自动判断支付方式). */
    RefundResponse refund(OrderDTO orderDTO);
}
