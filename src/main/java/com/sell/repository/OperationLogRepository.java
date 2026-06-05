package com.sell.repository;

import com.sell.dataobject.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    Page<OperationLog> findByOperationType(String operationType, Pageable pageable);

    /** 操作日志筛选: 日期区间 [start, end) + 关键字(操作员/类型/说明 模糊), 各项为空即不限. */
    @Query("SELECT o FROM OperationLog o WHERE "
            + "(:start IS NULL OR o.createTime >= :start) AND "
            + "(:end IS NULL OR o.createTime < :end) AND "
            + "(:kw IS NULL OR o.operator LIKE CONCAT('%', :kw, '%') "
            + "OR o.operationType LIKE CONCAT('%', :kw, '%') "
            + "OR o.detail LIKE CONCAT('%', :kw, '%'))")
    Page<OperationLog> search(@Param("start") Date start,
                              @Param("end") Date end,
                              @Param("kw") String kw,
                              Pageable pageable);
}
