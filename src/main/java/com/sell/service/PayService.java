package com.sell.service;

import com.sell.dto.OrderDTO;
import com.lly835.bestpay.model.PayResponse;
import com.lly835.bestpay.model.RefundResponse;

public interface PayService {

    /** 微信支付 (公众号 best-pay; 历史/存量). */
    PayResponse create(OrderDTO orderDTO);

    /** 微信支付异步通知. */
    PayResponse notify(String notifyData);

    /** 退款 (微信 best-pay; 历史单. 小程序 APIv3 新单退款由 MiniPayService 处理). */
    RefundResponse refund(OrderDTO orderDTO);
}
