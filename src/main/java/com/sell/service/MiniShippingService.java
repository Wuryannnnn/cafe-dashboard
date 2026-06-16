package com.sell.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sell.dataobject.PendingShipping;
import com.sell.repository.PendingShippingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 微信小程序「订单发货管理」上报 (交易类小程序保支付权限的硬要求).
 * 仅对【经小程序 APIv3 支付成功】的订单(有 transactionId)上报虚拟发货/服务完成;
 * 现金/收银台手动单/历史单不报. 失败入 pending_shipping 重试队列(独立于 WMS 队列).
 */
@Service
@Slf4j
public class MiniShippingService {

    private static final int[] BACKOFF_SECONDS = {60, 300, 900, 1800, 3600};
    private static final int MAX_ATTEMPTS = 10;

    @Autowired private WxMiniAccessTokenService accessTokenService;
    @Autowired private PendingShippingRepository repo;

    private final RestTemplate rest = new RestTemplate();

    /** 支付成功后异步触发 (不阻塞回调). */
    @Async
    public void reportAsync(String orderId, String openid, String transactionId) {
        report(orderId, openid, transactionId);
    }

    /** 同步上报核心: 无 transactionId / 未配置 → 跳过(不入队); 上报失败 → 入队列重试. */
    public void report(String orderId, String openid, String transactionId) {
        if (transactionId == null || transactionId.isEmpty()) {
            return;
        }
        if (accessTokenService.getAccessToken() == null) {
            log.warn("[发货上报] 未配置小程序凭据, 跳过 orderId={}", orderId);
            return;
        }
        if (!doReport(orderId, openid, transactionId)) {
            enqueue(orderId, openid, transactionId, "首次上报失败");
        }
    }

    private boolean doReport(String orderId, String openid, String transactionId) {
        String token = accessTokenService.getAccessToken();
        if (token == null) return false;
        try {
            JsonObject body = new JsonObject();
            JsonObject orderKey = new JsonObject();
            orderKey.addProperty("order_number_type", 2); // 2=微信支付单号
            orderKey.addProperty("transaction_id", transactionId);
            body.add("order_key", orderKey);
            body.addProperty("logistics_type", 4); // 4=虚拟商品/服务, 用户无需查看物流
            body.addProperty("delivery_mode", 1);   // 1=统一发货
            JsonArray shippingList = new JsonArray();
            JsonObject item = new JsonObject();
            item.addProperty("item_desc", "餐饮订单");
            shippingList.add(item);
            body.add("shipping_list", shippingList);
            body.addProperty("upload_time", rfc3339Now());
            JsonObject payer = new JsonObject();
            payer.addProperty("openid", openid);
            body.add("payer", payer);

            String url = "https://api.weixin.qq.com/wxa/sec/order/upload_shipping_info?access_token=" + token;
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            String resp = rest.postForObject(url, new HttpEntity<>(body.toString(), h), String.class);
            JsonObject o = JsonParser.parseString(resp).getAsJsonObject();
            int errcode = o.has("errcode") ? o.get("errcode").getAsInt() : -1;
            if (errcode == 0) {
                log.info("[发货上报] 成功 orderId={}", orderId);
                return true;
            }
            log.warn("[发货上报] 失败 orderId={}: {}", orderId, resp);
            return false;
        } catch (Exception e) {
            log.warn("[发货上报] 异常 orderId={}: {}", orderId, e.getMessage());
            return false;
        }
    }

    @Transactional
    public void enqueue(String orderId, String openid, String transactionId, String reason) {
        PendingShipping row = new PendingShipping();
        row.setOrderId(orderId);
        row.setOpenid(openid);
        row.setTransactionId(transactionId);
        row.setStatus("pending");
        row.setAttempts(0);
        row.setLastError(reason);
        row.setCreateTime(new Date());
        row.setNextRetryAt(new Date(System.currentTimeMillis() + BACKOFF_SECONDS[0] * 1000L));
        repo.save(row);
    }

    /** 每 60s 扫一次重试队列. */
    @Scheduled(fixedDelay = 60_000L, initialDelay = 45_000L)
    public void retryPending() {
        if (accessTokenService.getAccessToken() == null) return;
        List<PendingShipping> list = repo.findByStatusAndNextRetryAtLessThanEqual("pending", new Date());
        for (PendingShipping row : list) {
            try {
                processOne(row);
            } catch (Exception e) {
                log.error("[发货上报] 重试 #{} 异常: {}", row.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    public void processOne(PendingShipping row) {
        boolean ok = doReport(row.getOrderId(), row.getOpenid(), row.getTransactionId());
        row.setAttempts(row.getAttempts() + 1);
        row.setUpdateTime(new Date());
        if (ok) {
            row.setStatus("done");
            row.setLastError(null);
        } else if (row.getAttempts() >= MAX_ATTEMPTS) {
            row.setStatus("failed");
            row.setLastError("超过最大重试次数");
        } else {
            int idx = Math.min(row.getAttempts() - 1, BACKOFF_SECONDS.length - 1);
            row.setNextRetryAt(new Date(System.currentTimeMillis() + BACKOFF_SECONDS[idx] * 1000L));
        }
        repo.save(row);
    }

    private String rfc3339Now() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date());
    }
}
