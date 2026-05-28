package com.sell.utils;

/**
 * 2017-07-09 16:56
 */
public class MathUtil {

    private static final Double MONEY_RANGE = 0.01;

    /**
     * 比较2个金额是否相等
     * @param d1
     * @param d2
     * @return
     */
    public static Boolean equals(Double d1, Double d2) {
        // null 安全: 支付回调金额缺失时返回 false(校验不通过)而非抛 NPE
        if (d1 == null || d2 == null) {
            return false;
        }
        return Math.abs(d1 - d2) < MONEY_RANGE;
    }
}
