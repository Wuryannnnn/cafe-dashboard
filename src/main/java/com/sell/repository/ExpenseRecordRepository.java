package com.sell.repository;

import com.sell.dataobject.ExpenseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface ExpenseRecordRepository extends JpaRepository<ExpenseRecord, Long> {

    List<ExpenseRecord> findAllByOrderByOccurDateDescRecordIdDesc();

    @Query("SELECT e.recordType, COALESCE(SUM(e.amount), 0) FROM ExpenseRecord e WHERE e.occurDate >= :start AND e.occurDate < :end GROUP BY e.recordType")
    List<Object[]> sumByType(@Param("start") Date start, @Param("end") Date end);
}
