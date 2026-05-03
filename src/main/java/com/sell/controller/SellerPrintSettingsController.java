package com.sell.controller;

import com.sell.dataobject.ShopConfig;
import com.sell.repository.ShopConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/**
 * 打印管理设置 - PRD 12.2
 * - 票据样式: print.header / print.footer / print.fontSize / print.showQrCode
 * - 业务设置: print.onOrderCreate / print.onPay / print.onRefund / print.onItemCancel
 * (复用 ShopConfig KV 存储)
 */
@Controller
@RequestMapping("/seller/printSettings")
public class SellerPrintSettingsController {

    private static final String[] KEYS = {
            "print.header",
            "print.footer",
            "print.fontSize",
            "print.showQrCode",
            "print.onOrderCreate",
            "print.onPay",
            "print.onRefund",
            "print.onItemCancel"
    };

    @Autowired
    private ShopConfigRepository repository;

    @GetMapping("")
    public ModelAndView page(Map<String, Object> map) {
        for (String key : KEYS) {
            ShopConfig c = repository.findById(key).orElse(null);
            map.put(key.replace(".", "_"), c != null ? c.getConfigValue() : "");
        }
        return new ModelAndView("printer/settings", map);
    }

    @PostMapping("/save")
    public ModelAndView save(@RequestParam(name = "print.header", required = false, defaultValue = "") String header,
                             @RequestParam(name = "print.footer", required = false, defaultValue = "") String footer,
                             @RequestParam(name = "print.fontSize", required = false, defaultValue = "12") String fontSize,
                             @RequestParam(name = "print.showQrCode", required = false, defaultValue = "false") String showQr,
                             @RequestParam(name = "print.onOrderCreate", required = false, defaultValue = "true") String onCreate,
                             @RequestParam(name = "print.onPay", required = false, defaultValue = "true") String onPay,
                             @RequestParam(name = "print.onRefund", required = false, defaultValue = "true") String onRefund,
                             @RequestParam(name = "print.onItemCancel", required = false, defaultValue = "false") String onItemCancel,
                             Map<String, Object> map) {
        save("print.header", header);
        save("print.footer", footer);
        save("print.fontSize", fontSize);
        save("print.showQrCode", showQr);
        save("print.onOrderCreate", onCreate);
        save("print.onPay", onPay);
        save("print.onRefund", onRefund);
        save("print.onItemCancel", onItemCancel);
        map.put("url", "/sell/seller/printSettings");
        return new ModelAndView("common/success", map);
    }

    private void save(String key, String value) {
        ShopConfig c = repository.findById(key).orElse(new ShopConfig());
        c.setConfigKey(key);
        c.setConfigValue(value);
        repository.save(c);
    }
}
