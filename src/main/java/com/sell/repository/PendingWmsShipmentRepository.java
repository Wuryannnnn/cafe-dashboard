package com.sell.repository;

import com.sell.dataobject.PendingWmsShipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface PendingWmsShipmentRepository extends JpaRepository<PendingWmsShipment, Long> {

    /** 到了重试时间且仍处于 pending 状态的条目. */
    List<PendingWmsShipment> findByStatusAndNextRetryAtLessThanEqual(String status, Date now);

    long countByStatus(String status);
}
