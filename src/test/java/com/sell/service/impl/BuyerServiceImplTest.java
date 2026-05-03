package com.sell.service.impl;

import com.sell.dto.OrderDTO;
import com.sell.enums.OrderStatusEnum;
import com.sell.enums.ResultEnum;
import com.sell.exception.SellException;
import com.sell.service.OrderService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BuyerServiceImplTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private BuyerServiceImpl buyerService;

    private static final String OPENID = "test_openid";
    private static final String ORDER_ID = "123456";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void findOrderOne_success() {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderId(ORDER_ID);
        orderDTO.setBuyerOpenid(OPENID);
        when(orderService.findOne(ORDER_ID)).thenReturn(orderDTO);

        OrderDTO result = buyerService.findOrderOne(OPENID, ORDER_ID);

        assertNotNull(result);
        assertEquals(ORDER_ID, result.getOrderId());
    }

    @Test
    public void findOrderOne_wrongOwner() {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderId(ORDER_ID);
        orderDTO.setBuyerOpenid("other_openid");
        when(orderService.findOne(ORDER_ID)).thenReturn(orderDTO);

        try {
            buyerService.findOrderOne(OPENID, ORDER_ID);
            fail("should throw");
        } catch (SellException e) {
            assertEquals(ResultEnum.ORDER_OWNER_ERROR.getCode(), e.getCode());
        }
    }

    @Test
    public void cancelOrder_success() {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderId(ORDER_ID);
        orderDTO.setBuyerOpenid(OPENID);
        orderDTO.setOrderStatus(OrderStatusEnum.NEW.getCode());
        when(orderService.findOne(ORDER_ID)).thenReturn(orderDTO);
        when(orderService.cancel(any())).thenReturn(orderDTO);

        OrderDTO result = buyerService.cancelOrder(OPENID, ORDER_ID);
        assertNotNull(result);
        verify(orderService).cancel(any());
    }
}
