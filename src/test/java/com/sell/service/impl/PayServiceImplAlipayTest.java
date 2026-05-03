package com.sell.service.impl;

import com.lly835.bestpay.enums.BestPayPlatformEnum;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.lly835.bestpay.model.PayRequest;
import com.lly835.bestpay.model.PayResponse;
import com.lly835.bestpay.model.RefundRequest;
import com.lly835.bestpay.model.RefundResponse;
import com.lly835.bestpay.service.impl.BestPayServiceImpl;
import com.sell.dto.OrderDTO;
import com.sell.enums.PayTypeEnum;
import com.sell.exception.SellException;
import com.sell.service.OrderService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 支付宝相关分支的纯单元测试. Mock 掉 BestPay SDK 和 OrderService,
 * 不启动 Spring, 不需要数据库, 不调真实接口.
 */
@RunWith(MockitoJUnitRunner.class)
public class PayServiceImplAlipayTest {

    @Mock private BestPayServiceImpl bestPayService;
    @Mock private OrderService orderService;
    @InjectMocks private PayServiceImpl payService;

    private OrderDTO order;

    @Before
    public void setUp() {
        order = new OrderDTO();
        order.setOrderId("ORDER_TEST_001");
        order.setBuyerOpenid("openid-x");
        order.setOrderAmount(new BigDecimal("12.34"));
    }

    @Test
    public void createAlipay_setsAlipayWapAndForwardsOrderFields() {
        PayResponse stub = new PayResponse();
        when(bestPayService.pay(any(PayRequest.class))).thenReturn(stub);

        PayResponse out = payService.createAlipay(order);

        ArgumentCaptor<PayRequest> captor = ArgumentCaptor.forClass(PayRequest.class);
        verify(bestPayService).pay(captor.capture());
        PayRequest sent = captor.getValue();
        assertThat(sent.getPayTypeEnum()).isEqualTo(BestPayTypeEnum.ALIPAY_WAP);
        assertThat(sent.getOrderId()).isEqualTo("ORDER_TEST_001");
        assertThat(sent.getOrderAmount()).isEqualTo(12.34d);
        assertThat(out).isSameAs(stub);
    }

    @Test
    public void alipayNotify_paidWhenAmountMatches() {
        PayResponse async = new PayResponse();
        async.setOrderId("ORDER_TEST_001");
        async.setOrderAmount(12.34d);
        when(bestPayService.asyncNotify("data")).thenReturn(async);
        when(orderService.findOne("ORDER_TEST_001")).thenReturn(order);

        PayResponse out = payService.alipayNotify("data");

        verify(orderService).paid(order);
        assertThat(out).isSameAs(async);
    }

    @Test
    public void alipayNotify_orderNotFound_throwsAndDoesNotPay() {
        PayResponse async = new PayResponse();
        async.setOrderId("missing");
        when(bestPayService.asyncNotify("data")).thenReturn(async);
        when(orderService.findOne("missing")).thenReturn(null);

        assertThatThrownBy(() -> payService.alipayNotify("data"))
                .isInstanceOf(SellException.class);
        verify(orderService, never()).paid(any(OrderDTO.class));
    }

    @Test
    public void alipayNotify_amountMismatch_throwsAndDoesNotPay() {
        PayResponse async = new PayResponse();
        async.setOrderId("ORDER_TEST_001");
        async.setOrderAmount(99.99d);
        when(bestPayService.asyncNotify("data")).thenReturn(async);
        when(orderService.findOne("ORDER_TEST_001")).thenReturn(order);

        assertThatThrownBy(() -> payService.alipayNotify("data"))
                .isInstanceOf(SellException.class);
        verify(orderService, never()).paid(any(OrderDTO.class));
    }

    @Test
    public void refund_alipayPayType_routesToAlipayPlatform() {
        order.setPayType(PayTypeEnum.ALIPAY.getCode());
        when(bestPayService.refund(any(RefundRequest.class))).thenReturn(new RefundResponse());

        payService.refund(order);

        ArgumentCaptor<RefundRequest> cap = ArgumentCaptor.forClass(RefundRequest.class);
        verify(bestPayService).refund(cap.capture());
        assertThat(cap.getValue().getPayPlatformEnum()).isEqualTo(BestPayPlatformEnum.ALIPAY);
        assertThat(cap.getValue().getOrderId()).isEqualTo("ORDER_TEST_001");
        assertThat(cap.getValue().getOrderAmount()).isEqualTo(12.34d);
    }

    @Test
    public void refund_wechatPayType_routesToWechatPlatform() {
        order.setPayType(PayTypeEnum.WECHAT.getCode());
        when(bestPayService.refund(any(RefundRequest.class))).thenReturn(new RefundResponse());

        payService.refund(order);

        ArgumentCaptor<RefundRequest> cap = ArgumentCaptor.forClass(RefundRequest.class);
        verify(bestPayService).refund(cap.capture());
        assertThat(cap.getValue().getPayPlatformEnum()).isEqualTo(BestPayPlatformEnum.WX);
    }

    @Test
    public void refund_nullPayType_defaultsToWechat() {
        order.setPayType(null);
        when(bestPayService.refund(any(RefundRequest.class))).thenReturn(new RefundResponse());

        payService.refund(order);

        ArgumentCaptor<RefundRequest> cap = ArgumentCaptor.forClass(RefundRequest.class);
        verify(bestPayService).refund(cap.capture());
        assertThat(cap.getValue().getPayPlatformEnum()).isEqualTo(BestPayPlatformEnum.WX);
    }
}
