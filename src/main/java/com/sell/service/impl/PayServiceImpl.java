package com.sell.service.impl;

import com.sell.dto.OrderDTO;
import com.sell.enums.PayStatusEnum;
import com.sell.enums.PayTypeEnum;
import com.sell.enums.ResultEnum;
import com.sell.exception.SellException;
import com.sell.service.OrderService;
import com.sell.service.PayService;
import com.sell.utils.JsonUtil;
import com.sell.utils.MathUtil;
import com.lly835.bestpay.enums.BestPayPlatformEnum;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.lly835.bestpay.model.PayRequest;
import com.lly835.bestpay.model.PayResponse;
import com.lly835.bestpay.model.RefundRequest;
import com.lly835.bestpay.model.RefundResponse;
import com.lly835.bestpay.service.impl.BestPayServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PayServiceImpl implements PayService {

    private static final String ORDER_NAME = "咖啡厅订单";

    @Autowired
    private BestPayServiceImpl bestPayService;

    @Autowired
    private OrderService orderService;

    @Override
    public PayResponse create(OrderDTO orderDTO) {
        PayRequest payRequest = new PayRequest();
        payRequest.setOpenid(orderDTO.getBuyerOpenid());
        payRequest.setOrderAmount(orderDTO.getOrderAmount().doubleValue());
        payRequest.setOrderId(orderDTO.getOrderId());
        payRequest.setOrderName(ORDER_NAME);
        // 微信扫码点餐 = 公众号内 JSAPI 支付: best-pay-sdk 里为 WXPAY_MP(返回 appId/timeStamp/nonceStr/package/paySign,
        // 前端用 WeixinJSBridge.getBrandWCPayRequest 调起). 需要真实 openid(经 snsapi_base 网页授权获得).
        payRequest.setPayTypeEnum(BestPayTypeEnum.WXPAY_MP);
        log.info("【微信支付】发起支付, request={}", JsonUtil.toJson(payRequest));

        PayResponse payResponse = bestPayService.pay(payRequest);
        log.info("【微信支付】发起支付, response={}", JsonUtil.toJson(payResponse));
        return payResponse;
    }

    @Override
    public PayResponse notify(String notifyData) {
        PayResponse payResponse = bestPayService.asyncNotify(notifyData);
        log.info("【微信支付】异步通知, payResponse={}", JsonUtil.toJson(payResponse));

        //查询订单
        OrderDTO orderDTO = orderService.findOne(payResponse.getOrderId());
        if (orderDTO == null) {
            log.error("【微信支付】异步通知, 订单不存在, orderId={}", payResponse.getOrderId());
            throw new SellException(ResultEnum.ORDER_NOT_EXIST);
        }

        //幂等: 订单已支付时重复通知直接返回, 不再重复标记(否则 paid() 会抛异常导致微信收不到 SUCCESS 而无限重试)
        if (PayStatusEnum.SUCCESS.getCode().equals(orderDTO.getPayStatus())) {
            log.info("【微信支付】异步通知, 订单已支付, 幂等跳过, orderId={}", payResponse.getOrderId());
            return payResponse;
        }

        //判断金额是否一致
        if (!MathUtil.equals(payResponse.getOrderAmount(), orderDTO.getOrderAmount().doubleValue())) {
            log.error("【微信支付】异步通知, 订单金额不一致, orderId={}, 微信通知金额={}, 系统金额={}",
                    payResponse.getOrderId(),
                    payResponse.getOrderAmount(),
                    orderDTO.getOrderAmount());
            throw new SellException(ResultEnum.WXPAY_NOTIFY_MONEY_VERIFY_ERROR);
        }

        orderService.paid(orderDTO);
        return payResponse;
    }

    @Override
    public RefundResponse refund(OrderDTO orderDTO) {
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setOrderId(orderDTO.getOrderId());
        refundRequest.setOrderAmount(orderDTO.getOrderAmount().doubleValue());

        // 根据支付方式选择退款渠道
        if (orderDTO.getPayType() != null && orderDTO.getPayType().equals(PayTypeEnum.ALIPAY.getCode())) {
            refundRequest.setPayPlatformEnum(BestPayPlatformEnum.ALIPAY);
            log.info("【支付宝退款】request={}", JsonUtil.toJson(refundRequest));
        } else {
            refundRequest.setPayPlatformEnum(BestPayPlatformEnum.WX);
            log.info("【微信退款】request={}", JsonUtil.toJson(refundRequest));
        }

        RefundResponse refundResponse = bestPayService.refund(refundRequest);
        log.info("【退款】response={}", JsonUtil.toJson(refundResponse));
        return refundResponse;
    }
}
