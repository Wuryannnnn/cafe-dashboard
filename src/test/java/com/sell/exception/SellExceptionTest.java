package com.sell.exception;

import com.sell.enums.ResultEnum;
import org.junit.Test;

import static org.junit.Assert.*;

public class SellExceptionTest {

    @Test
    public void constructor_withResultEnum() {
        SellException e = new SellException(ResultEnum.PRODUCT_NOT_EXIST);
        assertEquals(Integer.valueOf(10), e.getCode());
        assertEquals("商品不存在", e.getMessage());
    }

    @Test
    public void constructor_withCodeAndMessage() {
        SellException e = new SellException(99, "自定义错误");
        assertEquals(Integer.valueOf(99), e.getCode());
        assertEquals("自定义错误", e.getMessage());
    }

    @Test
    public void allResultEnums_canCreateException() {
        for (ResultEnum r : ResultEnum.values()) {
            SellException e = new SellException(r);
            assertEquals(r.getCode(), e.getCode());
            assertEquals(r.getMessage(), e.getMessage());
        }
    }
}
