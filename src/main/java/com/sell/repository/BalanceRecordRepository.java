package com.sell.repository;

import com.sell.dataobject.BalanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BalanceRecordRepository extends JpaRepository<BalanceRecord, Long> {
    List<BalanceRecord> findByMemberIdOrderByRecordIdDesc(Integer memberId);
}
