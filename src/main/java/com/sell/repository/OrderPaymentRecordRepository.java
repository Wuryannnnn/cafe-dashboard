package com.sell.repository;

import com.sell.dataobject.OrderPaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface OrderPaymentRecordRepository extends JpaRepository<OrderPaymentRecord, Long> {

    List<OrderPaymentRecord> findByOrderIdOrderByPaymentIdAsc(String orderId);

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM OrderPaymentRecord o WHERE o.orderId = :orderId")
    BigDecimal sumByOrderId(@Param("orderId") String orderId);
}
