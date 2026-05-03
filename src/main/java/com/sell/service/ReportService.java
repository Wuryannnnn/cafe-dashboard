package com.sell.service;

import com.sell.dataobject.OrderMaster;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 报表 - PRD 9
 */
public interface ReportService {

    /** 营业汇总: 按天分组 (营业额/订单数/客单价). */
    List<Map<String, Object>> dailySalesSummary(Date start, Date end);

    /** 时段分析: 24 小时分布. */
    List<Map<String, Object>> hourlySales(Date start, Date end);

    /** 菜品销量排行 + 销售额. */
    List<Map<String, Object>> productSales(Date start, Date end);

    /** 分类销售占比. */
    List<Map<String, Object>> categorySalesShare(Date start, Date end);

    /** 收款方式统计. */
    List<Map<String, Object>> paymentMethodStats(Date start, Date end);

    /** 按业务类型 (堂食/外带) 收款汇总. */
    List<Map<String, Object>> diningTypeStats(Date start, Date end);

    /** 区间总额, 总订单, 客单价. */
    Map<String, Object> totals(Date start, Date end);

    /** 区间内有效订单 (排除已取消/已退款). */
    List<OrderMaster> findValidOrders(Date start, Date end);
}
