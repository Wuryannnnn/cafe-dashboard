package com.sell.controller;

import com.sell.dataobject.PrinterConfig;
import com.sell.enums.PrintStationEnum;
import com.sell.repository.PrinterConfigRepository;
import com.sell.service.PrinterService;
import com.sell.utils.KeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;

/**
 * 打印机管理
 */
@Controller
@RequestMapping("/seller/printer")
@Slf4j
public class SellerPrinterController {

    @Autowired
    private PrinterConfigRepository printerConfigRepository;

    @Autowired
    private PrinterService printerService;

    /**
     * 打印机列表
     */
    @GetMapping("/list")
    public ModelAndView list(Map<String, Object> map) {
        List<PrinterConfig> printerList = printerConfigRepository.findAll();
        map.put("printerList", printerList);
        map.put("stations", PrintStationEnum.values());
        return new ModelAndView("printer/list", map);
    }

    /**
     * 新增/编辑打印机
     */
    @PostMapping("/save")
    public ModelAndView save(@RequestParam(value = "printerId", required = false) String printerId,
                             @RequestParam("printerName") String printerName,
                             @RequestParam("station") Integer station,
                             @RequestParam("ipAddress") String ipAddress,
                             @RequestParam(value = "port", defaultValue = "9100") Integer port,
                             Map<String, Object> map) {
        PrinterConfig printer;
        if (printerId != null && !printerId.isEmpty()) {
            printer = printerConfigRepository.findById(printerId).orElse(new PrinterConfig());
        } else {
            printer = new PrinterConfig();
            printer.setPrinterId(KeyUtil.genUniqueKey());
        }

        printer.setPrinterName(printerName);
        printer.setStation(station);
        printer.setIpAddress(ipAddress);
        printer.setPort(port);
        printer.setEnabled(true);
        printerConfigRepository.save(printer);

        map.put("msg", "保存成功");
        map.put("url", "/sell/seller/printer/list");
        return new ModelAndView("common/success", map);
    }

    /**
     * 启用/禁用打印机
     */
    @GetMapping("/toggle")
    public ModelAndView toggle(@RequestParam("printerId") String printerId,
                               Map<String, Object> map) {
        PrinterConfig printer = printerConfigRepository.findById(printerId).orElse(null);
        if (printer != null) {
            printer.setEnabled(!printer.getEnabled());
            printerConfigRepository.save(printer);
        }
        map.put("msg", "操作成功");
        map.put("url", "/sell/seller/printer/list");
        return new ModelAndView("common/success", map);
    }

    /**
     * 删除打印机
     */
    @GetMapping("/delete")
    public ModelAndView delete(@RequestParam("printerId") String printerId,
                               Map<String, Object> map) {
        printerConfigRepository.deleteById(printerId);
        map.put("msg", "删除成功");
        map.put("url", "/sell/seller/printer/list");
        return new ModelAndView("common/success", map);
    }

    /**
     * 补打小票
     */
    @GetMapping("/reprint")
    public ModelAndView reprint(@RequestParam("orderId") String orderId,
                                Map<String, Object> map) {
        try {
            printerService.reprintOrder(orderId);
            map.put("msg", "已发送补打");
            map.put("url", "/sell/seller/order/list");
        } catch (Exception e) {
            log.error("【补打小票】失败, orderId={}", orderId, e);
            map.put("msg", "补打失败: " + e.getMessage());
            map.put("url", "/sell/seller/order/list");
            return new ModelAndView("common/error", map);
        }
        return new ModelAndView("common/success", map);
    }
}
