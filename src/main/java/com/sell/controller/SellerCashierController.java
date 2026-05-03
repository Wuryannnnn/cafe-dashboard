package com.sell.controller;

import com.sell.dataobject.OrderMaster;
import com.sell.dataobject.OrderPaymentRecord;
import com.sell.dataobject.PaymentMethod;
import com.sell.dataobject.ProductInfo;
import com.sell.dataobject.RestaurantTable;
import com.sell.dto.OrderDTO;
import com.sell.exception.SellException;
import com.sell.service.CashierService;
import com.sell.service.PaymentMethodService;
import com.sell.service.ProductService;
import com.sell.service.RestaurantTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 收银台 - PRD 7.3
 */
@Controller
@RequestMapping("/seller/cashier")
public class SellerCashierController {

    @Autowired
    private CashierService cashierService;

    @Autowired
    private PaymentMethodService paymentMethodService;

    @Autowired
    private RestaurantTableService tableService;

    @Autowired
    private ProductService productService;

    @GetMapping("/desk")
    public ModelAndView desk(@RequestParam(value = "tableId", required = false) Integer tableId,
                             Map<String, Object> map) {
        List<OrderMaster> orders = cashierService.findUnpaidByTableId(tableId);
        List<RestaurantTable> tables = tableService.findAll();
        List<PaymentMethod> methods = paymentMethodService.findEnabled();

        Map<Integer, String> tableNameMap = new HashMap<>();
        for (RestaurantTable t : tables) {
            tableNameMap.put(t.getTableId(), t.getTableCode());
        }

        map.put("orders", orders);
        map.put("tables", tables);
        map.put("paymentMethods", methods);
        map.put("currentTableId", tableId);
        map.put("tableNameMap", tableNameMap);
        return new ModelAndView("cashier/desk", map);
    }

    @PostMapping("/offlinePay")
    public ModelAndView offlinePay(@RequestParam("orderId") String orderId,
                                   @RequestParam("methodId") Integer methodId,
                                   Map<String, Object> map) {
        try {
            cashierService.offlinePay(orderId, methodId, "cashier");
        } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/cashier/desk");
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/cashier/desk");
        return new ModelAndView("common/success", map);
    }

    @PostMapping("/payRecord")
    public ModelAndView addPayRecord(@RequestParam("orderId") String orderId,
                                     @RequestParam("methodId") Integer methodId,
                                     @RequestParam("amount") BigDecimal amount,
                                     Map<String, Object> map) {
        try {
            cashierService.addPaymentRecord(orderId, methodId, amount, "cashier");
        } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/cashier/desk");
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/cashier/desk");
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/payments")
    @ResponseBody
    public List<OrderPaymentRecord> payments(@RequestParam("orderId") String orderId) {
        return cashierService.findPaymentsByOrder(orderId);
    }

    @GetMapping("/manualOrder")
    public ModelAndView manualOrderForm(Map<String, Object> map) {
        map.put("tables", tableService.findAll());
        map.put("products", productService.findUpAll());
        return new ModelAndView("cashier/manual", map);
    }

    @PostMapping("/manualOrderCreate")
    public ModelAndView manualOrderCreate(@RequestParam("tableId") Integer tableId,
                                          @RequestParam("items") String items,
                                          Map<String, Object> map) {
        try {
            OrderDTO dto = cashierService.manualCreateOrder(tableId, items, "cashier");
            map.put("url", "/sell/seller/cashier/desk");
            map.put("msg", "已创建订单 " + dto.getOrderId());
        } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/cashier/manualOrder");
            return new ModelAndView("common/error", map);
        }
        return new ModelAndView("common/success", map);
    }
}
