package com.sell.service;

import com.sell.dto.OrderDTO;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"admin.auth.enabled=false", "wechat.mini.appId=wxTESTmini", "wechat.mini.mchId=160000"})
public class MiniPayServiceCreateTest {

    @Autowired private MiniPayService miniPayService;
    @MockBean private JsapiServiceExtension jsapi;

    @Test
    public void create_buildsPrepayRequest_andReturnsFiveFields() {
        PrepayWithRequestPaymentResponse stub = new PrepayWithRequestPaymentResponse();
        stub.setAppId("wxTESTmini");
        stub.setTimeStamp("1700000000");
        stub.setNonceStr("n");
        stub.setPackageVal("prepay_id=pp1");
        stub.setSignType("RSA");
        stub.setPaySign("sig");
        when(jsapi.prepayWithRequestPayment(any(PrepayRequest.class))).thenReturn(stub);

        OrderDTO order = new OrderDTO();
        order.setOrderId("O123");
        order.setOrderAmount(new BigDecimal("18.00"));

        PrepayWithRequestPaymentResponse out = miniPayService.create(order, "openid-1");

        ArgumentCaptor<PrepayRequest> cap = ArgumentCaptor.forClass(PrepayRequest.class);
        verify(jsapi).prepayWithRequestPayment(cap.capture());
        PrepayRequest req = cap.getValue();
        assertEquals("O123", req.getOutTradeNo());
        assertEquals("openid-1", req.getPayer().getOpenid());
        assertEquals(Integer.valueOf(1800), req.getAmount().getTotal()); // 元→分
        assertEquals("CNY", req.getAmount().getCurrency());
        assertEquals("prepay_id=pp1", out.getPackageVal());
        assertEquals("RSA", out.getSignType());
    }
}
