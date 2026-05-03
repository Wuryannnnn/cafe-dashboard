package com.sell.repository;

import com.sell.dataobject.PointsRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointsRecordRepository extends JpaRepository<PointsRecord, Long> {
    List<PointsRecord> findByMemberIdOrderByRecordIdDesc(Integer memberId);
}
