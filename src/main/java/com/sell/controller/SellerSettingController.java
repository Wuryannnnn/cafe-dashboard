package com.sell.controller;

import com.sell.dataobject.CommonRemark;
import com.sell.dataobject.OperationLog;
import com.sell.dataobject.ShopConfig;
import com.sell.repository.CommonRemarkRepository;
import com.sell.repository.OperationLogRepository;
import com.sell.repository.ShopConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统设置 - PRD 12
 * 门店信息 (12.1) + 常用备注 (12.3) + 业务开关 (12.4) + 操作日志 (12.5)
 */
@Controller
@RequestMapping("/seller/setting")
public class SellerSettingController {

    private static final String[] STORE_KEYS = {
            "store.name", "store.address", "store.phone",
            "store.openHours", "store.logo"
    };
    private static final String[] SWITCH_KEYS = {
            "switch.qrOrder", "switch.autoAcceptOrder", "switch.memberRegister", "switch.payBeforeServe"
    };

    @Autowired private ShopConfigRepository configRepo;
    @Autowired private CommonRemarkRepository remarkRepo;
    @Autowired private OperationLogRepository logRepo;

    /* ---------- 门店信息 ---------- */
    @GetMapping("/store")
    public ModelAndView store(Map<String, Object> map) {
        Map<String, String> values = readKeys(STORE_KEYS);
        map.put("store", values);
        return new ModelAndView("setting/store", map);
    }

    @PostMapping("/store/save")
    public ModelAndView storeSave(@RequestParam Map<String, String> params, Map<String, Object> map) {
        for (String k : STORE_KEYS) {
            if (params.containsKey(k)) saveCfg(k, params.get(k));
        }
        map.put("url", "/sell/seller/setting/store");
        return new ModelAndView("common/success", map);
    }

    /* ---------- 常用备注 ---------- */
    @GetMapping("/remark")
    public ModelAndView remark(Map<String, Object> map) {
        map.put("remarks", remarkRepo.findAllByOrderBySortOrderAscRemarkIdAsc());
        return new ModelAndView("setting/remark", map);
    }

    @PostMapping("/remark/save")
    public ModelAndView remarkSave(@RequestParam(value = "remarkId", required = false) Integer remarkId,
                                   @RequestParam("text") String text,
                                   @RequestParam(value = "sortOrder", defaultValue = "0") Integer sortOrder,
                                   Map<String, Object> map) {
        CommonRemark r = remarkId != null
                ? remarkRepo.findById(remarkId).orElse(new CommonRemark()) : new CommonRemark();
        r.setText(text);
        r.setSortOrder(sortOrder);
        Date now = new Date();
        if (r.getCreateTime() == null) r.setCreateTime(now);
        r.setUpdateTime(now);
        remarkRepo.save(r);
        map.put("url", "/sell/seller/setting/remark");
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/remark/delete")
    public ModelAndView remarkDelete(@RequestParam("remarkId") Integer remarkId, Map<String, Object> map) {
        remarkRepo.deleteById(remarkId);
        map.put("url", "/sell/seller/setting/remark");
        return new ModelAndView("common/success", map);
    }

    /* ---------- 业务开关 ---------- */
    @GetMapping("/switch")
    public ModelAndView swit(Map<String, Object> map) {
        map.put("switches", readKeys(SWITCH_KEYS));
        return new ModelAndView("setting/switch", map);
    }

    @PostMapping("/switch/save")
    public ModelAndView switchSave(@RequestParam Map<String, String> params, Map<String, Object> map) {
        for (String k : SWITCH_KEYS) {
            saveCfg(k, params.getOrDefault(k, "false"));
        }
        map.put("url", "/sell/seller/setting/switch");
        return new ModelAndView("common/success", map);
    }

    /* ---------- 操作日志 ---------- */
    @GetMapping("/log")
    public ModelAndView log(@RequestParam(value = "page", defaultValue = "1") Integer page,
                            @RequestParam(value = "type", required = false) String type,
                            Map<String, Object> map) {
        Page<OperationLog> result;
        if (type != null && !type.isEmpty()) {
            result = logRepo.findByOperationType(type,
                    PageRequest.of(page - 1, 30, Sort.by(Sort.Direction.DESC, "logId")));
        } else {
            result = logRepo.findAll(PageRequest.of(page - 1, 30, Sort.by(Sort.Direction.DESC, "logId")));
        }
        map.put("logPage", result);
        map.put("currentPage", page);
        map.put("type", type);
        return new ModelAndView("setting/log", map);
    }

    private Map<String, String> readKeys(String[] keys) {
        Map<String, String> values = new HashMap<>();
        for (String k : keys) {
            ShopConfig c = configRepo.findById(k).orElse(null);
            values.put(k, c != null ? c.getConfigValue() : "");
        }
        return values;
    }

    private void saveCfg(String key, String value) {
        ShopConfig c = configRepo.findById(key).orElse(new ShopConfig());
        c.setConfigKey(key);
        c.setConfigValue(value);
        configRepo.save(c);
    }
}
