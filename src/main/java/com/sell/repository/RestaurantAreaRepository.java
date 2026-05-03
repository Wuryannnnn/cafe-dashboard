package com.sell.repository;

import com.sell.dataobject.RestaurantArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantAreaRepository extends JpaRepository<RestaurantArea, Integer> {

    List<RestaurantArea> findAllByOrderBySortOrderAscAreaIdAsc();
}
