package com.sell.repository;

import com.sell.dataobject.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Integer> {
    List<TimeSlot> findAllByOrderBySortOrderAscSlotIdAsc();
}
