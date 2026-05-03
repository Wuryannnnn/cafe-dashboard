package com.sell.controller;

import com.sell.dataobject.Promotion;
import com.sell.enums.PromotionTypeEnum;
import com.sell.repository.PromotionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;
import java.util.Map;

/** 营销活动 - PRD 8.7 */
@Controller
@RequestMapping("/seller/promotion")
public class SellerPromotionController {

    @Autowired
    private PromotionRepository repo;

    @GetMapping("/list")
    public ModelAndView list(Map<String, Object> map) {
        map.put("promotions", repo.findAllByOrderBySortOrderAscPromotionIdDesc());
        map.put("types", PromotionTypeEnum.values());
        return new ModelAndView("promotion/list", map);
    }

    @GetMapping("/index")
    public ModelAndView index(@RequestParam(value = "promotionId", required = false) Integer promotionId,
                              Map<String, Object> map) {
        if (promotionId != null) map.put("promotion", repo.findById(promotionId).orElse(null));
        map.put("types", PromotionTypeEnum.values());
        return new ModelAndView("promotion/index", map);
    }

    @PostMapping("/save")
    public ModelAndView save(@RequestParam(value = "promotionId", required = false) Integer promotionId,
                             @RequestParam("promotionName") String promotionName,
                             @RequestParam("promotionType") Integer promotionType,
                             @RequestParam(value = "configJson", required = false, defaultValue = "{}") String configJson,
                             @RequestParam(value = "startTime", required = false)
                             @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date startTime,
                             @RequestParam(value = "endTime", required = false)
                             @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date endTime,
                             @RequestParam(value = "enabled", defaultValue = "true") Boolean enabled,
                             @RequestParam(value = "sortOrder", defaultValue = "0") Integer sortOrder,
                             Map<String, Object> map) {
        Promotion p = promotionId != null
                ? repo.findById(promotionId).orElse(new Promotion())
                : new Promotion();
        p.setPromotionName(promotionName);
        p.setPromotionType(promotionType);
        p.setConfigJson(configJson);
        p.setStartTime(startTime);
        p.setEndTime(endTime);
        p.setEnabled(enabled);
        p.setSortOrder(sortOrder);
        Date now = new Date();
        if (p.getCreateTime() == null) p.setCreateTime(now);
        p.setUpdateTime(now);
        repo.save(p);
        map.put("url", "/sell/seller/promotion/list");
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/delete")
    public ModelAndView delete(@RequestParam("promotionId") Integer promotionId, Map<String, Object> map) {
        repo.deleteById(promotionId);
        map.put("url", "/sell/seller/promotion/list");
        return new ModelAndView("common/success", map);
    }
}
