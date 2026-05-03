package com.sell.utils;

import com.sell.VO.ResultVO;
import org.junit.Test;

import static org.junit.Assert.*;

public class ResultVOUtilTest {

    @Test
    public void success_withData() {
        ResultVO result = ResultVOUtil.success("test data");
        assertEquals(Integer.valueOf(0), result.getCode());
        assertEquals("成功", result.getMsg());
        assertEquals("test data", result.getData());
    }

    @Test
    public void success_noData() {
        ResultVO result = ResultVOUtil.success();
        assertEquals(Integer.valueOf(0), result.getCode());
        assertEquals("成功", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    public void error() {
        ResultVO result = ResultVOUtil.error(1, "参数错误");
        assertEquals(Integer.valueOf(1), result.getCode());
        assertEquals("参数错误", result.getMsg());
    }
}
