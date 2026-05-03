package com.sell.repository;

import com.sell.dataobject.RechargePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RechargePlanRepository extends JpaRepository<RechargePlan, Integer> {
    List<RechargePlan> findAllByOrderBySortOrderAscPlanIdAsc();
    List<RechargePlan> findByEnabledTrueOrderBySortOrderAsc();
}
