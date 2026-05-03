package com.sell.repository;

import com.sell.dataobject.OrderMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface OrderMasterRepository extends JpaRepository<OrderMaster, String> {

    Page<OrderMaster> findByBuyerOpenid(String buyerOpenid, Pageable pageable);

    /** 指定时间范围内的订单(排除已取消) */
    @Query("SELECT o FROM OrderMaster o WHERE o.createTime >= :start AND o.createTime < :end AND o.orderStatus <> 4")
    List<OrderMaster> findByDateRange(@Param("start") Date start, @Param("end") Date end);

    /** 统计指定时间范围内的订单数 */
    @Query("SELECT COUNT(o) FROM OrderMaster o WHERE o.createTime >= :start AND o.createTime < :end AND o.orderStatus <> 4")
    Long countByDateRange(@Param("start") Date start, @Param("end") Date end);

    /** 统计指定时间范围内的总金额 */
    @Query("SELECT COALESCE(SUM(o.orderAmount), 0) FROM OrderMaster o WHERE o.createTime >= :start AND o.createTime < :end AND o.orderStatus <> 4")
    java.math.BigDecimal sumAmountByDateRange(@Param("start") Date start, @Param("end") Date end);

    /** 按支付方式统计 */
    @Query("SELECT o.payType, COUNT(o), COALESCE(SUM(o.orderAmount), 0) FROM OrderMaster o WHERE o.createTime >= :start AND o.createTime < :end AND o.orderStatus <> 4 AND o.payStatus = 1 GROUP BY o.payType")
    List<Object[]> sumByPayType(@Param("start") Date start, @Param("end") Date end);

    /** 按状态查询占用中的桌台订单 (排除 tableId 为空的). */
    List<OrderMaster> findByOrderStatusInAndTableIdIsNotNull(List<Integer> orderStatuses);
}
