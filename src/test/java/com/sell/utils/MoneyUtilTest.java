package com.sell.utils;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class MoneyUtilTest {

    @Test
    public void yuanToFen_coversFloatingPointEdgeCases() {
        assertEquals(1, MoneyUtil.yuanToFen(new BigDecimal("0.01")));
        assertEquals(10, MoneyUtil.yuanToFen(new BigDecimal("0.10")));
        assertEquals(7, MoneyUtil.yuanToFen(new BigDecimal("0.07")));
        assertEquals(1999, MoneyUtil.yuanToFen(new BigDecimal("19.99")));
        assertEquals(1800, MoneyUtil.yuanToFen(new BigDecimal("18")));
        assertEquals(1800, MoneyUtil.yuanToFen(new BigDecimal("18.00")));
    }
}
