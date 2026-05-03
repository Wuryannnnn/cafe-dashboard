package com.sell.service.impl;

import com.sell.service.PickupNumberService;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 本地开发用: 内存计数器生成取餐号, 不依赖Redis
 */
@Service
@Profile({ "local", "prod" })
@Primary
public class PickupNumberLocalImpl implements PickupNumberService {

    private LocalDate currentDate = LocalDate.now();
    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public synchronized String generatePickupNumber() {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDate)) {
            currentDate = today;
            counter.set(0);
        }
        return String.format("%03d", counter.incrementAndGet());
    }
}
