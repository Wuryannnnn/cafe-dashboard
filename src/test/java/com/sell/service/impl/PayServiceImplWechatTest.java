package com.sell.service.impl;

import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.lly835.bestpay.model.PayRequest;
import com.lly835.bestpay.model.PayResponse;
import com.lly835.bestpay.service.impl.BestPayServiceImpl;
import com.sell.dto.OrderDTO;
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

@RunWith(MockitoJUnitRunner.class)
public class PayServiceImplWechatTest {

    @Mock private BestPayServiceImpl bestPayService;
    @Mock private OrderService orderService;
    @InjectMocks private PayServiceImpl payService;

    private OrderDTO order;

    @Before
    public void setUp() {
        order = new OrderDTO();
        order.setOrderId("WX_ORDER_1");
        order.setBuyerOpenid("openid-wx");
        order.setOrderAmount(new BigDecimal("8.88"));
    }

    @Test
    public void create_setsWxpayMwebAndForwardsOrderFields() {
        PayResponse stub = new PayResponse();
        when(bestPayService.pay(any(PayRequest.class))).thenReturn(stub);

        PayResponse out = payService.create(order);

        ArgumentCaptor<PayRequest> captor = ArgumentCaptor.forClass(PayRequest.class);
        verify(bestPayService).pay(captor.capture());
        PayRequest sent = captor.getValue();
        assertThat(sent.getPayTypeEnum()).isEqualTo(BestPayTypeEnum.WXPAY_MWEB);
        assertThat(sent.getOpenid()).isEqualTo("openid-wx");
        assertThat(sent.getOrderId()).isEqualTo("WX_ORDER_1");
        assertThat(sent.getOrderAmount()).isEqualTo(8.88d);
        assertThat(out).isSameAs(stub);
    }

    @Test
    public void notify_paidWhenAmountMatches() {
        PayResponse async = new PayResponse();
        async.setOrderId("WX_ORDER_1");
        async.setOrderAmount(8.88d);
        when(bestPayService.asyncNotify("xml")).thenReturn(async);
        when(orderService.findOne("WX_ORDER_1")).thenReturn(order);

        PayResponse out = payService.notify("xml");

        verify(orderService).paid(order);
        assertThat(out).isSameAs(async);
    }

    @Test
    public void notify_orderNotFound_throwsAndDoesNotPay() {
        PayResponse async = new PayResponse();
        async.setOrderId("missing");
        when(bestPayService.asyncNotify("xml")).thenReturn(async);
        when(orderService.findOne("missing")).thenReturn(null);

        assertThatThrownBy(() -> payService.notify("xml"))
                .isInstanceOf(SellException.class);
        verify(orderService, never()).paid(any(OrderDTO.class));
    }

    @Test
    public void notify_amountMismatch_throwsAndDoesNotPay() {
        PayResponse async = new PayResponse();
        async.setOrderId("WX_ORDER_1");
        async.setOrderAmount(0.01d);
        when(bestPayService.asyncNotify("xml")).thenReturn(async);
        when(orderService.findOne("WX_ORDER_1")).thenReturn(order);

        assertThatThrownBy(() -> payService.notify("xml"))
                .isInstanceOf(SellException.class);
        verify(orderService, never()).paid(any(OrderDTO.class));
    }
}
