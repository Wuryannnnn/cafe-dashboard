package com.sell.converter;

import com.sell.dto.OrderDTO;
import com.sell.enums.ResultEnum;
import com.sell.exception.SellException;
import com.sell.form.OrderForm;
import org.junit.Test;

import static org.junit.Assert.*;

public class OrderForm2OrderDTOConverterTest {

    @Test
    public void convert_success() {
        OrderForm form = new OrderForm();
        form.setName("张三");
        form.setPhone("13800138000");
        form.setAddress("A3桌");
        form.setOpenid("abc123");
        form.setItems("[{\"productId\":\"prod001\",\"productQuantity\":2}]");
        form.setDiningType(0);
        form.setTableNumber("A3");
        form.setPayType(1);
        form.setRemark("少冰");

        OrderDTO dto = OrderForm2OrderDTOConverter.convert(form);

        assertEquals("张三", dto.getBuyerName());
        assertEquals("13800138000", dto.getBuyerPhone());
        assertEquals("A3桌", dto.getBuyerAddress());
        assertEquals("abc123", dto.getBuyerOpenid());
        assertEquals(Integer.valueOf(0), dto.getDiningType());
        assertEquals("A3", dto.getTableNumber());
        assertEquals(Integer.valueOf(1), dto.getPayType());
        assertEquals("少冰", dto.getOrderRemark());
        assertNotNull(dto.getOrderDetailList());
        assertEquals(1, dto.getOrderDetailList().size());
    }

    @Test
    public void convert_nullOptionalFields() {
        OrderForm form = new OrderForm();
        form.setName("张三");
        form.setPhone("13800138000");
        form.setOpenid("abc123");
        form.setItems("[{\"productId\":\"prod001\",\"productQuantity\":1}]");

        OrderDTO dto = OrderForm2OrderDTOConverter.convert(form);

        assertEquals("", dto.getBuyerAddress());
        assertEquals(Integer.valueOf(0), dto.getDiningType());
        assertEquals(Integer.valueOf(0), dto.getPayType());
        assertNull(dto.getOrderRemark());
    }

    @Test(expected = SellException.class)
    public void convert_invalidItems() {
        OrderForm form = new OrderForm();
        form.setName("张三");
        form.setPhone("13800138000");
        form.setOpenid("abc123");
        form.setItems("not json");

        OrderForm2OrderDTOConverter.convert(form);
    }
}
