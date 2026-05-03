package com.sell.repository;

import com.sell.dataobject.AddonGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddonGroupRepository extends JpaRepository<AddonGroup, Integer> {
    List<AddonGroup> findAllByOrderBySortOrderAscGroupIdAsc();
}
