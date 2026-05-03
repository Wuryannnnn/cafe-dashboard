package com.sell.service.impl;

import com.sell.dataobject.RestaurantArea;
import com.sell.exception.SellException;
import com.sell.repository.RestaurantAreaRepository;
import com.sell.repository.RestaurantTableRepository;
import com.sell.service.RestaurantAreaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class RestaurantAreaServiceImpl implements RestaurantAreaService {

    @Autowired
    private RestaurantAreaRepository areaRepository;

    @Autowired
    private RestaurantTableRepository tableRepository;

    @Override
    public List<RestaurantArea> findAll() {
        return areaRepository.findAllByOrderBySortOrderAscAreaIdAsc();
    }

    @Override
    public RestaurantArea findOne(Integer areaId) {
        if (areaId == null) return null;
        return areaRepository.findById(areaId).orElse(null);
    }

    @Override
    public RestaurantArea save(RestaurantArea area) {
        Date now = new Date();
        if (area.getCreateTime() == null) {
            area.setCreateTime(now);
        }
        area.setUpdateTime(now);
        return areaRepository.save(area);
    }

    @Override
    public void delete(Integer areaId) {
        if (areaId == null) {
            throw new SellException(1, "区域id不能为空");
        }
        long tableCount = tableRepository.countByAreaId(areaId);
        if (tableCount > 0) {
            throw new SellException(1, "该区域下还有 " + tableCount + " 张桌台, 请先删除或迁移桌台");
        }
        areaRepository.deleteById(areaId);
    }
}
