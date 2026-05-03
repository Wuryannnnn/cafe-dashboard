package com.sell.service.impl;

import com.sell.dataobject.OrderMaster;
import com.sell.dataobject.RestaurantTable;
import com.sell.enums.OrderStatusEnum;
import com.sell.exception.SellException;
import com.sell.repository.OrderMasterRepository;
import com.sell.repository.RestaurantTableRepository;
import com.sell.service.RestaurantTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RestaurantTableServiceImpl implements RestaurantTableService {

    @Autowired
    private RestaurantTableRepository tableRepository;

    @Autowired
    private OrderMasterRepository orderMasterRepository;

    @Override
    public List<RestaurantTable> findAll() {
        return tableRepository.findAllByOrderBySortOrderAscTableIdAsc();
    }

    @Override
    public List<RestaurantTable> findByAreaId(Integer areaId) {
        if (areaId == null) return findAll();
        return tableRepository.findByAreaIdOrderBySortOrderAscTableIdAsc(areaId);
    }

    @Override
    public RestaurantTable findOne(Integer tableId) {
        if (tableId == null) return null;
        return tableRepository.findById(tableId).orElse(null);
    }

    @Override
    public RestaurantTable findByTableCode(String tableCode) {
        if (tableCode == null || tableCode.isEmpty()) return null;
        return tableRepository.findByTableCode(tableCode);
    }

    @Override
    public RestaurantTable save(RestaurantTable table) {
        if (table.getTableCode() == null || table.getTableCode().trim().isEmpty()) {
            throw new SellException(1, "桌号不能为空");
        }
        if (table.getAreaId() == null) {
            throw new SellException(1, "所属区域不能为空");
        }
        // 桌号唯一校验 (新增/改名时)
        RestaurantTable exist = tableRepository.findByTableCode(table.getTableCode());
        if (exist != null && !exist.getTableId().equals(table.getTableId())) {
            throw new SellException(1, "桌号 " + table.getTableCode() + " 已存在");
        }
        Date now = new Date();
        if (table.getCreateTime() == null) {
            table.setCreateTime(now);
        }
        table.setUpdateTime(now);
        if (table.getEnabled() == null) {
            table.setEnabled(true);
        }
        if (table.getSeatCount() == null || table.getSeatCount() < 1) {
            table.setSeatCount(4);
        }
        if (table.getSortOrder() == null) {
            table.setSortOrder(0);
        }
        return tableRepository.save(table);
    }

    @Override
    @Transactional
    public int batchCreate(Integer areaId, String prefix, int startNum, int endNum, int digitWidth, int seatCount) {
        if (areaId == null) {
            throw new SellException(1, "所属区域不能为空");
        }
        if (endNum < startNum) {
            throw new SellException(1, "结束编号必须 >= 起始编号");
        }
        int created = 0;
        Date now = new Date();
        String fmt = "%0" + Math.max(1, digitWidth) + "d";
        for (int n = startNum; n <= endNum; n++) {
            String code = (prefix == null ? "" : prefix) + String.format(fmt, n);
            if (tableRepository.findByTableCode(code) != null) {
                continue; // 跳过已存在
            }
            RestaurantTable t = new RestaurantTable();
            t.setAreaId(areaId);
            t.setTableCode(code);
            t.setSeatCount(seatCount > 0 ? seatCount : 4);
            t.setSortOrder(n);
            t.setEnabled(true);
            t.setCreateTime(now);
            t.setUpdateTime(now);
            tableRepository.save(t);
            created++;
        }
        return created;
    }

    @Override
    public void delete(Integer tableId) {
        if (tableId == null) {
            throw new SellException(1, "桌台id不能为空");
        }
        if (getOccupiedTableIds().contains(tableId)) {
            throw new SellException(1, "该桌正在使用中, 请先完结订单再删除");
        }
        tableRepository.deleteById(tableId);
    }

    @Override
    @Transactional
    public void resort(List<Integer> tableIdsInOrder) {
        if (tableIdsInOrder == null) return;
        for (int i = 0; i < tableIdsInOrder.size(); i++) {
            Integer id = tableIdsInOrder.get(i);
            RestaurantTable t = tableRepository.findById(id).orElse(null);
            if (t != null) {
                t.setSortOrder(i);
                t.setUpdateTime(new Date());
                tableRepository.save(t);
            }
        }
    }

    @Override
    public Set<Integer> getOccupiedTableIds() {
        // 未完结状态: 新订单/制作中/待取餐
        List<Integer> activeStatuses = Arrays.asList(
                OrderStatusEnum.NEW.getCode(),
                OrderStatusEnum.MAKING.getCode(),
                OrderStatusEnum.READY.getCode());
        List<OrderMaster> orders = orderMasterRepository.findByOrderStatusInAndTableIdIsNotNull(activeStatuses);
        return orders.stream()
                .map(OrderMaster::getTableId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
