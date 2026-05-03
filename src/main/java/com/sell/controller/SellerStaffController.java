package com.sell.controller;

import com.sell.dataobject.Staff;
import com.sell.enums.StaffRoleEnum;
import com.sell.exception.SellException;
import com.sell.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/** 员工管理 - PRD 10 */
@Controller
@RequestMapping("/seller/staff")
public class SellerStaffController {

    @Autowired
    private StaffService service;

    @GetMapping("/list")
    public ModelAndView list(Map<String, Object> map) {
        map.put("staffList", service.findAll());
        map.put("roles", StaffRoleEnum.values());
        return new ModelAndView("staff/list", map);
    }

    @GetMapping("/index")
    public ModelAndView index(@RequestParam(value = "staffId", required = false) Integer staffId,
                              Map<String, Object> map) {
        if (staffId != null) map.put("staff", service.findOne(staffId));
        map.put("roles", StaffRoleEnum.values());
        return new ModelAndView("staff/index", map);
    }

    @PostMapping("/save")
    public ModelAndView save(Staff form, Map<String, Object> map) {
        try {
            Staff s = form.getStaffId() != null ? service.findOne(form.getStaffId()) : new Staff();
            if (s == null) s = new Staff();
            s.setUsername(form.getUsername());
            if (form.getPassword() != null && !form.getPassword().isEmpty()) {
                s.setPassword(form.getPassword());
            }
            s.setName(form.getName());
            s.setPhone(form.getPhone());
            s.setRole(form.getRole());
            s.setEnabled(form.getEnabled() != null ? form.getEnabled() : true);
            service.save(s);
        } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/staff/list");
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/staff/list");
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/delete")
    public ModelAndView delete(@RequestParam("staffId") Integer staffId, Map<String, Object> map) {
        service.delete(staffId);
        map.put("url", "/sell/seller/staff/list");
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/toggle")
    public ModelAndView toggle(@RequestParam("staffId") Integer staffId, Map<String, Object> map) {
        try { service.toggleEnabled(staffId); } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/staff/list");
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/staff/list");
        return new ModelAndView("common/success", map);
    }
}
