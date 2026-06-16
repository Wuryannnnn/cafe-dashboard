package com.sell.service;

import com.sell.dto.OrderDTO;
import com.sell.enums.PayStatusEnum;
import com.sell.exception.SellException;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.model.TransactionAmount;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "admin.auth.enabled=false")
public class MiniPayServiceNotifyTest {

    @Autowired private MiniPayService miniPayService;
    @MockBean private NotificationParser notificationParser;
    @MockBean private OrderService orderService;

    private Transaction tx(String outTradeNo, int fen, String txnId, Transaction.TradeStateEnum state) {
        Transaction t = new Transaction();
        t.setOutTradeNo(outTradeNo);
        t.setTransactionId(txnId);
        t.setTradeState(state);
        TransactionAmount amt = new TransactionAmount();
        amt.setTotal(fen);
        t.setAmount(amt);
        return t;
    }

    private OrderDTO order(String id, int payStatus) {
        OrderDTO o = new OrderDTO();
        o.setOrderId(id);
        o.setOrderAmount(new BigDecimal("18.00"));
        o.setPayStatus(payStatus);
        return o;
    }

    private Map<String, String> headers() {
        Map<String, String> h = new HashMap<>();
        h.put("Wechatpay-Serial", "s");
        h.put("Wechatpay-Nonce", "n");
        h.put("Wechatpay-Signature", "sig");
        h.put("Wechatpay-Timestamp", "123");
        return h;
    }

    @Test
    public void notify_firstTime_savesTxnAndCallsPaid() {
        when(notificationParser.parse(any(RequestParam.class), eq(Transaction.class)))
                .thenReturn(tx("O1", 1800, "TX1", Transaction.TradeStateEnum.SUCCESS));
        when(orderService.findOne("O1")).thenReturn(order("O1", PayStatusEnum.WAIT.getCode()));

        miniPayService.notify(headers(), "body");

        verify(orderService).saveTransactionId("O1", "TX1");
        verify(orderService).paid(any(OrderDTO.class));
    }

    @Test
    public void notify_duplicate_skipsPaid() {
        when(notificationParser.parse(any(RequestParam.class), eq(Transaction.class)))
                .thenReturn(tx("O1", 1800, "TX1", Transaction.TradeStateEnum.SUCCESS));
        when(orderService.findOne("O1")).thenReturn(order("O1", PayStatusEnum.SUCCESS.getCode()));

        miniPayService.notify(headers(), "body");

        verify(orderService, never()).paid(any(OrderDTO.class)); // 幂等: 已支付直接跳过
        verify(orderService, never()).saveTransactionId(any(), any());
    }

    @Test(expected = SellException.class)
    public void notify_amountMismatch_throws() {
        when(notificationParser.parse(any(RequestParam.class), eq(Transaction.class)))
                .thenReturn(tx("O1", 1, "TX1", Transaction.TradeStateEnum.SUCCESS)); // 1 分 ≠ 1800
        when(orderService.findOne("O1")).thenReturn(order("O1", PayStatusEnum.WAIT.getCode()));

        miniPayService.notify(headers(), "body");
    }

    @Test
    public void notify_nonSuccessState_acksButSkipsPaid() {
        when(notificationParser.parse(any(RequestParam.class), eq(Transaction.class)))
                .thenReturn(tx("O1", 1800, "TX1", Transaction.TradeStateEnum.CLOSED));

        boolean ok = miniPayService.notify(headers(), "body");

        org.junit.Assert.assertTrue(ok);
        verify(orderService, never()).paid(any(OrderDTO.class));
    }
}
