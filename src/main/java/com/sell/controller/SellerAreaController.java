package com.sell.controller;

import com.sell.dataobject.RestaurantArea;
import com.sell.exception.SellException;
import com.sell.form.AreaForm;
import com.sell.repository.RestaurantTableRepository;
import com.sell.service.RestaurantAreaService;
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

/**
 * 桌台区域 - PRD 5.1
 */
@Controller
@RequestMapping("/seller/area")
public class SellerAreaController {

    @Autowired
    private RestaurantAreaService areaService;

    @Autowired
    private RestaurantTableRepository tableRepository;

    @GetMapping("/list")
    public ModelAndView list(Map<String, Object> map) {
        List<RestaurantArea> areaList = areaService.findAll();
        Map<Integer, Long> tableCountMap = new HashMap<>();
        for (RestaurantArea a : areaList) {
            tableCountMap.put(a.getAreaId(), tableRepository.countByAreaId(a.getAreaId()));
        }
        map.put("areaList", areaList);
        map.put("tableCountMap", tableCountMap);
        return new ModelAndView("area/list", map);
    }

    @GetMapping("/index")
    public ModelAndView index(@RequestParam(value = "areaId", required = false) Integer areaId,
                              Map<String, Object> map) {
        if (areaId != null) {
            map.put("area", areaService.findOne(areaId));
        }
        return new ModelAndView("area/index", map);
    }

    @PostMapping("/save")
    public ModelAndView save(@Valid AreaForm form,
                             BindingResult bindingResult,
                             Map<String, Object> map) {
        if (bindingResult.hasErrors()) {
            map.put("msg", bindingResult.getFieldError().getDefaultMessage());
            map.put("url", "/sell/seller/area/index");
            return new ModelAndView("common/error", map);
        }
        try {
            RestaurantArea area = (form.getAreaId() != null)
                    ? areaService.findOne(form.getAreaId())
                    : new RestaurantArea();
            if (area == null) area = new RestaurantArea();
            BeanUtils.copyProperties(form, area);
            areaService.save(area);
        } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/area/list");
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/area/list");
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/delete")
    public ModelAndView delete(@RequestParam("areaId") Integer areaId,
                               Map<String, Object> map) {
        try {
            areaService.delete(areaId);
        } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/area/list");
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/area/list");
        return new ModelAndView("common/success", map);
    }
}
