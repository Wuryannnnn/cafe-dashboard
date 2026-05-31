package com.sell.service.impl;

import com.sell.repository.OrderMasterRepository;
import com.sell.service.PickupNumberService;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 取餐号生成 (不依赖 Redis, 本项目所有 profile 都禁用了 Redis).
 * 内存计数器, 但每天首次取号(含进程重启后)会从数据库恢复当日已用的最大号,
 * 避免重启后从 001 重新开始导致同一天重号. (单实例部署足够; 多实例需改用 DB/Redis 原子序列.)
 */
@Service
@Profile({ "local", "prod" })
@Primary
public class PickupNumberLocalImpl implements PickupNumberService {

    private final OrderMasterRepository orderMasterRepository;

    private LocalDate currentDate = null;
    private final AtomicInteger counter = new AtomicInteger(0);

    public PickupNumberLocalImpl(OrderMasterRepository orderMasterRepository) {
        this.orderMasterRepository = orderMasterRepository;
    }

    @Override
    public synchronized String generatePickupNumber() {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDate)) {
            // 跨天 或 进程重启后的首次取号: 从 DB 恢复当日最大号, 接着往后发
            currentDate = today;
            counter.set(maxPickupNumberFromDb(today));
        }
        return String.format("%03d", counter.incrementAndGet());
    }

    /** 查询当日已用的最大取餐号; DB 异常时退回当前计数, 不阻塞下单. */
    private int maxPickupNumberFromDb(LocalDate day) {
        try {
            Date start = Date.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date end = Date.from(day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            int max = 0;
            for (String pn : orderMasterRepository.findPickupNumbersByDateRange(start, end)) {
                if (pn == null) continue;
                try {
                    max = Math.max(max, Integer.parseInt(pn.trim()));
                } catch (NumberFormatException ignore) {
                    // 非数字取餐号(历史脏数据)跳过
                }
            }
            return max;
        } catch (Exception e) {
            return counter.get();
        }
    }
}
