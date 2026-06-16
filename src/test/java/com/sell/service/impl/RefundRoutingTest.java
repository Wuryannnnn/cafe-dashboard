package com.sell.service.impl;

import com.sell.dataobject.OrderDetail;
import com.sell.dataobject.OrderMaster;
import com.sell.dataobject.ProductInfo;
import com.sell.dto.OrderDTO;
import com.sell.enums.PayStatusEnum;
import com.sell.repository.OrderMasterRepository;
import com.sell.repository.ProductInfoRepository;
import com.sell.service.MiniPayService;
import com.sell.service.OrderService;
import com.sell.service.PayService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** 退款渠道分流: 有 wxTransactionId → APIv3; 否则 → best-pay 历史单. */
@RunWith(SpringRunner.class)
@SpringBootTest(properties = "admin.auth.enabled=false")
@Transactional
public class RefundRoutingTest {

    @Autowired private OrderService orderService;
    @Autowired private ProductInfoRepository productRepo;
    @Autowired private OrderMasterRepository orderMasterRepo;
    @MockBean private MiniPayService miniPayService;
    @MockBean private PayService payService;

    private static final String PID = "REFUND_ROUTE_P1";

    @Before
    public void seed() {
        ProductInfo p = new ProductInfo();
        p.setProductId(PID);
        p.setProductName("美式");
        p.setProductPrice(new BigDecimal("18.00"));
        p.setProductStock(100);
        p.setProductStatus(0);
        p.setCategoryType(1);
        productRepo.save(p);
    }

    private String createPaidOrder(String txnId) {
        OrderDTO dto = new OrderDTO();
        dto.setBuyerOpenid("route_openid");
        dto.setBuyerName("顾客");
        dto.setBuyerPhone("");
        dto.setBuyerAddress("");
        OrderDetail d = new OrderDetail();
        d.setProductId(PID);
        d.setProductQuantity(1);
        List<OrderDetail> list = new ArrayList<>();
        list.add(d);
        dto.setOrderDetailList(list);
        OrderDTO created = orderService.create(dto);

        OrderMaster om = orderMasterRepo.findById(created.getOrderId()).orElseThrow();
        om.setPayStatus(PayStatusEnum.SUCCESS.getCode());
        om.setWxTransactionId(txnId); // null = 历史单
        orderMasterRepo.save(om);
        return created.getOrderId();
    }

    @Test
    public void refund_withTxnId_routesToApiV3() {
        String orderId = createPaidOrder("TX-APIV3-1");
        orderService.refund(orderService.findOne(orderId));
        verify(miniPayService).refund(eq(orderId), any(BigDecimal.class));
        verify(payService, never()).refund(any());
    }

    @Test
    public void refund_noTxnId_routesToBestPay() {
        String orderId = createPaidOrder(null);
        orderService.refund(orderService.findOne(orderId));
        verify(payService).refund(any());
        verify(miniPayService, never()).refund(any(), any(BigDecimal.class));
    }
}
