package com.sell.controller;

import com.sell.dataobject.CommonRemark;
import com.sell.dataobject.Coupon;
import com.sell.dataobject.Member;
import com.sell.dataobject.MemberLevel;
import com.sell.dataobject.OperationLog;
import com.sell.dataobject.OrderMaster;
import com.sell.dataobject.PaymentMethod;
import com.sell.dataobject.PrinterConfig;
import com.sell.dataobject.ProductCategory;
import com.sell.dataobject.ProductInfo;
import com.sell.dataobject.Promotion;
import com.sell.dataobject.RechargePlan;
import com.sell.dataobject.RestaurantArea;
import com.sell.dataobject.RestaurantTable;
import com.sell.dataobject.SettleAccount;
import com.sell.dataobject.ShopConfig;
import com.sell.dataobject.Staff;
import com.sell.dto.OrderDTO;
import com.sell.repository.CommonRemarkRepository;
import com.sell.repository.CouponRepository;
import com.sell.repository.DamageRecordRepository;
import com.sell.repository.ExpenseCategoryRepository;
import com.sell.repository.ExpenseRecordRepository;
import com.sell.repository.MemberLevelRepository;
import com.sell.repository.MemberRepository;
import com.sell.repository.OperationLogRepository;
import com.sell.repository.OrderMasterRepository;
import com.sell.repository.PaymentMethodRepository;
import com.sell.repository.PrinterConfigRepository;
import com.sell.repository.ProductCategoryRepository;
import com.sell.repository.ProductInfoRepository;
import com.sell.repository.PromotionRepository;
import com.sell.repository.RechargePlanRepository;
import com.sell.repository.RestaurantAreaRepository;
import com.sell.repository.RestaurantTableRepository;
import com.sell.repository.AddonGroupRepository;
import com.sell.repository.ProductAddonRepository;
import com.sell.repository.ProductSkuRepository;
import com.sell.repository.RecipeRepository;
import com.sell.repository.SettleAccountRepository;
import com.sell.repository.ShopConfigRepository;
import com.sell.repository.StaffRepository;
import com.sell.repository.StockRecordRepository;
import com.sell.repository.TimeSlotRepository;
import com.sell.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * React 后台 (Phase 3+) 的 JSON API. 老 FreeMarker 控制器保持不动.
 */
@RestController
@RequestMapping("/api/admin")
public class ApiAdminController {

    @Autowired private OrderService orderService;
    @Autowired private com.sell.repository.PendingWmsShipmentRepository pendingWmsRepo;
    @Autowired private com.sell.service.WmsRetryService wmsRetryService;
    @Autowired private OrderMasterRepository orderMasterRepository;
    @Autowired private ProductInfoRepository productInfoRepository;
    @Autowired private ProductCategoryRepository categoryRepository;
    @Autowired private RestaurantTableRepository tableRepository;
    @Autowired private RestaurantAreaRepository areaRepository;
    @Autowired private PaymentMethodRepository paymentMethodRepository;
    @Autowired private ShopConfigRepository shopConfigRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberLevelRepository memberLevelRepository;
    @Autowired private RechargePlanRepository rechargePlanRepository;
    @Autowired private CouponRepository couponRepository;
    @Autowired private PromotionRepository promotionRepository;
    @Autowired private CommonRemarkRepository commonRemarkRepository;
    @Autowired private StaffRepository staffRepository;
    @Autowired private OperationLogRepository operationLogRepository;
    @Autowired private PrinterConfigRepository printerConfigRepository;
    @Autowired private SettleAccountRepository settleAccountRepository;
    @Autowired private ExpenseCategoryRepository expenseCategoryRepository;
    @Autowired private ExpenseRecordRepository expenseRecordRepository;
    @Autowired private DamageRecordRepository damageRecordRepository;
    @Autowired private AddonGroupRepository addonGroupRepository;
    @Autowired private ProductAddonRepository productAddonRepository;
    @Autowired private ProductSkuRepository productSkuRepository;
    @Autowired private TimeSlotRepository timeSlotRepository;
    @Autowired private StockRecordRepository stockRecordRepository;
    @Autowired private RecipeRepository recipeRepository;

    @GetMapping("/orders")
    public Map<String, Object> orders(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "keyword", required = false) String keyword) {
        String kw = (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim();
        org.springframework.data.domain.Pageable pageable =
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<OrderMaster> p = orderMasterRepository.search(status, kw, pageable);
        List<OrderDTO> content = com.sell.converter.OrderMaster2OrderDTOConverter.convert(p.getContent());
        Map<String, Object> r = new HashMap<>();
        r.put("content", content);
        r.put("totalElements", p.getTotalElements());
        r.put("totalPages", p.getTotalPages());
        r.put("page", page);
        r.put("size", size);
        return r;
    }

    @GetMapping("/orders/pending")
    public List<OrderMaster> pendingOrders(
            @RequestParam(value = "tableId", required = false) Integer tableId) {
        List<OrderMaster> list = new java.util.ArrayList<>(orderMasterRepository.findAll(Sort.by(Sort.Direction.DESC, "createTime")));
        list.removeIf(o -> o.getPayStatus() == null || o.getPayStatus() != 0);
        list.removeIf(o -> o.getOrderStatus() != null && o.getOrderStatus() >= 3);
        if (tableId != null) {
            String code = tableRepository.findById(tableId).map(RestaurantTable::getTableCode).orElse(null);
            if (code != null) {
                list.removeIf(o -> !code.equals(o.getTableNumber()));
            }
        }
        return list;
    }

    @GetMapping("/orders/{orderId}")
    public OrderDTO orderDetail(@PathVariable("orderId") String orderId) {
        return orderService.findOne(orderId);
    }

    /* ===== 订单操作 (PRD 6.3 / 7.3) —— React 后台用, 统一返回 {code,msg}; 业务失败 code=-1 ===== */

    @Autowired private com.sell.service.CashierService cashierService;

    private Map<String, Object> ok(String msg) {
        Map<String, Object> r = new HashMap<>();
        r.put("code", 0);
        r.put("msg", msg);
        return r;
    }

    private Map<String, Object> fail(Exception e) {
        Map<String, Object> r = new HashMap<>();
        r.put("code", -1);
        r.put("msg", e.getMessage() != null ? e.getMessage() : "操作失败");
        return r;
    }

    @PostMapping("/orders/{orderId}/making")
    public Map<String, Object> orderMaking(@PathVariable("orderId") String orderId) {
        try { orderService.making(orderService.findOne(orderId)); return ok("已接单, 开始制作"); }
        catch (Exception e) { return fail(e); }
    }

    @PostMapping("/orders/{orderId}/ready")
    public Map<String, Object> orderReady(@PathVariable("orderId") String orderId) {
        try { orderService.ready(orderService.findOne(orderId)); return ok("已标记待取餐"); }
        catch (Exception e) { return fail(e); }
    }

    @PostMapping("/orders/{orderId}/finish")
    public Map<String, Object> orderFinish(@PathVariable("orderId") String orderId) {
        try { orderService.finish(orderService.findOne(orderId)); return ok("订单已完结"); }
        catch (Exception e) { return fail(e); }
    }

    @PostMapping("/orders/{orderId}/cancel")
    public Map<String, Object> orderCancel(@PathVariable("orderId") String orderId) {
        try { orderService.cancel(orderService.findOne(orderId)); return ok("订单已取消"); }
        catch (Exception e) { return fail(e); }
    }

    @PostMapping("/orders/{orderId}/refund")
    public Map<String, Object> orderRefund(@PathVariable("orderId") String orderId) {
        try { orderService.refund(orderService.findOne(orderId)); return ok("退款成功"); }
        catch (Exception e) { return fail(e); }
    }

    @PostMapping("/orders/{orderId}/free")
    public Map<String, Object> orderFree(@PathVariable("orderId") String orderId) {
        try { orderService.freeOrder(orderId); return ok("已免单"); }
        catch (Exception e) { return fail(e); }
    }

    @PostMapping("/orders/{orderId}/amount")
    public Map<String, Object> orderAmount(@PathVariable("orderId") String orderId,
                                           @RequestParam("newAmount") java.math.BigDecimal newAmount) {
        try { orderService.updateAmount(orderId, newAmount); return ok("改价成功"); }
        catch (Exception e) { return fail(e); }
    }

    @PostMapping("/orders/{orderId}/discount")
    public Map<String, Object> orderDiscount(@PathVariable("orderId") String orderId,
                                             @RequestParam("discountRate") int discountRate) {
        try { orderService.applyDiscount(orderId, discountRate); return ok("打折成功"); }
        catch (Exception e) { return fail(e); }
    }

    /* ===== 收银台: 手动建单 / 组合(分笔)收款 ===== */

    @PostMapping("/cashier/manual-order")
    public Map<String, Object> manualOrder(@RequestParam(value = "tableId", required = false) Integer tableId,
                                           @RequestParam("items") String items) {
        try {
            OrderDTO d = cashierService.manualCreateOrder(tableId, items, "cashier");
            Map<String, Object> r = ok("已创建订单");
            r.put("orderId", d.getOrderId());
            return r;
        } catch (Exception e) { return fail(e); }
    }

    /** 整单一次性收款 (单一支付方式). */
    @PostMapping("/cashier/offline-pay")
    public Map<String, Object> offlinePay(@RequestParam("orderId") String orderId,
                                          @RequestParam("methodId") Integer methodId) {
        try { cashierService.offlinePay(orderId, methodId, "cashier"); return ok("收款成功"); }
        catch (Exception e) { return fail(e); }
    }

    /** 组合收款: 记一笔指定金额的付款, 累计达到订单金额自动结清. */
    @PostMapping("/cashier/pay-record")
    public Map<String, Object> payRecord(@RequestParam("orderId") String orderId,
                                         @RequestParam("methodId") Integer methodId,
                                         @RequestParam("amount") java.math.BigDecimal amount) {
        try { cashierService.addPaymentRecord(orderId, methodId, amount, "cashier"); return ok("已记一笔收款"); }
        catch (Exception e) { return fail(e); }
    }

    /** 某订单的收款流水 (组合收款用). */
    @GetMapping("/cashier/payments")
    public List<com.sell.dataobject.OrderPaymentRecord> cashierPayments(@RequestParam("orderId") String orderId) {
        return cashierService.findPaymentsByOrder(orderId);
    }

    /** WMS 同步失败队列. */
    @GetMapping("/wms/pending")
    public Map<String, Object> wmsPending() {
        List<com.sell.dataobject.PendingWmsShipment> rows =
                pendingWmsRepo.findAll(Sort.by(Sort.Direction.DESC, "createTime")).stream().limit(200).toList();
        Map<String, Object> r = new HashMap<>();
        r.put("items", rows);
        r.put("pendingCount", pendingWmsRepo.countByStatus("pending"));
        r.put("failedCount", pendingWmsRepo.countByStatus("failed"));
        return r;
    }

    /** 手动重试某条补偿记录. */
    @GetMapping("/wms/pending/retry")
    public Map<String, Object> wmsRetryOne(@RequestParam("id") Long id) {
        boolean ok = wmsRetryService.retryNow(id);
        Map<String, Object> r = new HashMap<>();
        r.put("code", ok ? 0 : 1);
        r.put("msg", ok ? "已触发重试" : "记录不存在");
        return r;
    }

    @GetMapping("/products")
    public List<ProductInfo> products() {
        return productInfoRepository.findAll(Sort.by("categoryType"));
    }

    /** 切换商品在顾客端(H5)是否展示 (PRD 4.2 点餐菜品独立管理). show=true 展示, false 隐藏. */
    @PostMapping("/products/{productId}/h5-display")
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> setH5Display(@PathVariable("productId") String productId,
                                            @RequestParam("show") boolean show) {
        int updated = productInfoRepository.updateH5Display(productId, show ? 1 : 0);
        return updated > 0 ? ok(show ? "已在顾客端展示" : "已从顾客端隐藏") : fail(new RuntimeException("商品不存在"));
    }

    /** 复制商品: 基于现有商品生成一份副本(基本信息 + 规格 SKU), 新副本默认停售, 供店主改名/调价后上架. */
    @PostMapping("/products/{productId}/copy")
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> copyProduct(@PathVariable("productId") String productId) {
        try {
            ProductInfo src = productInfoRepository.findById(productId).orElse(null);
            if (src == null) return fail(new RuntimeException("商品不存在"));

            java.util.Date now = new java.util.Date();
            ProductInfo copy = new ProductInfo();
            org.springframework.beans.BeanUtils.copyProperties(src, copy);
            copy.setProductId(com.sell.utils.KeyUtil.genUniqueKey());
            copy.setProductName((src.getProductName() == null ? "" : src.getProductName()) + "(副本)");
            copy.setProductStatus(com.sell.enums.ProductStatusEnum.DOWN.getCode()); // 默认停售, 确认后再上架
            copy.setCreateTime(now);
            copy.setUpdateTime(now);
            productInfoRepository.save(copy);

            // 连同规格(SKU)一起复制, 各自生成新 skuId 指向副本
            for (com.sell.dataobject.ProductSku s : productSkuRepository.findByProductId(productId)) {
                com.sell.dataobject.ProductSku ns = new com.sell.dataobject.ProductSku();
                org.springframework.beans.BeanUtils.copyProperties(s, ns);
                ns.setSkuId(com.sell.utils.KeyUtil.genUniqueKey());
                ns.setProductId(copy.getProductId());
                productSkuRepository.save(ns);
            }
            return ok("已复制为「" + copy.getProductName() + "」, 默认停售, 可编辑后上架");
        } catch (Exception e) {
            return fail(e);
        }
    }

    @GetMapping("/categories")
    public List<ProductCategory> categories() {
        return categoryRepository.findAll(Sort.by("categoryType"));
    }

    @GetMapping("/tables")
    public List<RestaurantTable> tables() {
        return tableRepository.findAll(Sort.by("areaId", "sortOrder"));
    }

    @GetMapping("/payment-methods")
    public List<PaymentMethod> paymentMethods() {
        return paymentMethodRepository.findByEnabledTrueOrderBySortOrderAscMethodIdAsc();
    }

    @GetMapping("/shop-config")
    public Map<String, String> shopConfig() {
        Map<String, String> r = new HashMap<>();
        for (ShopConfig c : shopConfigRepository.findAll()) {
            r.put(c.getConfigKey(), c.getConfigValue());
        }
        return r;
    }

    @GetMapping("/areas")
    public List<RestaurantArea> areas() {
        return areaRepository.findAll(Sort.by("sortOrder"));
    }

    @GetMapping("/members")
    public List<Member> members() {
        return memberRepository.findAll(Sort.by(Sort.Direction.DESC, "registerTime"));
    }

    @Autowired private com.sell.service.MemberService memberService;

    /** 会员概览看板: 总会员 / 本周新增 / 活跃 / 储值总余额 (PRD 8.1). */
    @GetMapping("/member-overview")
    public Map<String, Object> memberOverview() {
        return memberService.overview();
    }

    @GetMapping("/member-levels")
    public List<MemberLevel> memberLevels() {
        return memberLevelRepository.findAll(Sort.by("sortOrder"));
    }

    @GetMapping("/recharge-plans")
    public List<RechargePlan> rechargePlans() {
        return rechargePlanRepository.findAll(Sort.by("sortOrder"));
    }

    @GetMapping("/coupons")
    public List<Coupon> coupons() {
        return couponRepository.findAll(Sort.by(Sort.Direction.DESC, "couponId"));
    }

    /** 定向发放优惠券: target=all 全部会员 / level 指定等级(value=等级id) / tag 指定标签(value=标签). PRD 8.6. */
    @PostMapping("/coupons/{couponId}/distribute")
    public Map<String, Object> distributeCoupon(@PathVariable("couponId") Integer couponId,
                                                @RequestParam("target") String target,
                                                @RequestParam(value = "value", required = false) String value) {
        try {
            int n = memberService.distributeCoupon(couponId, target, value);
            return ok("已发放 " + n + " 张");
        } catch (Exception e) {
            return fail(e);
        }
    }

    @GetMapping("/promotions")
    public List<Promotion> promotions() {
        return promotionRepository.findAll(Sort.by(Sort.Direction.DESC, "promotionId"));
    }

    @GetMapping("/remarks")
    public List<CommonRemark> remarks() {
        return commonRemarkRepository.findAll(Sort.by("sortOrder"));
    }

    @GetMapping("/staff")
    public List<Staff> staff() {
        return staffRepository.findAll(Sort.by("staffId"));
    }

    /** 操作日志: 支持按日期区间(start/end, yyyy-MM-dd) + 关键字(操作员/类型/说明) 筛选, 均可空. */
    @GetMapping("/logs")
    public List<OperationLog> logs(@RequestParam(value = "start", required = false) String start,
                                   @RequestParam(value = "end", required = false) String end,
                                   @RequestParam(value = "keyword", required = false) String keyword) {
        java.util.Date s = parseDay(start, 0);
        java.util.Date e = parseDay(end, 1); // 含当天: 取到次日 0 点 (左闭右开)
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return operationLogRepository.search(s, e, kw,
                PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "createTime"))).getContent();
    }

    /** 把 yyyy-MM-dd 解析成当天 0 点(+plusDays 天)的时间; 空或非法格式返回 null(即不限). */
    private java.util.Date parseDay(String day, int plusDays) {
        if (day == null || day.isBlank()) return null;
        try {
            return java.util.Date.from(java.time.LocalDate.parse(day.trim()).plusDays(plusDays)
                    .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        } catch (java.time.format.DateTimeParseException ex) {
            return null;
        }
    }

    @GetMapping("/printers")
    public List<PrinterConfig> printers() {
        return printerConfigRepository.findAll();
    }

    @GetMapping("/settle-accounts")
    public List<SettleAccount> settleAccounts() {
        return settleAccountRepository.findAll(Sort.by("accountId"));
    }

    @GetMapping("/expense-categories")
    public List<com.sell.dataobject.ExpenseCategory> expenseCategories() {
        return expenseCategoryRepository.findAll(Sort.by("sortOrder"));
    }

    @GetMapping("/expenses")
    public List<com.sell.dataobject.ExpenseRecord> expenses() {
        return expenseRecordRepository.findAll(Sort.by(Sort.Direction.DESC, "occurDate"));
    }

    @GetMapping("/damages")
    public List<com.sell.dataobject.DamageRecord> damages() {
        return damageRecordRepository.findAll(Sort.by(Sort.Direction.DESC, "occurDate"));
    }

    @GetMapping("/addon-groups")
    public List<com.sell.dataobject.AddonGroup> addonGroups() {
        return addonGroupRepository.findAll(Sort.by("sortOrder", "groupId"));
    }

    @GetMapping("/addons")
    public List<com.sell.dataobject.ProductAddon> addons() {
        return productAddonRepository.findAll(Sort.by("groupId", "sortOrder"));
    }

    @GetMapping("/skus")
    public List<com.sell.dataobject.ProductSku> skus(@RequestParam(value = "productId", required = false) String productId) {
        if (productId != null) return productSkuRepository.findByProductId(productId);
        return productSkuRepository.findAll();
    }

    @GetMapping("/timeslots")
    public List<com.sell.dataobject.TimeSlot> timeslots() {
        return timeSlotRepository.findAll(Sort.by("sortOrder", "slotId"));
    }

    @GetMapping("/stock-records")
    public List<com.sell.dataobject.StockRecord> stockRecords(@RequestParam(value = "productId", required = false) String productId,
                                                              @RequestParam(value = "limit", defaultValue = "100") int limit) {
        return stockRecordRepository.findAll(Sort.by(Sort.Direction.DESC, "createTime")).stream()
                .filter(s -> productId == null || productId.equals(s.getProductId()))
                .limit(limit).toList();
    }

    @GetMapping("/recipes")
    public List<com.sell.dataobject.Recipe> recipes(@RequestParam(value = "productId", required = false) String productId) {
        if (productId != null) return recipeRepository.findByProductId(productId);
        return recipeRepository.findAll(Sort.by("productId"));
    }

    @Autowired
    private com.sell.service.WmsClient wmsClient;

    @GetMapping("/wms-items")
    public List<Map<String, Object>> wmsItems() {
        try {
            return wmsClient.listItems();
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    @GetMapping("/reports/sales")
    public Map<String, Object> reportsSales(@RequestParam(value = "days", defaultValue = "7") int days) {
        java.time.LocalDate end = java.time.LocalDate.now();
        java.time.LocalDate start = end.minusDays(days - 1);
        java.util.List<Map<String, Object>> daily = new java.util.ArrayList<>();
        java.math.BigDecimal totalRev = java.math.BigDecimal.ZERO;
        long totalCnt = 0;
        for (int i = 0; i < days; i++) {
            java.time.LocalDate d = start.plusDays(i);
            java.util.Date s = java.util.Date.from(d.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
            java.util.Date e = java.util.Date.from(d.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
            long cnt = orderMasterRepository.countByDateRange(s, e);
            java.math.BigDecimal rev = orderMasterRepository.sumAmountByDateRange(s, e);
            Map<String, Object> row = new HashMap<>();
            row.put("date", d.toString());
            row.put("orderCount", cnt);
            row.put("revenue", rev);
            row.put("avgPrice", cnt > 0 ? rev.divide(java.math.BigDecimal.valueOf(cnt), 2, java.math.RoundingMode.HALF_UP) : java.math.BigDecimal.ZERO);
            daily.add(row);
            totalRev = totalRev.add(rev);
            totalCnt += cnt;
        }
        java.math.BigDecimal[] hourly = new java.math.BigDecimal[24];
        for (int h = 0; h < 24; h++) hourly[h] = java.math.BigDecimal.ZERO;
        java.util.Date startD = java.util.Date.from(start.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        java.util.Date endD = java.util.Date.from(end.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        for (OrderMaster om : orderMasterRepository.findByDateRange(startD, endD)) {
            int h = om.getCreateTime().toInstant().atZone(java.time.ZoneId.systemDefault()).getHour();
            hourly[h] = hourly[h].add(om.getOrderAmount() == null ? java.math.BigDecimal.ZERO : om.getOrderAmount());
        }
        java.util.List<Map<String, Object>> hourlyList = new java.util.ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> r = new HashMap<>();
            r.put("hour", String.format("%02d:00", h));
            r.put("revenue", hourly[h]);
            hourlyList.add(r);
        }
        Map<String, Object> r = new HashMap<>();
        r.put("daily", daily);
        r.put("hourly", hourlyList);
        Map<String, Object> totals = new HashMap<>();
        totals.put("revenue", totalRev);
        totals.put("orderCount", totalCnt);
        totals.put("avgPrice", totalCnt > 0 ? totalRev.divide(java.math.BigDecimal.valueOf(totalCnt), 2, java.math.RoundingMode.HALF_UP) : java.math.BigDecimal.ZERO);
        r.put("totals", totals);
        r.put("days", days);
        return r;
    }

    @GetMapping("/reports/products")
    public Map<String, Object> reportsProducts(@RequestParam(value = "days", defaultValue = "30") int days) {
        java.time.LocalDate end = java.time.LocalDate.now();
        java.time.LocalDate start = end.minusDays(days - 1);
        java.util.Date startD = java.util.Date.from(start.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        java.util.Date endD = java.util.Date.from(end.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        java.util.List<OrderMaster> orders = orderMasterRepository.findByDateRange(startD, endD);
        java.util.List<String> ids = orders.stream().map(OrderMaster::getOrderId).toList();
        java.util.List<Map<String, Object>> top = new java.util.ArrayList<>();
        if (!ids.isEmpty()) {
            try {
                java.util.List<Object[]> rows = ((com.sell.repository.OrderDetailRepository) ctx.getBean("orderDetailRepository")).findTopProducts(ids);
                int rank = 1;
                for (Object[] row : rows) {
                    if (rank > 50) break;
                    Map<String, Object> m = new HashMap<>();
                    m.put("rank", rank++);
                    m.put("name", row[0]);
                    m.put("qty", row[1]);
                    top.add(m);
                }
            } catch (Exception ignored) {}
        }
        Map<String, Object> r = new HashMap<>();
        r.put("days", days);
        r.put("topProducts", top);
        return r;
    }

    @Autowired private com.sell.service.ReportService reportService;

    @GetMapping("/reports/payments")
    public Map<String, Object> reportsPayments(@RequestParam(value = "days", defaultValue = "30") int days) {
        java.time.LocalDate end = java.time.LocalDate.now();
        java.time.LocalDate start = end.minusDays(days - 1);
        java.util.Date startD = java.util.Date.from(start.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        java.util.Date endD = java.util.Date.from(end.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        // 用实际收款方式口径(含现金/会员卡), 而非 OrderMaster.payType(只有微信/支付宝)
        java.util.List<Map<String, Object>> rows = new java.util.ArrayList<>();
        for (Map<String, Object> s : reportService.paymentMethodStats(startD, endD)) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", s.get("methodName"));
            m.put("count", s.get("count"));
            m.put("amount", s.get("amount"));
            rows.add(m);
        }
        Map<String, Object> r = new HashMap<>();
        r.put("days", days);
        r.put("rows", rows);
        return r;
    }

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.context.ApplicationContext ctx;

    @GetMapping("/finance-overview")
    public Map<String, Object> financeOverview() {
        java.time.LocalDate first = java.time.LocalDate.now().withDayOfMonth(1);
        java.math.BigDecimal income = java.math.BigDecimal.ZERO;
        java.math.BigDecimal expense = java.math.BigDecimal.ZERO;
        for (com.sell.dataobject.ExpenseRecord r : expenseRecordRepository.findAll()) {
            if (r.getOccurDate() == null) continue;
            java.time.LocalDate d = r.getOccurDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            if (d.isBefore(first)) continue;
            if (r.getRecordType() != null && r.getRecordType() == 1) income = income.add(r.getAmount() == null ? java.math.BigDecimal.ZERO : r.getAmount());
            else expense = expense.add(r.getAmount() == null ? java.math.BigDecimal.ZERO : r.getAmount());
        }
        // 营业收入: 本月已支付订单总额 (PRD 9.5 财务报表需"营业收入 vs 日常支出对比")
        java.util.Date mStart = java.util.Date.from(first.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        java.util.Date mEnd = java.util.Date.from(first.plusMonths(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        java.math.BigDecimal orderRevenue = orderMasterRepository.sumAmountByDateRange(mStart, mEnd);
        if (orderRevenue == null) orderRevenue = java.math.BigDecimal.ZERO;

        Map<String, Object> r = new HashMap<>();
        r.put("monthOrderRevenue", orderRevenue);        // 营业收入(订单)
        r.put("monthIncome", income);                    // 其他收入(手动记账)
        r.put("monthExpense", expense);                  // 日常支出
        r.put("monthBalance", income.subtract(expense)); // 兼容旧字段(其他收支结余)
        r.put("monthNet", orderRevenue.add(income).subtract(expense)); // 净利 = 营业收入+其他收入-支出
        return r;
    }
}
