package com.sell.controller;

import com.sell.service.ReportService;
import com.sell.utils.ExcelExportUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** 报表中心 - PRD 9 */
@Controller
@RequestMapping("/seller/report")
public class SellerReportController {

    @Autowired
    private ReportService reportService;

    /** 9.2 营业报表. */
    @GetMapping("/sales")
    public ModelAndView sales(@RequestParam(value = "start", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                              @RequestParam(value = "end", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                              Map<String, Object> map) {
        if (start == null) start = LocalDate.now().minusDays(7);
        if (end == null) end = LocalDate.now();
        Date s = toDate(start);
        Date e = toDate(end.plusDays(1));
        map.put("daily", reportService.dailySalesSummary(s, e));
        map.put("hourly", reportService.hourlySales(s, e));
        map.put("totals", reportService.totals(s, e));
        map.put("start", start);
        map.put("end", end);
        return new ModelAndView("report/sales", map);
    }

    /** 9.3 菜品报表. */
    @GetMapping("/products")
    public ModelAndView products(@RequestParam(value = "start", required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                 @RequestParam(value = "end", required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                 Map<String, Object> map) {
        if (start == null) start = LocalDate.now().minusDays(7);
        if (end == null) end = LocalDate.now();
        Date s = toDate(start);
        Date e = toDate(end.plusDays(1));
        map.put("products", reportService.productSales(s, e));
        map.put("categories", reportService.categorySalesShare(s, e));
        map.put("start", start);
        map.put("end", end);
        return new ModelAndView("report/products", map);
    }

    /** 9.4 收款报表. */
    @GetMapping("/payments")
    public ModelAndView payments(@RequestParam(value = "start", required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                 @RequestParam(value = "end", required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                 Map<String, Object> map) {
        if (start == null) start = LocalDate.now().minusDays(7);
        if (end == null) end = LocalDate.now();
        Date s = toDate(start);
        Date e = toDate(end.plusDays(1));
        map.put("payMethods", reportService.paymentMethodStats(s, e));
        map.put("dining", reportService.diningTypeStats(s, e));
        map.put("totals", reportService.totals(s, e));
        map.put("start", start);
        map.put("end", end);
        return new ModelAndView("report/payments", map);
    }

    /** 通用 Excel 导出. type = sales/products/payments */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam("type") String type,
                                         @RequestParam(value = "start", required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                         @RequestParam(value = "end", required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        if (start == null) start = LocalDate.now().minusDays(7);
        if (end == null) end = LocalDate.now();
        Date s = toDate(start);
        Date e = toDate(end.plusDays(1));

        byte[] data;
        String filename;
        switch (type) {
            case "sales":
                data = ExcelExportUtil.toXlsx("营业汇总",
                        Arrays.asList("日期", "营业额", "订单数", "客单价"),
                        reportService.dailySalesSummary(s, e),
                        Arrays.asList("date", "revenue", "orderCount", "avgPrice"));
                filename = "营业报表-" + start + "_" + end + ".xlsx";
                break;
            case "products":
                data = ExcelExportUtil.toXlsx("菜品销售",
                        Arrays.asList("排名", "菜品", "销量", "销售额", "占比%"),
                        reportService.productSales(s, e),
                        Arrays.asList("rank", "name", "qty", "revenue", "share"));
                filename = "菜品报表-" + start + "_" + end + ".xlsx";
                break;
            case "payments":
                data = ExcelExportUtil.toXlsx("收款方式",
                        Arrays.asList("结账方式", "订单数", "金额"),
                        reportService.paymentMethodStats(s, e),
                        Arrays.asList("methodName", "count", "amount"));
                filename = "收款报表-" + start + "_" + end + ".xlsx";
                break;
            default:
                return ResponseEntity.badRequest().build();
        }
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded);
        return ResponseEntity.ok().headers(headers).body(data);
    }

    private static Date toDate(LocalDate ld) {
        return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
