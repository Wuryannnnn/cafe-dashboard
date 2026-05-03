package com.sell.service;

import com.google.gson.Gson;
import com.sell.dataobject.PendingWmsShipment;
import com.sell.repository.PendingWmsShipmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * WMS 出库补偿队列.
 * - 下单时若 WMS 不可达 / 超时, 调用 enqueue() 保存到 DB
 * - 定时任务每分钟扫 pending 条目, 按指数退避重试 (1min → 5min → 15min → 30min → 1h 封顶)
 * - WMS 侧 shipment() 已做幂等, 同 orderId 重复调用不会多扣
 */
@Slf4j
@Service
public class WmsRetryService {

    // 退避策略: 第 N 次失败后等待多久 (秒)
    private static final int[] BACKOFF_SECONDS = { 60, 300, 900, 1800, 3600 };
    // 超过此次数判为不可恢复, 标记 failed
    private static final int MAX_ATTEMPTS = 10;

    private static final Gson GSON = new Gson();
    private static final Type DETAILS_TYPE =
            new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>(){}.getType();

    @Autowired private PendingWmsShipmentRepository repo;
    @Autowired private WmsClient wmsClient;

    /** 把下单时同步失败的 WMS 出库请求写入队列. */
    @Transactional
    public void enqueue(String orderId, List<Map<String, Object>> details, String reason) {
        PendingWmsShipment row = new PendingWmsShipment();
        row.setOrderId(orderId);
        row.setDetailsJson(GSON.toJson(details));
        row.setStatus("pending");
        row.setAttempts(0);
        row.setLastError(reason);
        row.setCreateTime(new Date());
        row.setNextRetryAt(new Date(System.currentTimeMillis() + BACKOFF_SECONDS[0] * 1000L));
        repo.save(row);
    }

    /** 每 60s 扫一次队列. */
    @Scheduled(fixedDelay = 60_000L, initialDelay = 30_000L)
    public void retryPending() {
        if (!wmsClient.isConfigured()) return;
        List<PendingWmsShipment> list = repo.findByStatusAndNextRetryAtLessThanEqual("pending", new Date());
        if (list.isEmpty()) return;
        log.info("[WMS retry] 处理 {} 条 pending", list.size());
        for (PendingWmsShipment row : list) {
            try {
                processOne(row);
            } catch (Exception e) {
                log.error("[WMS retry] 处理 #{} 异常: {}", row.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    public void processOne(PendingWmsShipment row) {
        List<Map<String, Object>> details;
        try {
            details = GSON.fromJson(row.getDetailsJson(), DETAILS_TYPE);
        } catch (Exception e) {
            row.setStatus("failed");
            row.setLastError("detailsJson 解析失败: " + e.getMessage());
            row.setUpdateTime(new Date());
            repo.save(row);
            return;
        }
        if (details == null) details = new ArrayList<>();

        WmsClient.ShipmentResult r = wmsClient.createShipment(row.getOrderId(), details);
        row.setAttempts(row.getAttempts() + 1);
        row.setUpdateTime(new Date());

        if (r.isOk() || r.status == WmsClient.ShipmentStatus.SKIPPED) {
            row.setStatus("done");
            row.setLastError(null);
            log.info("[WMS retry] order {} 补偿成功 (第 {} 次)", row.getOrderId(), row.getAttempts());
        } else if (r.outOfStock()) {
            // 重试时才发现库存不足 —— 比如人工补货前又被别的单消耗掉
            // 订单已成交, 无法回滚, 只能标记失败让人工处理
            row.setStatus("failed");
            row.setLastError("库存不足 (重试阶段): " + r.message);
            log.warn("[WMS retry] order {} 重试时发现库存不足, 需人工介入", row.getOrderId());
        } else {
            row.setLastError(r.message);
            if (row.getAttempts() >= MAX_ATTEMPTS) {
                row.setStatus("failed");
                log.error("[WMS retry] order {} 超过 {} 次失败, 标记 failed", row.getOrderId(), MAX_ATTEMPTS);
            } else {
                int idx = Math.min(row.getAttempts() - 1, BACKOFF_SECONDS.length - 1);
                row.setNextRetryAt(new Date(System.currentTimeMillis() + BACKOFF_SECONDS[idx] * 1000L));
            }
        }
        repo.save(row);
    }

    /** 手动触发某条的重试 (给看板用). */
    @Transactional
    public boolean retryNow(Long id) {
        PendingWmsShipment row = repo.findById(id).orElse(null);
        if (row == null) return false;
        row.setNextRetryAt(new Date());
        row.setStatus("pending");
        repo.save(row);
        processOne(row);
        return true;
    }
}
