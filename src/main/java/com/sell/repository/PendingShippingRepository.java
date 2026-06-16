package com.sell.repository;

import com.sell.dataobject.PendingShipping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface PendingShippingRepository extends JpaRepository<PendingShipping, Long> {
    List<PendingShipping> findByStatusAndNextRetryAtLessThanEqual(String status, Date before);
}
