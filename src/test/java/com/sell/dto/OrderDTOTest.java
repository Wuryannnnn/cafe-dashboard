package com.sell.dto;

import com.sell.enums.DiningTypeEnum;
import com.sell.enums.OrderStatusEnum;
import com.sell.enums.PayStatusEnum;
import com.sell.enums.PayTypeEnum;
import org.junit.Test;

import static org.junit.Assert.*;

public class OrderDTOTest {

    @Test
    public void getOrderStatusEnum() {
        OrderDTO dto = new OrderDTO();

        dto.setOrderStatus(OrderStatusEnum.NEW.getCode());
        assertEquals(OrderStatusEnum.NEW, dto.getOrderStatusEnum());

        dto.setOrderStatus(OrderStatusEnum.MAKING.getCode());
        assertEquals(OrderStatusEnum.MAKING, dto.getOrderStatusEnum());

        dto.setOrderStatus(OrderStatusEnum.READY.getCode());
        assertEquals(OrderStatusEnum.READY, dto.getOrderStatusEnum());
    }

    @Test
    public void getPayStatusEnum() {
        OrderDTO dto = new OrderDTO();
        dto.setPayStatus(PayStatusEnum.WAIT.getCode());
        assertEquals(PayStatusEnum.WAIT, dto.getPayStatusEnum());
    }

    @Test
    public void getPayTypeEnum() {
        OrderDTO dto = new OrderDTO();
        dto.setPayType(PayTypeEnum.ALIPAY.getCode());
        assertEquals(PayTypeEnum.ALIPAY, dto.getPayTypeEnum());
    }

    @Test
    public void getDiningTypeEnum() {
        OrderDTO dto = new OrderDTO();
        dto.setDiningType(DiningTypeEnum.TAKEAWAY.getCode());
        assertEquals(DiningTypeEnum.TAKEAWAY, dto.getDiningTypeEnum());
    }

    @Test
    public void newFields() {
        OrderDTO dto = new OrderDTO();
        dto.setPickupNumber("001");
        dto.setOrderRemark("少冰");
        dto.setTableNumber("A3");

        assertEquals("001", dto.getPickupNumber());
        assertEquals("少冰", dto.getOrderRemark());
        assertEquals("A3", dto.getTableNumber());
    }
}
