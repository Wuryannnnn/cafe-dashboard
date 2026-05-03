package com.sell.controller;

import com.sell.dataobject.RestaurantArea;
import com.sell.dataobject.RestaurantTable;
import com.sell.exception.SellException;
import com.sell.form.TableForm;
import com.sell.service.RestaurantAreaService;
import com.sell.service.RestaurantTableService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 桌台 - PRD 5.2
 */
@Controller
@RequestMapping("/seller/table")
public class SellerTableController {

    @Autowired
    private RestaurantTableService tableService;

    @Autowired
    private RestaurantAreaService areaService;

    @GetMapping("/list")
    public ModelAndView list(@RequestParam(value = "areaId", required = false) Integer areaId,
                             Map<String, Object> map) {
        List<RestaurantArea> areaList = areaService.findAll();
        List<RestaurantTable> tableList = tableService.findByAreaId(areaId);
        Set<Integer> occupied = tableService.getOccupiedTableIds();

        // areaId -> areaName 映射
        Map<Integer, String> areaNameMap = new HashMap<>();
        for (RestaurantArea a : areaList) {
            areaNameMap.put(a.getAreaId(), a.getAreaName());
        }

        map.put("areaList", areaList);
        map.put("tableList", tableList);
        map.put("currentAreaId", areaId);
        map.put("occupied", occupied);
        map.put("areaNameMap", areaNameMap);
        return new ModelAndView("table/list", map);
    }

    @GetMapping("/index")
    public ModelAndView index(@RequestParam(value = "tableId", required = false) Integer tableId,
                              Map<String, Object> map) {
        if (tableId != null) {
            map.put("table", tableService.findOne(tableId));
        }
        map.put("areaList", areaService.findAll());
        return new ModelAndView("table/index", map);
    }

    @PostMapping("/save")
    public ModelAndView save(@Valid TableForm form,
                             BindingResult bindingResult,
                             Map<String, Object> map) {
        if (bindingResult.hasErrors()) {
            map.put("msg", bindingResult.getFieldError().getDefaultMessage());
            map.put("url", "/sell/seller/table/index");
            return new ModelAndView("common/error", map);
        }
        try {
            RestaurantTable table = (form.getTableId() != null)
                    ? tableService.findOne(form.getTableId())
                    : new RestaurantTable();
            if (table == null) table = new RestaurantTable();
            BeanUtils.copyProperties(form, table);
            tableService.save(table);
        } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/table/list");
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/table/list");
        return new ModelAndView("common/success", map);
    }

    @PostMapping("/batchCreate")
    public ModelAndView batchCreate(@RequestParam("areaId") Integer areaId,
                                    @RequestParam(value = "prefix", defaultValue = "") String prefix,
                                    @RequestParam("startNum") Integer startNum,
                                    @RequestParam("endNum") Integer endNum,
                                    @RequestParam(value = "digitWidth", defaultValue = "1") Integer digitWidth,
                                    @RequestParam(value = "seatCount", defaultValue = "4") Integer seatCount,
                                    Map<String, Object> map) {
        try {
            int created = tableService.batchCreate(areaId, prefix, startNum, endNum, digitWidth, seatCount);
            map.put("msg", "成功创建 " + created + " 张桌台");
        } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/table/list");
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/table/list");
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/delete")
    public ModelAndView delete(@RequestParam("tableId") Integer tableId,
                               Map<String, Object> map) {
        try {
            tableService.delete(tableId);
        } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/table/list");
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/table/list");
        return new ModelAndView("common/success", map);
    }

    @PostMapping("/sort")
    @ResponseBody
    public Map<String, Object> sort(@RequestBody List<Integer> tableIdsInOrder) {
        Map<String, Object> resp = new HashMap<>();
        try {
            tableService.resort(tableIdsInOrder);
            resp.put("code", 0);
            resp.put("msg", "排序更新成功");
        } catch (Exception e) {
            resp.put("code", 1);
            resp.put("msg", e.getMessage());
        }
        return resp;
    }
}
