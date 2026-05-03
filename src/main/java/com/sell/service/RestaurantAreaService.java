package com.sell.service;

import com.sell.dataobject.RestaurantArea;

import java.util.List;

public interface RestaurantAreaService {

    List<RestaurantArea> findAll();

    RestaurantArea findOne(Integer areaId);

    RestaurantArea save(RestaurantArea area);

    /** 删除区域 (区域下还有桌台时禁止删除). */
    void delete(Integer areaId);
}
