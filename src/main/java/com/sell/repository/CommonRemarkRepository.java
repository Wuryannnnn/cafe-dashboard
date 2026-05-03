package com.sell.repository;

import com.sell.dataobject.CommonRemark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommonRemarkRepository extends JpaRepository<CommonRemark, Integer> {
    List<CommonRemark> findAllByOrderBySortOrderAscRemarkIdAsc();
}
