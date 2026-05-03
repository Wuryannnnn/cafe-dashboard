package com.sell.controller;

import com.sell.dataobject.RechargePlan;
import com.sell.repository.RechargePlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;
import java.util.Map;

/** 充值方案 - PRD 8.5 */
@Controller
@RequestMapping("/seller/rechargePlan")
public class SellerRechargePlanController {

    @Autowired
    private RechargePlanRepository repo;

    @GetMapping("/list")
    public ModelAndView list(Map<String, Object> map) {
        map.put("plans", repo.findAllByOrderBySortOrderAscPlanIdAsc());
        return new ModelAndView("member/recharge_plan_list", map);
    }

    @GetMapping("/index")
    public ModelAndView index(@RequestParam(value = "planId", required = false) Integer planId,
                              Map<String, Object> map) {
        if (planId != null) map.put("plan", repo.findById(planId).orElse(null));
        return new ModelAndView("member/recharge_plan_index", map);
    }

    @PostMapping("/save")
    public ModelAndView save(RechargePlan form, Map<String, Object> map) {
        RechargePlan p = form.getPlanId() != null
                ? repo.findById(form.getPlanId()).orElse(new RechargePlan())
                : new RechargePlan();
        p.setPlanName(form.getPlanName());
        p.setPayAmount(form.getPayAmount());
        p.setGiveAmount(form.getGiveAmount());
        p.setEnabled(form.getEnabled() != null ? form.getEnabled() : true);
        p.setSortOrder(form.getSortOrder() != null ? form.getSortOrder() : 0);
        Date now = new Date();
        if (p.getCreateTime() == null) p.setCreateTime(now);
        p.setUpdateTime(now);
        repo.save(p);
        map.put("url", "/sell/seller/rechargePlan/list");
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/delete")
    public ModelAndView delete(@RequestParam("planId") Integer planId, Map<String, Object> map) {
        repo.deleteById(planId);
        map.put("url", "/sell/seller/rechargePlan/list");
        return new ModelAndView("common/success", map);
    }
}
