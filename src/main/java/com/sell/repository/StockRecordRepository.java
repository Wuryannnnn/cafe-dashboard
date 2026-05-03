package com.sell.repository;

import com.sell.dataobject.StockRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRecordRepository extends JpaRepository<StockRecord, Long> {
    Page<StockRecord> findByProductId(String productId, Pageable pageable);
}
