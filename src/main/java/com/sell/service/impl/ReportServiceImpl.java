package com.sell.service.impl;

import com.sell.dataobject.OrderMaster;
import com.sell.dataobject.ProductCategory;
import com.sell.dataobject.ProductInfo;
import com.sell.enums.PayTypeEnum;
import com.sell.repository.OrderDetailRepository;
import com.sell.repository.OrderMasterRepository;
import com.sell.repository.ProductCategoryRepository;
import com.sell.repository.ProductInfoRepository;
import com.sell.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMasterRepository orderMasterRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private ProductInfoRepository productInfoRepository;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Override
    public List<OrderMaster> findValidOrders(Date start, Date end) {
        // findByDateRange 已按 payStatus=1 过滤(排除未支付/已退款), 与 totals() 完全同口径, 直接返回即可
        return orderMasterRepository.findByDateRange(start, end);
    }

    @Override
    public List<Map<String, Object>> dailySalesSummary(Date start, Date end) {
        List<OrderMaster> orders = findValidOrders(start, end);
        Map<String, List<OrderMaster>> byDay = orders.stream().collect(Collectors.groupingBy(o ->
                LocalDate.ofInstant(o.getCreateTime().toInstant(), ZoneId.systemDefault()).toString()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<OrderMaster>> e : new TreeMap<>(byDay).entrySet()) {
            BigDecimal sum = e.getValue().stream()
                    .map(OrderMaster::getOrderAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int count = e.getValue().size();
            BigDecimal avg = count > 0 ? sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", e.getKey());
            row.put("revenue", sum);
            row.put("orderCount", count);
            row.put("avgPrice", avg);
            result.add(row);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> hourlySales(Date start, Date end) {
        List<OrderMaster> orders = findValidOrders(start, end);
        Map<Integer, BigDecimal> revByHour = new TreeMap<>();
        Map<Integer, Long> cntByHour = new TreeMap<>();
        for (int h = 0; h < 24; h++) {
            revByHour.put(h, BigDecimal.ZERO);
            cntByHour.put(h, 0L);
        }
        for (OrderMaster o : orders) {
            int h = LocalDateTime.ofInstant(o.getCreateTime().toInstant(), ZoneId.systemDefault()).getHour();
            revByHour.put(h, revByHour.get(h).add(o.getOrderAmount()));
            cntByHour.put(h, cntByHour.get(h) + 1);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("hour", String.format("%02d:00", h));
            r.put("revenue", revByHour.get(h));
            r.put("orderCount", cntByHour.get(h));
            rows.add(r);
        }
        return rows;
    }

    @Override
    public List<Map<String, Object>> productSales(Date start, Date end) {
        List<OrderMaster> orders = findValidOrders(start, end);
        if (orders.isEmpty()) return Collections.emptyList();
        List<String> ids = orders.stream().map(OrderMaster::getOrderId).collect(Collectors.toList());
        List<Object[]> rows = orderDetailRepository.findProductSalesDetail(ids);
        BigDecimal totalRev = BigDecimal.ZERO;
        for (Object[] r : rows) {
            totalRev = totalRev.add((BigDecimal) r[2]);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (Object[] r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rank", rank++);
            m.put("name", r[0]);
            m.put("qty", r[1]);
            m.put("revenue", r[2]);
            BigDecimal share = totalRev.signum() > 0
                    ? ((BigDecimal) r[2]).multiply(BigDecimal.valueOf(100)).divide(totalRev, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            m.put("share", share);
            result.add(m);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> categorySalesShare(Date start, Date end) {
        List<OrderMaster> orders = findValidOrders(start, end);
        if (orders.isEmpty()) return Collections.emptyList();
        List<String> ids = orders.stream().map(OrderMaster::getOrderId).collect(Collectors.toList());
        List<Object[]> rows = orderDetailRepository.findProductSalesByProductId(ids);

        // productId -> categoryType
        Map<String, Integer> productCat = new HashMap<>();
        for (ProductInfo p : productInfoRepository.findAll()) {
            productCat.put(p.getProductId(), p.getCategoryType());
        }
        // categoryType -> categoryName
        Map<Integer, String> catName = new HashMap<>();
        for (ProductCategory c : categoryRepository.findAll()) {
            catName.put(c.getCategoryType(), c.getCategoryName());
        }

        Map<String, BigDecimal> revByCat = new HashMap<>();
        Map<String, Long> qtyByCat = new HashMap<>();
        for (Object[] r : rows) {
            String pid = (String) r[0];
            Long qty = ((Number) r[2]).longValue();
            BigDecimal rev = (BigDecimal) r[3];
            Integer ctype = productCat.get(pid);
            String cname = ctype == null ? "未分类" : catName.getOrDefault(ctype, "未分类");
            revByCat.merge(cname, rev, BigDecimal::add);
            qtyByCat.merge(cname, qty, Long::sum);
        }
        BigDecimal totalRev = revByCat.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : revByCat.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("category", e.getKey());
            m.put("qty", qtyByCat.getOrDefault(e.getKey(), 0L));
            m.put("revenue", e.getValue());
            BigDecimal share = totalRev.signum() > 0
                    ? e.getValue().multiply(BigDecimal.valueOf(100)).divide(totalRev, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            m.put("share", share);
            result.add(m);
        }
        result.sort((a, b) -> ((BigDecimal) b.get("revenue")).compareTo((BigDecimal) a.get("revenue")));
        return result;
    }

    @Override
    public List<Map<String, Object>> paymentMethodStats(Date start, Date end) {
        List<Object[]> rows = orderMasterRepository.sumByPayType(start, end);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            Integer code = (Integer) r[0];
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("methodCode", code);
            m.put("methodName", code != null && code == PayTypeEnum.ALIPAY.getCode() ? "支付宝" : "微信支付");
            m.put("count", r[1]);
            m.put("amount", r[2]);
            result.add(m);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> diningTypeStats(Date start, Date end) {
        List<OrderMaster> orders = findValidOrders(start, end);
        Map<String, BigDecimal> rev = new HashMap<>();
        Map<String, Long> cnt = new HashMap<>();
        for (OrderMaster o : orders) {
            String name = (o.getDiningType() != null && o.getDiningType() == 1) ? "外带" : "堂食";
            rev.merge(name, o.getOrderAmount(), BigDecimal::add);
            cnt.merge(name, 1L, Long::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : rev.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", e.getKey());
            m.put("count", cnt.get(e.getKey()));
            m.put("amount", e.getValue());
            result.add(m);
        }
        return result;
    }

    @Override
    public Map<String, Object> totals(Date start, Date end) {
        Long count = orderMasterRepository.countByDateRange(start, end);
        BigDecimal sum = orderMasterRepository.sumAmountByDateRange(start, end);
        BigDecimal avg = count > 0
                ? sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderCount", count);
        m.put("revenue", sum);
        m.put("avgPrice", avg);
        return m;
    }
}
