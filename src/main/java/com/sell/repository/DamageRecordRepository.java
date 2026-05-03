package com.sell.repository;

import com.sell.dataobject.DamageRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DamageRecordRepository extends JpaRepository<DamageRecord, Long> {
    List<DamageRecord> findAllByOrderByOccurDateDescRecordIdDesc();
}
