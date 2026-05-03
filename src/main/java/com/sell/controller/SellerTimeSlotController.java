package com.sell.controller;

import com.sell.dataobject.TimeSlot;
import com.sell.repository.TimeSlotRepository;
import com.sell.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;
import java.util.List;
import java.util.Map;

/** 时段菜单 - PRD 3.4 */
@Controller
@RequestMapping("/seller/timeslot")
public class SellerTimeSlotController {

    @Autowired
    private TimeSlotRepository repo;

    @Autowired
    private ProductService productService;

    @GetMapping("/list")
    public ModelAndView list(Map<String, Object> map) {
        map.put("slots", repo.findAllByOrderBySortOrderAscSlotIdAsc());
        return new ModelAndView("timeslot/list", map);
    }

    @GetMapping("/index")
    public ModelAndView index(@RequestParam(value = "slotId", required = false) Integer slotId,
                              Map<String, Object> map) {
        if (slotId != null) map.put("slot", repo.findById(slotId).orElse(null));
        map.put("products", productService.findUpAll());
        return new ModelAndView("timeslot/index", map);
    }

    @PostMapping("/save")
    public ModelAndView save(TimeSlot form,
                             @RequestParam(value = "selectedProducts", required = false) List<String> selectedProducts,
                             Map<String, Object> map) {
        TimeSlot s = form.getSlotId() != null
                ? repo.findById(form.getSlotId()).orElse(new TimeSlot()) : new TimeSlot();
        s.setSlotName(form.getSlotName());
        s.setStartTime(form.getStartTime());
        s.setEndTime(form.getEndTime());
        s.setEnabled(form.getEnabled() != null ? form.getEnabled() : true);
        s.setSortOrder(form.getSortOrder() != null ? form.getSortOrder() : 0);
        s.setProductIds(selectedProducts == null ? "" : String.join(",", selectedProducts));
        Date now = new Date();
        if (s.getCreateTime() == null) s.setCreateTime(now);
        s.setUpdateTime(now);
        repo.save(s);
        map.put("url", "/sell/seller/timeslot/list");
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/delete")
    public ModelAndView delete(@RequestParam("slotId") Integer slotId, Map<String, Object> map) {
        repo.deleteById(slotId);
        map.put("url", "/sell/seller/timeslot/list");
        return new ModelAndView("common/success", map);
    }
}
