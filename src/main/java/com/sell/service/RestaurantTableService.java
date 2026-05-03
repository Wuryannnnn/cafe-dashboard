package com.sell.service;

import com.sell.dataobject.RestaurantTable;

import java.util.List;
import java.util.Set;

public interface RestaurantTableService {

    List<RestaurantTable> findAll();

    List<RestaurantTable> findByAreaId(Integer areaId);

    RestaurantTable findOne(Integer tableId);

    RestaurantTable findByTableCode(String tableCode);

    RestaurantTable save(RestaurantTable table);

    /** 批量按规则新增. 返回成功创建的数量. */
    int batchCreate(Integer areaId, String prefix, int startNum, int endNum, int digitWidth, int seatCount);

    /** 删除桌台 (使用中的不可删). */
    void delete(Integer tableId);

    /** 排序: 按给定 tableId 顺序更新 sortOrder. */
    void resort(List<Integer> tableIdsInOrder);

    /** 当前被未完结订单占用的 tableId 集合. */
    Set<Integer> getOccupiedTableIds();
}
