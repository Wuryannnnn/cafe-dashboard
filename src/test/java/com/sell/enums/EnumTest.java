package com.sell.enums;

import org.junit.Test;

import static org.junit.Assert.*;

public class EnumTest {

    @Test
    public void orderStatusEnum_values() {
        assertEquals(Integer.valueOf(0), OrderStatusEnum.NEW.getCode());
        assertEquals("新订单", OrderStatusEnum.NEW.getMessage());
        assertEquals(Integer.valueOf(1), OrderStatusEnum.MAKING.getCode());
        assertEquals("制作中", OrderStatusEnum.MAKING.getMessage());
        assertEquals(Integer.valueOf(2), OrderStatusEnum.READY.getCode());
        assertEquals("待取餐", OrderStatusEnum.READY.getMessage());
        assertEquals(Integer.valueOf(3), OrderStatusEnum.FINISHED.getCode());
        assertEquals(Integer.valueOf(4), OrderStatusEnum.CANCEL.getCode());
    }

    @Test
    public void diningTypeEnum_values() {
        assertEquals(Integer.valueOf(0), DiningTypeEnum.DINE_IN.getCode());
        assertEquals("堂食", DiningTypeEnum.DINE_IN.getMessage());
        assertEquals(Integer.valueOf(1), DiningTypeEnum.TAKEAWAY.getCode());
        assertEquals("外带", DiningTypeEnum.TAKEAWAY.getMessage());
    }

    @Test
    public void payTypeEnum_values() {
        assertEquals(Integer.valueOf(0), PayTypeEnum.WECHAT.getCode());
        assertEquals("微信支付", PayTypeEnum.WECHAT.getMessage());
        assertEquals(Integer.valueOf(1), PayTypeEnum.ALIPAY.getCode());
        assertEquals("支付宝", PayTypeEnum.ALIPAY.getMessage());
    }

    @Test
    public void printStationEnum_values() {
        assertEquals(Integer.valueOf(0), PrintStationEnum.BAR.getCode());
        assertEquals("吧台", PrintStationEnum.BAR.getMessage());
        assertEquals(Integer.valueOf(1), PrintStationEnum.KITCHEN.getCode());
        assertEquals("后厨", PrintStationEnum.KITCHEN.getMessage());
    }

    @Test
    public void resultEnum_newValues() {
        assertNotNull(ResultEnum.ORDER_MAKING_SUCCESS);
        assertNotNull(ResultEnum.ORDER_READY_SUCCESS);
        assertNotNull(ResultEnum.PRODUCT_SKU_NOT_EXIST);
        assertNotNull(ResultEnum.PAY_TYPE_ERROR);
        assertNotNull(ResultEnum.ALIPAY_NOTIFY_VERIFY_ERROR);
    }

    @Test
    public void productStatusEnum_values() {
        assertEquals(Integer.valueOf(0), ProductStatusEnum.UP.getCode());
        assertEquals(Integer.valueOf(1), ProductStatusEnum.DOWN.getCode());
    }
}
