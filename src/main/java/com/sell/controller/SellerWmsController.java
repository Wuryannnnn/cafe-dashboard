package com.sell.controller;

import com.sell.dataobject.PendingWmsShipment;
import com.sell.dataobject.ProductInfo;
import com.sell.dataobject.Recipe;
import com.sell.dataobject.ShopConfig;
import com.sell.repository.PendingWmsShipmentRepository;
import com.sell.repository.ProductInfoRepository;
import com.sell.repository.ShopConfigRepository;
import com.sell.service.RecipeService;
import com.sell.service.WmsClient;
import com.sell.service.WmsRetryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.util.*;

/**
 * WMS 整合 - 设置 / 配方 / 看板
 */
@Controller
@RequestMapping("/seller/wms")
public class SellerWmsController {

    @Autowired private WmsClient wmsClient;
    @Autowired private RecipeService recipeService;
    @Autowired private ProductInfoRepository productRepo;
    @Autowired private ShopConfigRepository configRepo;
    @Autowired private PendingWmsShipmentRepository pendingRepo;
    @Autowired private WmsRetryService wmsRetryService;

    /** 入口页 (转跳到外部 WMS UI). */
    @GetMapping("/")
    public ModelAndView entry(Map<String, Object> map) {
        map.put("baseUrl", wmsClient.getBaseUrl());
        map.put("configured", wmsClient.isConfigured());
        return new ModelAndView("wms/entry", map);
    }

    /** 连接设置. */
    @GetMapping("/settings")
    public ModelAndView settings(Map<String, Object> map) {
        map.put("baseUrl", wmsClient.getBaseUrl());
        map.put("username", wmsClient.getUsername());
        map.put("password", wmsClient.getPassword());
        map.put("token", wmsClient.getToken());
        map.put("warehouseId", wmsClient.getDefaultWarehouseId());
        return new ModelAndView("wms/settings", map);
    }

    @PostMapping("/settings/save")
    public ModelAndView saveSettings(@RequestParam(required = false, defaultValue = "") String baseUrl,
                                     @RequestParam(required = false, defaultValue = "") String username,
                                     @RequestParam(required = false, defaultValue = "") String password,
                                     @RequestParam(required = false, defaultValue = "") String token,
                                     @RequestParam(required = false, defaultValue = "") String warehouseId,
                                     Map<String, Object> map) {
        saveCfg(WmsClient.CFG_BASE_URL, baseUrl.trim());
        saveCfg(WmsClient.CFG_USERNAME, username.trim());
        saveCfg(WmsClient.CFG_PASSWORD, password.trim());
        saveCfg(WmsClient.CFG_TOKEN, token.trim());
        saveCfg(WmsClient.CFG_DEFAULT_WAREHOUSE, warehouseId.trim());
        map.put("url", "/sell/seller/wms/settings");
        return new ModelAndView("common/success", map);
    }

    /** 取 WMS 仓库列表 (设置页下拉用). */
    @GetMapping("/warehouses")
    @ResponseBody
    public List<Map<String, Object>> warehouses() {
        return wmsClient.listWarehouses();
    }

    /** 一键灌入咖啡店常见原料到 WMS. */
    @PostMapping("/seed-materials")
    @ResponseBody
    public Map<String, Object> seedMaterials() {
        Map<String, Object> r = new LinkedHashMap<>();
        if (!wmsClient.isConfigured()) {
            r.put("ok", false); r.put("msg", "未配置 WMS"); return r;
        }
        // [name, code, category, unit]
        String[][] presets = {
                {"咖啡豆-阿拉比卡", "BEAN-ARA",   "原料", "g"},
                {"咖啡豆-罗布斯塔", "BEAN-ROB",   "原料", "g"},
                {"鲜牛奶",        "MILK-FRESH", "原料", "ml"},
                {"燕麦奶",        "MILK-OAT",   "原料", "ml"},
                {"椰奶",          "MILK-COCO",  "原料", "ml"},
                {"豆奶",          "MILK-SOY",   "原料", "ml"},
                {"白砂糖",        "SUGAR-W",    "原料", "g"},
                {"红糖",          "SUGAR-B",    "原料", "g"},
                {"香草糖浆",      "SYRUP-VAN",  "原料", "ml"},
                {"焦糖糖浆",      "SYRUP-CAR",  "原料", "ml"},
                {"榛果糖浆",      "SYRUP-HAZ",  "原料", "ml"},
                {"巧克力酱",      "SAUCE-CHO",  "原料", "g"},
                {"奶油",          "CREAM",      "原料", "g"},
                {"抹茶粉",        "MATCHA",     "原料", "g"},
                {"红茶",          "TEA-BLACK",  "原料", "g"},
                {"绿茶",          "TEA-GREEN",  "原料", "g"},
                {"冰块",          "ICE",        "耗材", "g"},
                {"纸杯-中杯",    "CUP-M",      "耗材", "个"},
                {"纸杯-大杯",    "CUP-L",      "耗材", "个"},
                {"杯盖",          "LID",        "耗材", "个"},
                {"吸管",          "STRAW",      "耗材", "个"},
                {"打包袋",        "BAG",        "耗材", "个"},
        };
        int created = 0, skipped = 0, failed = 0;
        for (String[] p : presets) {
            try {
                if (wmsClient.createItem(p[0], p[1], p[2], p[3])) created++;
                else skipped++;
            } catch (Exception e) {
                failed++;
            }
        }
        r.put("ok", true);
        r.put("total", presets.length);
        r.put("created", created);
        r.put("skipped", skipped);
        r.put("failed", failed);
        return r;
    }

    /** 立即触发一次登录, 用于设置页 "测试登录" 按钮. */
    @PostMapping("/login")
    @ResponseBody
    public Map<String, Object> forceLogin() {
        Map<String, Object> r = new LinkedHashMap<>();
        if (!wmsClient.isConfigured()) {
            r.put("ok", false);
            r.put("msg", "未配置 baseUrl");
            return r;
        }
        String t = wmsClient.login();
        if (t != null && !t.isEmpty()) {
            r.put("ok", true);
            r.put("tokenPrefix", t.substring(0, Math.min(20, t.length())) + "...");
        } else {
            r.put("ok", false);
            r.put("msg", "登录失败, 检查日志或确认 WMS 验证码已关闭 (CAPTCHA_ENABLE=false)");
        }
        return r;
    }

    /** 测试连接. */
    @GetMapping("/test")
    @ResponseBody
    public Map<String, Object> testConnection() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("baseUrl", wmsClient.getBaseUrl());
        r.put("configured", wmsClient.isConfigured());
        if (!wmsClient.isConfigured()) {
            r.put("ok", false);
            r.put("msg", "未配置 baseUrl");
            return r;
        }
        try {
            List<Map<String, Object>> items = wmsClient.listItems();
            r.put("ok", true);
            r.put("itemCount", items.size());
            r.put("sample", items.size() > 0 ? items.get(0) : null);
        } catch (Exception e) {
            r.put("ok", false);
            r.put("msg", e.getMessage());
        }
        return r;
    }

    /** Dashboard widget 数据: 库存预警. */
    @GetMapping("/widget/alerts")
    @ResponseBody
    public Map<String, Object> widgetAlerts() {
        Map<String, Object> r = new HashMap<>();
        if (!wmsClient.isConfigured()) {
            r.put("configured", false);
            r.put("alerts", Collections.emptyList());
            return r;
        }
        r.put("configured", true);
        r.put("alerts", wmsClient.lowStockAlerts());
        return r;
    }

    /** Widget: 库存总览 (物料数 / 低库存数 / 待重试与失败出库数). */
    @GetMapping("/widget/overview")
    @ResponseBody
    public Map<String, Object> widgetOverview() {
        Map<String, Object> r = new LinkedHashMap<>();
        // 待重试 / 彻底失败的出库数是本地数据, 不依赖 WMS 是否在线, 始终返回 → 看板可据此告警
        r.put("pendingCount", pendingRepo.countByStatus("pending"));
        r.put("failedCount", pendingRepo.countByStatus("failed"));
        if (!wmsClient.isConfigured()) {
            r.put("configured", false);
            return r;
        }
        r.put("configured", true);
        List<Map<String, Object>> items = wmsClient.listItems();
        List<Map<String, Object>> alerts = wmsClient.lowStockAlerts();
        r.put("itemCount", items.size());
        r.put("alertCount", alerts.size());
        return r;
    }

    /** 待处理 / 失败的 WMS 出库计数 (看板红点用). */
    @GetMapping("/pending/count")
    @ResponseBody
    public Map<String, Object> pendingCount() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("pending", pendingRepo.countByStatus("pending"));
        r.put("failed", pendingRepo.countByStatus("failed"));
        return r;
    }

    /** 待处理 + 失败的 WMS 出库清单 (供人工对账 / 手动重试; 排除已完成). */
    @GetMapping("/pending/list")
    @ResponseBody
    public List<Map<String, Object>> pendingList() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (PendingWmsShipment row : pendingRepo.findAll()) {
            if ("done".equals(row.getStatus())) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", row.getId());
            m.put("orderId", row.getOrderId());
            m.put("status", row.getStatus());
            m.put("attempts", row.getAttempts());
            m.put("lastError", row.getLastError());
            m.put("nextRetryAt", row.getNextRetryAt());
            m.put("createTime", row.getCreateTime());
            out.add(m);
        }
        return out;
    }

    /** 手动重试一条失败 / 待处理的出库 (看板按钮). */
    @PostMapping("/pending/retry")
    @ResponseBody
    public Map<String, Object> pendingRetry(@RequestParam("id") Long id) {
        Map<String, Object> r = new LinkedHashMap<>();
        if (!wmsClient.isConfigured()) {
            r.put("ok", false);
            r.put("msg", "未配置 WMS, 无法重试");
            return r;
        }
        r.put("ok", wmsRetryService.retryNow(id));
        return r;
    }

    /** Widget: 最近 N 条出库 (物料消耗趋势). */
    @GetMapping("/widget/recent-shipments")
    @ResponseBody
    public Map<String, Object> widgetRecentShipments(@RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> r = new LinkedHashMap<>();
        if (!wmsClient.isConfigured()) {
            r.put("configured", false);
            r.put("items", Collections.emptyList());
            return r;
        }
        r.put("configured", true);
        r.put("items", wmsClient.recentShipments(limit));
        return r;
    }

    /** 物料消耗报表 (页面). */
    @GetMapping("/report/material-cost")
    public ModelAndView materialCostReport(Map<String, Object> map) {
        if (!wmsClient.isConfigured()) {
            map.put("notConfigured", true);
            return new ModelAndView("wms/material_cost", map);
        }
        // 拉最近 50 条出库 + 物料字典
        List<Map<String, Object>> shipments = wmsClient.recentShipments(50);
        Map<String, String> itemNameMap = new HashMap<>();
        for (Map<String, Object> it : wmsClient.listItems()) {
            Object id = it.get("id");
            if (id != null) itemNameMap.put(id.toString(), (String) it.get("itemName"));
        }
        map.put("shipments", shipments);
        map.put("itemNameMap", itemNameMap);
        map.put("notConfigured", false);
        return new ModelAndView("wms/material_cost", map);
    }

    /* ---------- 配方 (BOM) 管理 ---------- */

    @Autowired
    private com.sell.repository.ProductSkuRepository productSkuRepo;

    @GetMapping("/recipe/list")
    public ModelAndView recipeList(Map<String, Object> map) {
        List<ProductInfo> products = productRepo.findAll();
        Map<String, List<Recipe>> recipesByProduct = new HashMap<>();
        for (Recipe r : recipeService.findAll()) {
            recipesByProduct.computeIfAbsent(r.getProductId(), k -> new ArrayList<>()).add(r);
        }
        // 每个商品的 SKU 列表 (用于按规格区分配方)
        Map<String, List<com.sell.dataobject.ProductSku>> skuByProduct = new HashMap<>();
        if (!products.isEmpty()) {
            List<String> ids = products.stream().map(ProductInfo::getProductId).toList();
            for (com.sell.dataobject.ProductSku s : productSkuRepo.findByProductIdIn(ids)) {
                skuByProduct.computeIfAbsent(s.getProductId(), k -> new ArrayList<>()).add(s);
            }
        }
        map.put("products", products);
        map.put("recipesByProduct", recipesByProduct);
        map.put("skuByProduct", skuByProduct);
        map.put("wmsItems", wmsClient.listItems());
        return new ModelAndView("wms/recipe_list", map);
    }

    /** 复制配方: 把 fromProductId 的所有配方复制到 toProductId. */
    @PostMapping("/recipe/copy")
    public ModelAndView recipeCopy(@RequestParam("fromProductId") String fromProductId,
                                   @RequestParam("toProductId") String toProductId,
                                   @RequestParam(value = "overwrite", defaultValue = "false") Boolean overwrite,
                                   Map<String, Object> map) {
        if (fromProductId.equals(toProductId)) {
            map.put("msg", "源和目标菜品相同");
            map.put("url", "/sell/seller/wms/recipe/list");
            return new ModelAndView("common/error", map);
        }
        // 删原配方 (overwrite 模式)
        if (overwrite) {
            for (Recipe r : recipeService.findByProduct(toProductId)) {
                recipeService.delete(r.getRecipeId());
            }
        }
        int count = 0;
        for (Recipe src : recipeService.findByProduct(fromProductId)) {
            Recipe copy = new Recipe();
            copy.setProductId(toProductId);
            copy.setSkuId(src.getSkuId());
            copy.setWmsItemId(src.getWmsItemId());
            copy.setWmsItemName(src.getWmsItemName());
            copy.setWmsSkuId(src.getWmsSkuId());
            copy.setQuantity(src.getQuantity());
            copy.setUnit(src.getUnit());
            recipeService.save(copy);
            count++;
        }
        map.put("msg", "已复制 " + count + " 条配方");
        map.put("url", "/sell/seller/wms/recipe/list");
        return new ModelAndView("common/success", map);
    }

    @Autowired
    private com.sell.repository.RecipeRepository recipeRepo;

    @PostMapping("/recipe/save")
    public ModelAndView recipeSave(@RequestParam(required = false) Long recipeId,
                                   @RequestParam("productId") String productId,
                                   @RequestParam(required = false) String skuId,
                                   @RequestParam("wmsItemId") Long wmsItemId,
                                   @RequestParam(required = false) String wmsItemName,
                                   @RequestParam("quantity") BigDecimal quantity,
                                   @RequestParam(required = false) String unit,
                                   Map<String, Object> map) {
        Recipe r = recipeId != null
                ? recipeRepo.findById(recipeId).orElse(new Recipe())
                : new Recipe();
        r.setProductId(productId);
        r.setSkuId(skuId == null || skuId.isEmpty() ? null : skuId);
        r.setWmsItemId(wmsItemId);
        r.setWmsItemName(wmsItemName);
        r.setQuantity(quantity);
        r.setUnit(unit);
        recipeService.save(r);
        map.put("url", "/sell/seller/wms/recipe/list");
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/recipe/delete")
    public ModelAndView recipeDelete(@RequestParam("recipeId") Long recipeId, Map<String, Object> map) {
        recipeService.delete(recipeId);
        map.put("url", "/sell/seller/wms/recipe/list");
        return new ModelAndView("common/success", map);
    }

    private void saveCfg(String key, String value) {
        ShopConfig c = configRepo.findById(key).orElse(new ShopConfig());
        c.setConfigKey(key);
        c.setConfigValue(value);
        configRepo.save(c);
    }
}
