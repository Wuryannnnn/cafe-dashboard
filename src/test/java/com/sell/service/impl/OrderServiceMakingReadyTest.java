package com.sell.service.impl;

import com.sell.dataobject.OrderMaster;
import com.sell.dto.OrderDTO;
import com.sell.enums.OrderStatusEnum;
import com.sell.enums.PayStatusEnum;
import com.sell.enums.ResultEnum;
import com.sell.exception.SellException;
import com.sell.repository.OrderDetailRepository;
import com.sell.repository.OrderMasterRepository;
import com.sell.repository.ProductSkuRepository;
import com.sell.service.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class OrderServiceMakingReadyTest {

    @Mock private ProductService productService;
    @Mock private OrderDetailRepository orderDetailRepository;
    @Mock private OrderMasterRepository orderMasterRepository;
    @Mock private ProductSkuRepository productSkuRepository;
    @Mock private PayService payService;
    @Mock private PushMessageService pushMessageService;
    @Mock private PrinterService printerService;
    @Mock private PickupNumberService pickupNumberService;
    @Mock private WebSocket webSocket;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(orderMasterRepository.save(any(OrderMaster.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ======== making() ========

    @Test
    public void making_success() {
        OrderDTO dto = buildDTO(OrderStatusEnum.NEW);
        OrderDTO result = orderService.making(dto);
        assertEquals(OrderStatusEnum.MAKING.getCode(), result.getOrderStatus());
    }

    @Test(expected = SellException.class)
    public void making_invalidStatus_finished() {
        orderService.making(buildDTO(OrderStatusEnum.FINISHED));
    }

    @Test(expected = SellException.class)
    public void making_invalidStatus_cancel() {
        orderService.making(buildDTO(OrderStatusEnum.CANCEL));
    }

    @Test(expected = SellException.class)
    public void making_invalidStatus_alreadyMaking() {
        orderService.making(buildDTO(OrderStatusEnum.MAKING));
    }

    // ======== ready() ========

    @Test
    public void ready_success() {
        OrderDTO dto = buildDTO(OrderStatusEnum.MAKING);
        OrderDTO result = orderService.ready(dto);
        assertEquals(OrderStatusEnum.READY.getCode(), result.getOrderStatus());
        verify(pushMessageService).orderStatus(any());
        verify(webSocket).sendMessage(contains("order_ready"));
    }

    @Test(expected = SellException.class)
    public void ready_invalidStatus_new() {
        orderService.ready(buildDTO(OrderStatusEnum.NEW));
    }

    // ======== finish() ========

    @Test
    public void finish_fromReady() {
        OrderDTO dto = buildDTO(OrderStatusEnum.READY);
        OrderDTO result = orderService.finish(dto);
        assertEquals(OrderStatusEnum.FINISHED.getCode(), result.getOrderStatus());
    }

    @Test
    public void finish_fromNew() {
        OrderDTO dto = buildDTO(OrderStatusEnum.NEW);
        OrderDTO result = orderService.finish(dto);
        assertEquals(OrderStatusEnum.FINISHED.getCode(), result.getOrderStatus());
    }

    @Test
    public void finish_fromMaking() {
        OrderDTO dto = buildDTO(OrderStatusEnum.MAKING);
        OrderDTO result = orderService.finish(dto);
        assertEquals(OrderStatusEnum.FINISHED.getCode(), result.getOrderStatus());
    }

    @Test(expected = SellException.class)
    public void finish_invalidStatus_cancelled() {
        orderService.finish(buildDTO(OrderStatusEnum.CANCEL));
    }

    // ======== cancel() ========
    // 注: paid/cancel/refund 现以悲观行锁重读订单(findByOrderIdForUpdate)的最新状态为准,
    // 故需 stub 该方法返回与场景一致的 OrderMaster.

    @Test(expected = SellException.class)
    public void cancel_alreadyFinished() {
        lock(OrderStatusEnum.FINISHED, PayStatusEnum.SUCCESS);
        OrderDTO dto = buildDTO(OrderStatusEnum.FINISHED);
        dto.setOrderDetailList(new java.util.ArrayList<>());
        orderService.cancel(dto);
    }

    @Test(expected = SellException.class)
    public void cancel_alreadyCancelled() {
        lock(OrderStatusEnum.CANCEL, PayStatusEnum.WAIT);
        OrderDTO dto = buildDTO(OrderStatusEnum.CANCEL);
        dto.setOrderDetailList(new java.util.ArrayList<>());
        orderService.cancel(dto);
    }

    // ======== paid() ========

    @Test
    public void paid_success() {
        lock(OrderStatusEnum.NEW, PayStatusEnum.WAIT);
        OrderDTO dto = buildDTO(OrderStatusEnum.NEW);
        dto.setPayStatus(PayStatusEnum.WAIT.getCode());
        OrderDTO result = orderService.paid(dto);
        assertEquals(PayStatusEnum.SUCCESS.getCode(), result.getPayStatus());
    }

    @Test(expected = SellException.class)
    public void paid_cancelledOrderRejected() {
        // 已取消订单不能标记支付 (注: 已完结现允许支付, 以兼容先食后付)
        lock(OrderStatusEnum.CANCEL, PayStatusEnum.WAIT);
        OrderDTO dto = buildDTO(OrderStatusEnum.CANCEL);
        dto.setPayStatus(PayStatusEnum.WAIT.getCode());
        orderService.paid(dto);
    }

    @Test(expected = SellException.class)
    public void paid_alreadyPaid() {
        lock(OrderStatusEnum.NEW, PayStatusEnum.SUCCESS);
        OrderDTO dto = buildDTO(OrderStatusEnum.NEW);
        dto.setPayStatus(PayStatusEnum.SUCCESS.getCode());
        orderService.paid(dto);
    }

    /** stub 行锁重读, 返回带指定状态的订单主表. */
    private void lock(OrderStatusEnum status, PayStatusEnum payStatus) {
        OrderMaster m = new OrderMaster();
        m.setOrderId("test_order_001");
        m.setOrderStatus(status.getCode());
        m.setPayStatus(payStatus.getCode());
        when(orderMasterRepository.findByOrderIdForUpdate(anyString())).thenReturn(Optional.of(m));
    }

    private OrderDTO buildDTO(OrderStatusEnum status) {
        OrderDTO dto = new OrderDTO();
        dto.setOrderId("test_order_001");
        dto.setOrderStatus(status.getCode());
        dto.setBuyerOpenid("test_openid");
        dto.setBuyerName("test");
        dto.setBuyerPhone("13800138000");
        dto.setBuyerAddress("");
        return dto;
    }
}
