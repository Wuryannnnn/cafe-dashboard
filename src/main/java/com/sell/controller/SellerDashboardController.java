package com.sell.controller;

import com.sell.dataobject.OrderMaster;
import com.sell.dataobject.ShopConfig;
import com.sell.repository.OrderDetailRepository;
import com.sell.repository.OrderMasterRepository;
import com.sell.repository.ProductInfoRepository;
import com.sell.repository.ShopConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/seller/dashboard")
@Slf4j
public class SellerDashboardController {

    @Autowired
    private OrderMasterRepository orderMasterRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private ShopConfigRepository shopConfigRepository;

    @Autowired
    private ProductInfoRepository productInfoRepository;

    @GetMapping("")
    public String index(Model model) {
        String shopName = shopConfigRepository.findById("shopName")
                .map(ShopConfig::getConfigValue).orElse("咖啡厅");
        long productCount = productInfoRepository.count();
        model.addAttribute("shopName", shopName);
        model.addAttribute("productCount", productCount);
        java.time.LocalDate today = java.time.LocalDate.now();
        String[] weekdays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        int dow = today.getDayOfWeek().getValue() % 7;
        model.addAttribute("todayLabel",
                today.getYear() + "年" + today.getMonthValue() + "月" + today.getDayOfMonth() + "日 · " + weekdays[dow]);
        return "dashboard/index";
    }

    /** 仪表盘数据API */
    @GetMapping("/data")
    @ResponseBody
    public Map<String, Object> getData(@RequestParam(value = "days", defaultValue = "7") int days) {
        Map<String, Object> result = new HashMap<>();

        // ===== 今日数据 =====
        Date todayStart = toDate(LocalDate.now());
        Date todayEnd = toDate(LocalDate.now().plusDays(1));
        Long todayOrders = orderMasterRepository.countByDateRange(todayStart, todayEnd);
        BigDecimal todayRevenue = orderMasterRepository.sumAmountByDateRange(todayStart, todayEnd);
        BigDecimal avgPrice = todayOrders > 0 ? todayRevenue.divide(BigDecimal.valueOf(todayOrders), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        result.put("todayOrders", todayOrders);
        result.put("todayRevenue", todayRevenue);
        result.put("avgPrice", avgPrice);

        // ===== 昨日对比 =====
        Date yestStart = toDate(LocalDate.now().minusDays(1));
        Long yestOrders = orderMasterRepository.countByDateRange(yestStart, todayStart);
        BigDecimal yestRevenue = orderMasterRepository.sumAmountByDateRange(yestStart, todayStart);
        result.put("yestOrders", yestOrders);
        result.put("yestRevenue", yestRevenue);

        // ===== 近N天趋势 =====
        List<String> trendLabels = new ArrayList<>();
        List<BigDecimal> trendRevenue = new ArrayList<>();
        List<Long> trendOrders = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            Date s = toDate(d);
            Date e = toDate(d.plusDays(1));
            trendLabels.add(String.format("%d/%d", d.getMonthValue(), d.getDayOfMonth()));
            trendRevenue.add(orderMasterRepository.sumAmountByDateRange(s, e));
            trendOrders.add(orderMasterRepository.countByDateRange(s, e));
        }
        result.put("trendLabels", trendLabels);
        result.put("trendRevenue", trendRevenue);
        result.put("trendOrders", trendOrders);

        // ===== 支付方式分布(今日已支付) =====
        List<Object[]> payData = orderMasterRepository.sumByPayType(todayStart, todayEnd);
        List<Map<String, Object>> payTypes = new ArrayList<>();
        for (Object[] row : payData) {
            Map<String, Object> m = new HashMap<>();
            Integer payType = (Integer) row[0];
            m.put("name", payType == 1 ? "支付宝" : "微信支付");
            m.put("count", row[1]);
            m.put("amount", row[2]);
            payTypes.add(m);
        }
        result.put("payTypes", payTypes);

        // ===== 热销TOP10(近N天) =====
        Date rangeStart = toDate(LocalDate.now().minusDays(days));
        List<OrderMaster> rangeOrders = orderMasterRepository.findByDateRange(rangeStart, todayEnd);
        List<String> orderIds = rangeOrders.stream().map(OrderMaster::getOrderId).collect(Collectors.toList());
        List<Map<String, Object>> topProducts = new ArrayList<>();
        if (!orderIds.isEmpty()) {
            List<Object[]> tops = orderDetailRepository.findTopProducts(orderIds);
            int rank = 1;
            for (Object[] row : tops) {
                if (rank > 10) break;
                Map<String, Object> m = new HashMap<>();
                m.put("rank", rank++);
                m.put("name", row[0]);
                m.put("qty", row[1]);
                topProducts.add(m);
            }
        }
        result.put("topProducts", topProducts);

        // ===== 订单状态分布(今日) =====
        List<OrderMaster> todayOrderList = orderMasterRepository.findByDateRange(todayStart, todayEnd);
        Map<String, Long> statusDist = new java.util.LinkedHashMap<>();
        String[] statusNames = {"新订单", "制作中", "待取餐", "完结"};
        for (int i = 0; i < statusNames.length; i++) {
            final int code = i;
            statusDist.put(statusNames[i], todayOrderList.stream().filter(o -> o.getOrderStatus() == code).count());
        }
        result.put("statusDist", statusDist);

        return result;
    }

    private Date toDate(LocalDate ld) {
        return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
