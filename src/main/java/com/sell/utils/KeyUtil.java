package com.sell.utils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 2017-06-11 19:12
 */
public class KeyUtil {

    /** 进程内单调自增序列, 保证同一毫秒内连续生成的 key 也不重复. */
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    /**
     * 生成唯一的主键.
     * 格式: 13位时间戳 + 6位自增序列 (共19位数字字符串).
     * 原实现用随机数, 同毫秒高并发下存在生日碰撞风险; 改用自增序列后单实例内不会碰撞.
     * @return 唯一字符串主键
     */
    public static String genUniqueKey() {
        int n = (SEQ.getAndIncrement() & 0x7fffffff) % 1_000_000;
        return System.currentTimeMillis() + String.format("%06d", n);
    }
}
