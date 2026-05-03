package com.sell.utils;

import com.sell.enums.OrderStatusEnum;
import com.sell.enums.PayStatusEnum;
import com.sell.enums.DiningTypeEnum;
import com.sell.enums.PayTypeEnum;
import com.sell.enums.PrintStationEnum;
import org.junit.Test;

import static org.junit.Assert.*;

public class EnumUtilTest {

    @Test
    public void getByCode_validCode() {
        assertEquals(OrderStatusEnum.NEW, EnumUtil.getByCode(0, OrderStatusEnum.class));
        assertEquals(OrderStatusEnum.MAKING, EnumUtil.getByCode(1, OrderStatusEnum.class));
        assertEquals(OrderStatusEnum.READY, EnumUtil.getByCode(2, OrderStatusEnum.class));
        assertEquals(OrderStatusEnum.FINISHED, EnumUtil.getByCode(3, OrderStatusEnum.class));
        assertEquals(OrderStatusEnum.CANCEL, EnumUtil.getByCode(4, OrderStatusEnum.class));
    }

    @Test
    public void getByCode_invalidCode() {
        assertNull(EnumUtil.getByCode(99, OrderStatusEnum.class));
    }

    @Test
    public void getByCode_payStatus() {
        assertEquals(PayStatusEnum.WAIT, EnumUtil.getByCode(0, PayStatusEnum.class));
        assertEquals(PayStatusEnum.SUCCESS, EnumUtil.getByCode(1, PayStatusEnum.class));
    }

    @Test
    public void getByCode_diningType() {
        assertEquals(DiningTypeEnum.DINE_IN, EnumUtil.getByCode(0, DiningTypeEnum.class));
        assertEquals(DiningTypeEnum.TAKEAWAY, EnumUtil.getByCode(1, DiningTypeEnum.class));
    }

    @Test
    public void getByCode_payType() {
        assertEquals(PayTypeEnum.WECHAT, EnumUtil.getByCode(0, PayTypeEnum.class));
        assertEquals(PayTypeEnum.ALIPAY, EnumUtil.getByCode(1, PayTypeEnum.class));
    }

    @Test
    public void getByCode_printStation() {
        assertEquals(PrintStationEnum.BAR, EnumUtil.getByCode(0, PrintStationEnum.class));
        assertEquals(PrintStationEnum.KITCHEN, EnumUtil.getByCode(1, PrintStationEnum.class));
    }
}
