package com.sell.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sell.dataobject.ShopConfig;
import com.sell.repository.ShopConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * WMS 系统 HTTP 客户端
 * 配置 (ShopConfig):
 *   wms.baseUrl       — 后端地址, 如 http://localhost:8081
 *   wms.username      — 登录用户名 (默认 admin)
 *   wms.password      — 登录密码 (默认 admin123)
 *   wms.token         — 手动覆盖的 token (可选, 留空则自动登录获取)
 *   wms.defaultWarehouseId — 出库时使用的仓库 id
 *
 * 自动登录: 首次或 401 时调 /login 获取新 token, 缓存在内存 + 写回 ShopConfig
 * 安全降级: 任何调用失败都返回空 list / false, 不影响咖啡店主流程
 */
@Service
@Slf4j
public class WmsClient {

    public static final String CFG_BASE_URL = "wms.baseUrl";
    public static final String CFG_USERNAME = "wms.username";
    public static final String CFG_PASSWORD = "wms.password";
    public static final String CFG_TOKEN = "wms.token";
    public static final String CFG_DEFAULT_WAREHOUSE = "wms.defaultWarehouseId";

    @Autowired
    private ShopConfigRepository configRepo;

    private final RestTemplate rest = new RestTemplate();

    private volatile String cachedToken;

    public boolean isConfigured() {
        return getBaseUrl() != null && !getBaseUrl().isEmpty();
    }

    public String getBaseUrl() { return readConfig(CFG_BASE_URL); }
    public String getUsername() {
        String v = readConfig(CFG_USERNAME);
        return (v == null || v.isEmpty()) ? "admin" : v;
    }
    public String getPassword() {
        String v = readConfig(CFG_PASSWORD);
        return (v == null || v.isEmpty()) ? "admin123" : v;
    }
    public String getToken() {
        if (cachedToken != null && !cachedToken.isEmpty()) return cachedToken;
        String t = readConfig(CFG_TOKEN);
        if (t != null && !t.isEmpty()) cachedToken = t;
        return cachedToken;
    }
    public String getDefaultWarehouseId() { return readConfig(CFG_DEFAULT_WAREHOUSE); }

    /** 强制登录, 写入 cachedToken + ShopConfig. */
    public synchronized String login() {
        if (!isConfigured()) return null;
        try {
            JsonObject body = new JsonObject();
            body.addProperty("username", getUsername());
            body.addProperty("password", getPassword());
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> resp = rest.exchange(
                    getBaseUrl() + "/login", HttpMethod.POST,
                    new HttpEntity<>(body.toString(), h), String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonObject root = JsonParser.parseString(resp.getBody()).getAsJsonObject();
                JsonElement data = root.get("data");
                if (data != null && data.isJsonObject()) {
                    JsonElement tokenEl = data.getAsJsonObject().get("token");
                    if (tokenEl == null) tokenEl = data.getAsJsonObject().get("access_token");
                    if (tokenEl != null) {
                        cachedToken = tokenEl.getAsString();
                        saveConfig(CFG_TOKEN, cachedToken);
                        log.info("[WMS] login OK, token cached");
                        return cachedToken;
                    }
                }
            }
            log.warn("[WMS] login response missing token: {}", resp.getBody());
        } catch (Exception e) {
            log.warn("[WMS] login failed: {}", e.getMessage());
        }
        return null;
    }

    public List<Map<String, Object>> listItems() {
        return parseDataAsList(safeGet("/wms/item/listNoPage"));
    }

    public List<Map<String, Object>> lowStockAlerts() {
        try {
            List<Map<String, Object>> all = parseDataAsList(safeGet("/wms/inventory/listNoPage"));
            List<Map<String, Object>> alerts = new ArrayList<>();
            for (Map<String, Object> inv : all) {
                Object qty = inv.get("quantity");
                Object safe = inv.get("safeQuantity");
                if (qty instanceof Number && safe instanceof Number
                        && ((Number) qty).doubleValue() <= ((Number) safe).doubleValue()
                        && ((Number) safe).doubleValue() > 0) {
                    alerts.add(inv);
                }
            }
            return alerts;
        } catch (Exception e) {
            log.warn("[WMS] lowStockAlerts failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> listWarehouses() {
        return parseDataAsList(safeGet("/wms/warehouse/listNoPage"));
    }

    /** 全量库存 (返回 itemId/skuId/quantity 等). */
    public List<Map<String, Object>> listInventory() {
        return parseDataAsList(safeGet("/wms/inventory/listNoPage"));
    }

    /** 取指定 itemId 的当前库存 (合计所有 sku). */
    public java.math.BigDecimal getItemTotalStock(Long itemId) {
        if (itemId == null) return java.math.BigDecimal.ZERO;
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (Map<String, Object> inv : listInventory()) {
            Object id = inv.get("itemId");
            if (id == null) continue;
            if (itemId.toString().equals(id.toString())) {
                Object qty = inv.get("quantity");
                if (qty instanceof Number) total = total.add(new java.math.BigDecimal(qty.toString()));
            }
        }
        return total;
    }

    /** 取最近出库记录 (近 N 条). */
    public List<Map<String, Object>> recentShipments(int limit) {
        try {
            String body = safeGet("/wms/shipmentOrder/list?pageNum=1&pageSize=" + limit);
            return parseDataAsList(body);
        } catch (Exception e) { return Collections.emptyList(); }
    }

    /** 取最近入库记录 (近 N 条). */
    public List<Map<String, Object>> recentReceipts(int limit) {
        try {
            String body = safeGet("/wms/receiptOrder/list?pageNum=1&pageSize=" + limit);
            return parseDataAsList(body);
        } catch (Exception e) { return Collections.emptyList(); }
    }

    /** 创建一个物料 (默认带一个 SKU). 已存在 itemCode 则跳过. */
    public boolean createItem(String name, String code, String category, String unit) {
        if (!isConfigured()) return false;
        try {
            JsonObject body = new JsonObject();
            body.addProperty("itemName", name);
            body.addProperty("itemCode", code);
            body.addProperty("itemCategory", category);
            body.addProperty("unit", unit);
            JsonArray sku = new JsonArray();
            JsonObject defaultSku = new JsonObject();
            defaultSku.addProperty("skuName", "默认");
            defaultSku.addProperty("skuCode", code + "-DEFAULT");
            defaultSku.addProperty("itemId", 0);
            sku.add(defaultSku);
            body.add("sku", sku);
            String resp = safePost("/wms/item", body.toString());
            return resp != null && resp.contains("\"code\":200");
        } catch (Exception e) {
            log.warn("[WMS] createItem({}) failed: {}", name, e.getMessage());
            return false;
        }
    }

    /** 出库结果 —— 区分「库存不足 (业务失败)」「网络/服务端错误 (可重试)」「成功」. */
    public enum ShipmentStatus { OK, OUT_OF_STOCK, RETRYABLE, SKIPPED }

    public static class ShipmentResult {
        public final ShipmentStatus status;
        public final String message;
        public ShipmentResult(ShipmentStatus s, String m) { this.status = s; this.message = m; }
        public boolean isOk() { return status == ShipmentStatus.OK; }
        public boolean shouldRetry() { return status == ShipmentStatus.RETRYABLE; }
        public boolean outOfStock() { return status == ShipmentStatus.OUT_OF_STOCK; }
    }

    /** 创建出库单 (订单完成时). 返回结构化结果, 调用方自行决定是否回滚/重试. */
    public ShipmentResult createShipment(String bizOrderNo, List<Map<String, Object>> details) {
        if (!isConfigured()) return new ShipmentResult(ShipmentStatus.RETRYABLE, "WMS 未配置");
        if (details == null || details.isEmpty()) return new ShipmentResult(ShipmentStatus.SKIPPED, "无 BOM 配方");
        String warehouseId = getDefaultWarehouseId();
        if (warehouseId == null || warehouseId.isEmpty()) {
            return new ShipmentResult(ShipmentStatus.RETRYABLE, "defaultWarehouseId 未配置");
        }
        JsonObject body = new JsonObject();
        // WMS 必填 orderNo (BaseOrderBo 的校验); bizOrderNo 是业务关联号, 同值即可
        body.addProperty("orderNo", bizOrderNo);
        body.addProperty("bizOrderNo", bizOrderNo);
        body.addProperty("warehouseId", Long.parseLong(warehouseId));
        body.addProperty("optType", 1L);
        body.add("details", buildDetails(details));
        // WMS 直接出库 endpoint, 跳过待审核, 一步扣库存
        final String SHIPMENT_PATH = "/wms/shipmentOrder/shipment";
        try {
            String resp = doPut(SHIPMENT_PATH, body.toString());
            // WMS 业务错误是 HTTP 200 + body code != 200, 必须解析 body 才能识别库存不足
            return mapBodyResult(bizOrderNo, resp);
        } catch (HttpClientErrorException.Unauthorized e) {
            cachedToken = null;
            if (login() != null) {
                try {
                    String resp = doPut(SHIPMENT_PATH, body.toString());
                    return mapBodyResult(bizOrderNo, resp);
                } catch (HttpClientErrorException ex) {
                    return mapHttpError(bizOrderNo, ex);
                } catch (Exception ex) {
                    log.warn("[WMS] shipment retry after relogin failed {}: {}", bizOrderNo, ex.getMessage());
                    return new ShipmentResult(ShipmentStatus.RETRYABLE, ex.getMessage());
                }
            }
            return new ShipmentResult(ShipmentStatus.RETRYABLE, "WMS 登录失败");
        } catch (HttpClientErrorException ex) {
            return mapHttpError(bizOrderNo, ex);
        } catch (Exception e) {
            log.warn("[WMS] shipment failed {}: {}", bizOrderNo, e.getMessage());
            return new ShipmentResult(ShipmentStatus.RETRYABLE, e.getMessage());
        }
    }

    /** WMS 用 HTTP 200 + body.code 表达业务结果, 解析 code: 200 OK / 409 缺料 / 其它可重试. */
    private ShipmentResult mapBodyResult(String bizOrderNo, String body) {
        if (body == null || body.isEmpty()) {
            return new ShipmentResult(ShipmentStatus.RETRYABLE, "WMS 空响应");
        }
        try {
            com.google.gson.JsonObject o = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
            int code = o.has("code") && !o.get("code").isJsonNull() ? o.get("code").getAsInt() : -1;
            if (code == 200) {
                log.info("[WMS] shipment created for order {}", bizOrderNo);
                return new ShipmentResult(ShipmentStatus.OK, null);
            }
            String msg = extractMessage(body);
            if (code == 409) {
                log.info("[WMS] out-of-stock for {}: {}", bizOrderNo, msg);
                return new ShipmentResult(ShipmentStatus.OUT_OF_STOCK, msg);
            }
            log.warn("[WMS] shipment biz error code={} for {}: {}", code, bizOrderNo, msg);
            return new ShipmentResult(ShipmentStatus.RETRYABLE, msg);
        } catch (Exception e) {
            log.warn("[WMS] parse shipment resp failed for {}: {}", bizOrderNo, e.getMessage());
            return new ShipmentResult(ShipmentStatus.RETRYABLE, "WMS 响应解析失败");
        }
    }

    /** 把 HTTP 错误映射到业务语义: 409 = 库存不足 (不重试), 其它 4xx/5xx = 可重试. */
    private ShipmentResult mapHttpError(String bizOrderNo, HttpClientErrorException ex) {
        if (ex.getStatusCode().value() == 409) {
            String msg = extractMessage(ex.getResponseBodyAsString());
            log.info("[WMS] out-of-stock for {}: {}", bizOrderNo, msg);
            return new ShipmentResult(ShipmentStatus.OUT_OF_STOCK, msg);
        }
        log.warn("[WMS] shipment HTTP error {} for {}: {}", ex.getStatusCode(), bizOrderNo, ex.getResponseBodyAsString());
        return new ShipmentResult(ShipmentStatus.RETRYABLE, "WMS 返回 " + ex.getStatusCode());
    }

    private String extractMessage(String body) {
        if (body == null) return "库存不足";
        try {
            com.google.gson.JsonElement el = com.google.gson.JsonParser.parseString(body);
            if (el.isJsonObject()) {
                com.google.gson.JsonObject o = el.getAsJsonObject();
                if (o.has("msg") && !o.get("msg").isJsonNull()) return o.get("msg").getAsString();
                if (o.has("message") && !o.get("message").isJsonNull()) return o.get("message").getAsString();
            }
        } catch (Exception ignore) {}
        return body.length() > 200 ? body.substring(0, 200) : body;
    }

    /** 创建入库单 (退款/取消时退还原料). */
    public boolean createReceipt(String bizOrderNo, List<Map<String, Object>> details) {
        if (!isConfigured() || details == null || details.isEmpty()) return false;
        String warehouseId = getDefaultWarehouseId();
        if (warehouseId == null || warehouseId.isEmpty()) return false;
        try {
            JsonObject body = new JsonObject();
            String orderNo = "REFUND-" + bizOrderNo;
            body.addProperty("orderNo", orderNo);
            body.addProperty("bizOrderNo", orderNo);
            body.addProperty("warehouseId", Long.parseLong(warehouseId));
            body.addProperty("optType", 3L);
            body.add("details", buildDetails(details));
            // 直接入库 endpoint, 一步把库存加回去
            String resp = safePost("/wms/receiptOrder/warehousing", body.toString());
            log.info("[WMS] receipt (refund) created for order {}", bizOrderNo);
            return resp != null;
        } catch (Exception e) {
            log.warn("[WMS] createReceipt failed for order {}: {}", bizOrderNo, e.getMessage());
            return false;
        }
    }

    private JsonArray buildDetails(List<Map<String, Object>> details) {
        // WMS 的 InventoryService.subtract 按 detail.warehouseId 查库存行,
        // body 顶层 warehouseId 不会自动透传, 必须在每条 detail 上重复填.
        String warehouseId = getDefaultWarehouseId();
        JsonArray arr = new JsonArray();
        for (Map<String, Object> d : details) {
            JsonObject o = new JsonObject();
            // Number → long → string, 避免重试反序列化时 Long 被解成 Double 又 toString 出 "2.045E18"
            if (d.get("itemId") != null) o.addProperty("itemId", longLikeToString(d.get("itemId")));
            if (d.get("skuId") != null) o.addProperty("skuId", longLikeToString(d.get("skuId")));
            if (d.get("quantity") != null) o.addProperty("quantity", d.get("quantity").toString());
            if (warehouseId != null && !warehouseId.isEmpty()) {
                o.addProperty("warehouseId", warehouseId);
            }
            arr.add(o);
        }
        return arr;
    }

    private static String longLikeToString(Object v) {
        if (v instanceof Number) return Long.toString(((Number) v).longValue());
        return v.toString();
    }

    /* ---------- HTTP ---------- */

    private String safeGet(String path) {
        try {
            return doGet(path);
        } catch (HttpClientErrorException.Unauthorized e) {
            cachedToken = null;
            if (login() != null) {
                try { return doGet(path); } catch (Exception ex) {
                    log.warn("[WMS] retry GET {} failed: {}", path, ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("[WMS] GET {} failed: {}", path, e.getMessage());
        }
        return null;
    }

    private String safePost(String path, String json) {
        try {
            return doPost(path, json);
        } catch (HttpClientErrorException.Unauthorized e) {
            cachedToken = null;
            if (login() != null) {
                try { return doPost(path, json); } catch (Exception ex) {
                    log.warn("[WMS] retry POST {} failed: {}", path, ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("[WMS] POST {} failed: {}", path, e.getMessage());
        }
        return null;
    }

    private String doGet(String path) {
        ResponseEntity<String> resp = rest.exchange(
                getBaseUrl() + path, HttpMethod.GET, new HttpEntity<>(headers()), String.class);
        return resp.getBody();
    }

    private String doPost(String path, String json) {
        HttpHeaders h = headers();
        h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.exchange(
                getBaseUrl() + path, HttpMethod.POST, new HttpEntity<>(json, h), String.class);
        return resp.getBody();
    }

    private String doPut(String path, String json) {
        HttpHeaders h = headers();
        h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.exchange(
                getBaseUrl() + path, HttpMethod.PUT, new HttpEntity<>(json, h), String.class);
        return resp.getBody();
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        String token = getToken();
        if ((token == null || token.isEmpty()) && isConfigured()) {
            login();
            token = getToken();
        }
        if (token != null && !token.isEmpty()) {
            h.set("Authorization", "Bearer " + token);
        }
        h.set("Accept", "application/json");
        return h;
    }

    private List<Map<String, Object>> parseDataAsList(String body) {
        if (body == null || body.isEmpty()) return Collections.emptyList();
        try {
            JsonElement root = JsonParser.parseString(body);
            if (!root.isJsonObject()) return Collections.emptyList();
            JsonElement data = root.getAsJsonObject().get("data");
            if (data == null || !data.isJsonArray()) {
                JsonElement rows = root.getAsJsonObject().get("rows");
                if (rows != null && rows.isJsonArray()) data = rows;
                else return Collections.emptyList();
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonElement el : data.getAsJsonArray()) {
                if (el.isJsonObject()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    el.getAsJsonObject().entrySet().forEach(e -> {
                        JsonElement v = e.getValue();
                        if (v.isJsonNull()) m.put(e.getKey(), null);
                        else if (v.isJsonPrimitive()) {
                            if (v.getAsJsonPrimitive().isNumber()) m.put(e.getKey(), v.getAsBigDecimal());
                            else if (v.getAsJsonPrimitive().isBoolean()) m.put(e.getKey(), v.getAsBoolean());
                            else m.put(e.getKey(), v.getAsString());
                        } else {
                            m.put(e.getKey(), v.toString());
                        }
                    });
                    result.add(m);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("[WMS] parse failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String readConfig(String key) {
        ShopConfig c = configRepo.findById(key).orElse(null);
        return c == null ? null : c.getConfigValue();
    }

    private void saveConfig(String key, String value) {
        ShopConfig c = configRepo.findById(key).orElse(new ShopConfig());
        c.setConfigKey(key);
        c.setConfigValue(value);
        configRepo.save(c);
    }
}
