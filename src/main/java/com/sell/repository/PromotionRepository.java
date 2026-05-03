package com.sell.repository;

import com.sell.dataobject.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Integer> {
    List<Promotion> findAllByOrderBySortOrderAscPromotionIdDesc();
}
