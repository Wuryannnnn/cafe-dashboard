package com.sell.repository;

import com.sell.dataobject.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Integer> {

    List<RestaurantTable> findAllByOrderBySortOrderAscTableIdAsc();

    List<RestaurantTable> findByAreaIdOrderBySortOrderAscTableIdAsc(Integer areaId);

    RestaurantTable findByTableCode(String tableCode);

    long countByAreaId(Integer areaId);
}
