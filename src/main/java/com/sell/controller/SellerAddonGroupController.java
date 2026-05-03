package com.sell.controller;

import com.sell.dataobject.AddonGroup;
import com.sell.dataobject.ProductAddon;
import com.sell.repository.AddonGroupRepository;
import com.sell.repository.ProductAddonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;
import java.util.List;
import java.util.Map;

/** 菜品属性组 + 属性值 - PRD 3.3 */
@Controller
@RequestMapping("/seller/addonGroup")
public class SellerAddonGroupController {

    @Autowired
    private AddonGroupRepository groupRepo;

    @Autowired
    private ProductAddonRepository addonRepo;

    @GetMapping("/list")
    public ModelAndView list(Map<String, Object> map) {
        List<AddonGroup> groups = groupRepo.findAllByOrderBySortOrderAscGroupIdAsc();
        // 每组下的属性值
        Map<Integer, List<ProductAddon>> groupItems = new java.util.HashMap<>();
        List<ProductAddon> all = addonRepo.findAll();
        for (ProductAddon a : all) {
            groupItems.computeIfAbsent(a.getGroupId() == null ? 0 : a.getGroupId(), k -> new java.util.ArrayList<>()).add(a);
        }
        map.put("groups", groups);
        map.put("groupItems", groupItems);
        map.put("ungrouped", groupItems.getOrDefault(0, java.util.Collections.emptyList()));
        return new ModelAndView("addon/list", map);
    }

    @PostMapping("/save")
    public ModelAndView save(AddonGroup form, Map<String, Object> map) {
        AddonGroup g = form.getGroupId() != null
                ? groupRepo.findById(form.getGroupId()).orElse(new AddonGroup())
                : new AddonGroup();
        g.setGroupName(form.getGroupName());
        g.setRequired(form.getRequired() != null ? form.getRequired() : false);
        g.setMultipleChoice(form.getMultipleChoice() != null ? form.getMultipleChoice() : false);
        g.setSortOrder(form.getSortOrder() != null ? form.getSortOrder() : 0);
        Date now = new Date();
        if (g.getCreateTime() == null) g.setCreateTime(now);
        g.setUpdateTime(now);
        groupRepo.save(g);
        map.put("url", "/sell/seller/addonGroup/list");
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/delete")
    public ModelAndView delete(@RequestParam("groupId") Integer groupId, Map<String, Object> map) {
        groupRepo.deleteById(groupId);
        map.put("url", "/sell/seller/addonGroup/list");
        return new ModelAndView("common/success", map);
    }

    /** 属性值 (Addon) 的快速保存. */
    @PostMapping("/addonSave")
    public ModelAndView addonSave(@RequestParam(value = "addonId", required = false) String addonId,
                                  @RequestParam("addonName") String addonName,
                                  @RequestParam("addonPrice") java.math.BigDecimal price,
                                  @RequestParam("groupId") Integer groupId,
                                  Map<String, Object> map) {
        ProductAddon a = addonId != null && !addonId.isEmpty()
                ? addonRepo.findById(addonId).orElse(new ProductAddon())
                : new ProductAddon();
        if (a.getAddonId() == null) {
            a.setAddonId(com.sell.utils.KeyUtil.genUniqueKey());
        }
        a.setAddonName(addonName);
        a.setAddonPrice(price);
        a.setGroupId(groupId);
        Date now = new Date();
        if (a.getCreateTime() == null) a.setCreateTime(now);
        a.setUpdateTime(now);
        addonRepo.save(a);
        map.put("url", "/sell/seller/addonGroup/list");
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/addonDelete")
    public ModelAndView addonDelete(@RequestParam("addonId") String addonId, Map<String, Object> map) {
        addonRepo.deleteById(addonId);
        map.put("url", "/sell/seller/addonGroup/list");
        return new ModelAndView("common/success", map);
    }
}
