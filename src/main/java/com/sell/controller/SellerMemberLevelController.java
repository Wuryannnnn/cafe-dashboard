package com.sell.controller;

import com.sell.dataobject.MemberLevel;
import com.sell.repository.MemberLevelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;
import java.util.Map;

/** 会员等级 - PRD 8.4 */
@Controller
@RequestMapping("/seller/level")
public class SellerMemberLevelController {

    @Autowired
    private MemberLevelRepository repo;

    @GetMapping("/list")
    public ModelAndView list(Map<String, Object> map) {
        map.put("levels", repo.findAllByOrderBySortOrderAscLevelIdAsc());
        return new ModelAndView("member/level_list", map);
    }

    @GetMapping("/index")
    public ModelAndView index(@RequestParam(value = "levelId", required = false) Integer levelId,
                              Map<String, Object> map) {
        if (levelId != null) map.put("level", repo.findById(levelId).orElse(null));
        return new ModelAndView("member/level_index", map);
    }

    @PostMapping("/save")
    public ModelAndView save(MemberLevel form, Map<String, Object> map) {
        MemberLevel l = form.getLevelId() != null
                ? repo.findById(form.getLevelId()).orElse(new MemberLevel())
                : new MemberLevel();
        l.setLevelName(form.getLevelName());
        l.setUpgradeAmount(form.getUpgradeAmount());
        l.setUpgradeCount(form.getUpgradeCount());
        l.setDiscountRate(form.getDiscountRate() != null ? form.getDiscountRate() : 100);
        l.setSortOrder(form.getSortOrder() != null ? form.getSortOrder() : 0);
        Date now = new Date();
        if (l.getCreateTime() == null) l.setCreateTime(now);
        l.setUpdateTime(now);
        repo.save(l);
        map.put("url", "/sell/seller/level/list");
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/delete")
    public ModelAndView delete(@RequestParam("levelId") Integer levelId, Map<String, Object> map) {
        repo.deleteById(levelId);
        map.put("url", "/sell/seller/level/list");
        return new ModelAndView("common/success", map);
    }
}
