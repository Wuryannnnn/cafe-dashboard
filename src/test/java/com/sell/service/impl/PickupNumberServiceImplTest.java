package com.sell.service.impl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PickupNumberServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PickupNumberServiceImpl pickupNumberService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    public void generatePickupNumber_firstOfDay() {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);

        String number = pickupNumberService.generatePickupNumber();

        assertEquals("001", number);
        verify(redisTemplate).expire(anyString(), eq(2L), any());
    }

    @Test
    public void generatePickupNumber_subsequent() {
        when(valueOperations.increment(anyString())).thenReturn(42L);

        String number = pickupNumberService.generatePickupNumber();

        assertEquals("042", number);
        // 非首次不应设置expire
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any());
    }

    @Test
    public void generatePickupNumber_threeDigitFormat() {
        when(valueOperations.increment(anyString())).thenReturn(5L);
        assertEquals("005", pickupNumberService.generatePickupNumber());

        when(valueOperations.increment(anyString())).thenReturn(99L);
        assertEquals("099", pickupNumberService.generatePickupNumber());

        when(valueOperations.increment(anyString())).thenReturn(100L);
        assertEquals("100", pickupNumberService.generatePickupNumber());

        when(valueOperations.increment(anyString())).thenReturn(999L);
        assertEquals("999", pickupNumberService.generatePickupNumber());
    }
}
