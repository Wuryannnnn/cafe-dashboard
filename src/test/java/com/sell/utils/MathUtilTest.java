package com.sell.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class MathUtilTest {

    @Test
    public void equals_sameValue() {
        assertTrue(MathUtil.equals(1.0, 1.0));
    }

    @Test
    public void equals_withinTolerance() {
        // 差值在0.01以内应相等(微信支付精度问题 0.10 vs 0.1)
        assertTrue(MathUtil.equals(0.10, 0.1));
        assertTrue(MathUtil.equals(1.005, 1.0));
    }

    @Test
    public void equals_outsideTolerance() {
        assertFalse(MathUtil.equals(1.0, 1.02));
        assertFalse(MathUtil.equals(0.0, 0.02));
    }

    @Test
    public void equals_negativeValues() {
        assertTrue(MathUtil.equals(-1.0, -1.0));
        assertFalse(MathUtil.equals(-1.0, 1.0));
    }
}
