package com.sell.service.impl;

import com.sell.service.PickupNumberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

// 仅当显式开启 redis-pickup profile 时启用; 否则用 PickupNumberLocalImpl (内存)
@Service
@Profile("redis-pickup")
public class PickupNumberServiceImpl implements PickupNumberService {

    private static final String KEY_PREFIX = "pickup_number:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public String generatePickupNumber() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = KEY_PREFIX + today;

        // Redis INCR 原子自增, 天然并发安全
        Long number = redisTemplate.opsForValue().increment(key);

        // 首次创建时设置过期时间(2天, 确保跨零点不丢)
        if (number != null && number == 1) {
            redisTemplate.expire(key, 2, TimeUnit.DAYS);
        }

        // 格式化为3位数: 001, 002, ...
        return String.format("%03d", number);
    }
}
