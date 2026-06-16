package com.sell.service;

import com.sell.config.WechatMiniConfig;
import com.sell.dto.OrderDTO;
import com.sell.enums.PayStatusEnum;
import com.sell.exception.SellException;
import com.sell.utils.MoneyUtil;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 微信小程序支付 (APIv3, wechatpay-java 0.2.17, 公钥模式): 下单 / 回调 / 退款.
 * SDK Bean 仅在 wechat.mini.enabled=true 时装配; 未装配时各方法抛可读错误.
 */
@Service
@Slf4j
public class MiniPayService {

    private static final String ORDER_NAME = "咖啡厅订单";

    @Autowired private WechatMiniConfig cfg;
    @Autowired(required = false) private JsapiServiceExtension jsapi;
    @Autowired(required = false) private RefundService refundService;
    @Autowired(required = false) private NotificationParser notificationParser;
    @Autowired private OrderService orderService;

    /** APIv3 小程序下单 + 调起签名 (SDK 一步返回 5 字段). */
    public PrepayWithRequestPaymentResponse create(OrderDTO order, String openid) {
        if (jsapi == null) {
            throw new SellException(-1, "支付未配置, 请联系商家");
        }
        PrepayRequest req = new PrepayRequest();
        req.setAppid(cfg.getAppId());
        req.setMchid(cfg.getMchId());
        req.setDescription(ORDER_NAME);
        req.setOutTradeNo(order.getOrderId());
        req.setNotifyUrl(cfg.getNotifyUrl());
        Amount amount = new Amount();
        amount.setTotal(MoneyUtil.yuanToFen(order.getOrderAmount()));
        amount.setCurrency("CNY");
        req.setAmount(amount);
        Payer payer = new Payer();
        payer.setOpenid(openid);
        req.setPayer(payer);
        return jsapi.prepayWithRequestPayment(req);
    }

    /**
     * APIv3 回调: 验签+解密(SDK) → 幂等前置 → 金额比对(分) → 写交易号 + 标记已支付.
     * 幂等关键: 已支付直接返回, 不再调 paid (paid 对非 WAIT 抛异常, 否则微信无限重试).
     */
    public boolean notify(Map<String, String> headers, String body) {
        if (notificationParser == null) {
            throw new SellException(-1, "支付未配置");
        }
        RequestParam params = new RequestParam.Builder()
                .serialNumber(headers.get("Wechatpay-Serial"))
                .nonce(headers.get("Wechatpay-Nonce"))
                .signature(headers.get("Wechatpay-Signature"))
                .timestamp(headers.get("Wechatpay-Timestamp"))
                .body(body)
                .build();
        Transaction tx = notificationParser.parse(params, Transaction.class);
        String orderId = tx.getOutTradeNo();
        // 仅成功状态才标记已支付; 其它状态(如 CLOSED/REVOKED)应答成功避免重试, 但不动订单
        if (tx.getTradeState() != Transaction.TradeStateEnum.SUCCESS) {
            log.warn("[小程序支付] 回调非成功状态 {} orderId={}", tx.getTradeState(), orderId);
            return true;
        }
        OrderDTO order = orderService.findOne(orderId);

        if (PayStatusEnum.SUCCESS.getCode().equals(order.getPayStatus())) {
            log.info("[小程序支付] 回调幂等跳过 orderId={}", orderId);
            return true;
        }
        int needFen = MoneyUtil.yuanToFen(order.getOrderAmount());
        int gotFen = (tx.getAmount() != null && tx.getAmount().getTotal() != null) ? tx.getAmount().getTotal() : -1;
        if (needFen != gotFen) {
            log.error("[小程序支付] 金额不一致 orderId={} 需{}分 收{}分", orderId, needFen, gotFen);
            throw new SellException(-1, "回调金额不一致");
        }
        // 先持久化交易号(退款分流依据), 再标记已支付
        orderService.saveTransactionId(orderId, tx.getTransactionId());
        orderService.paid(order);
        return true;
    }

    /** APIv3 退款; out_refund_no 用 R+订单号 (单订单单次退款; 多次退款需带序号并落库). */
    public void refund(String orderId, BigDecimal amountYuan) {
        if (refundService == null) {
            throw new SellException(-1, "支付未配置");
        }
        CreateRequest req = new CreateRequest();
        req.setOutTradeNo(orderId);
        req.setOutRefundNo("R" + orderId);
        AmountReq amt = new AmountReq();
        long fen = MoneyUtil.yuanToFen(amountYuan);
        amt.setRefund(fen);
        amt.setTotal(fen);
        amt.setCurrency("CNY");
        req.setAmount(amt);
        refundService.create(req);
    }
}
