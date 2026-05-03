package com.sell.repository;

import com.sell.dataobject.MemberLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberLevelRepository extends JpaRepository<MemberLevel, Integer> {

    List<MemberLevel> findAllByOrderBySortOrderAscLevelIdAsc();
}
