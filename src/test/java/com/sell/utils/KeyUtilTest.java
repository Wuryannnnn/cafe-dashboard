package com.sell.utils;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class KeyUtilTest {

    @Test
    public void genUniqueKey_notNull() {
        String key = KeyUtil.genUniqueKey();
        assertNotNull(key);
        assertFalse(key.isEmpty());
    }

    @Test
    public void genUniqueKey_lengthCorrect() {
        String key = KeyUtil.genUniqueKey();
        // 13位时间戳 + 6位随机数 = 19位
        assertEquals(19, key.length());
    }

    @Test
    public void genUniqueKey_uniqueness() {
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            keys.add(KeyUtil.genUniqueKey());
        }
        // 100次生成应全部唯一
        assertEquals(100, keys.size());
    }
}
