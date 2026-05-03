package com.sell.utils;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class JsonUtilTest {

    @Test
    public void toJson_map() {
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        String json = JsonUtil.toJson(map);
        assertNotNull(json);
        assertTrue(json.contains("key"));
        assertTrue(json.contains("value"));
    }

    @Test
    public void toJson_null() {
        String json = JsonUtil.toJson(null);
        assertEquals("null", json);
    }

    @Test
    public void toJson_string() {
        String json = JsonUtil.toJson("hello");
        assertEquals("\"hello\"", json);
    }
}
