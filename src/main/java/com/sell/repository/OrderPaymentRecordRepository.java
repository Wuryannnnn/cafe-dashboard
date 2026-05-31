package com.sell.repository;

import com.sell.dataobject.OrderPaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface OrderPaymentRecordRepository extends JpaRepository<OrderPaymentRecord, Long> {

    List<OrderPaymentRecord> findByOrderIdOrderByPaymentIdAsc(String orderId);

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM OrderPaymentRecord o WHERE o.orderId = :orderId")
    BigDecimal sumByOrderId(@Param("orderId") String orderId);

    /**
     * 按【实际收款方式】统计 (现金/会员卡/微信/支付宝/...): 区间内、排除已退款订单(payStatus=2)的收款流水,
     * 返回 [methodName, 笔数, 金额]. 这才是真实收款口径——OrderMaster.payType 只记下单意向且漏现金/会员.
     */
    @Query("SELECT r.methodName, COUNT(r), COALESCE(SUM(r.amount), 0) "
            + "FROM OrderPaymentRecord r, OrderMaster o "
            + "WHERE r.orderId = o.orderId AND o.payStatus <> 2 "
            + "AND r.createTime >= :start AND r.createTime < :end "
            + "GROUP BY r.methodName ORDER BY SUM(r.amount) DESC")
    List<Object[]> sumByMethodName(@Param("start") Date start, @Param("end") Date end);
}
