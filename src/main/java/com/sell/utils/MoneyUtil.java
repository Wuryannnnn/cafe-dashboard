package com.sell.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 元 ↔ 分 换算, 全程整数, 不经 double (避免 0.1/0.07 等浮点误差). */
public final class MoneyUtil {

    private MoneyUtil() {}

    /** 元(BigDecimal) → 分(int), 四舍五入. */
    public static int yuanToFen(BigDecimal yuan) {
        return yuan.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }
}
