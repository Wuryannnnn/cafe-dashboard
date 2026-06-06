package com.sell.service.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sell.dataobject.OrderDetail;
import com.sell.dataobject.OrderMaster;
import com.sell.dataobject.OrderPaymentRecord;
import com.sell.dataobject.PaymentMethod;
import com.sell.dataobject.RestaurantTable;
import com.sell.dto.OrderDTO;
import com.sell.enums.OrderStatusEnum;
import com.sell.enums.PayStatusEnum;
import com.sell.enums.ResultEnum;
import com.sell.exception.SellException;
import com.sell.repository.OrderMasterRepository;
import com.sell.repository.OrderPaymentRecordRepository;
import com.sell.service.CashierService;
import com.sell.service.OrderService;
import com.sell.service.PaymentMethodService;
import com.sell.service.RestaurantTableService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class CashierServiceImpl implements CashierService {

    @Autowired
    private OrderMasterRepository orderMasterRepository;

    @Autowired
    private OrderPaymentRecordRepository paymentRecordRepository;

    @Autowired
    private PaymentMethodService paymentMethodService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private RestaurantTableService tableService;

    @Override
    public List<OrderMaster> findUnpaidOrders() {
        // 待支付且未取消的订单
        return orderMasterRepository.findAll().stream()
                .filter(o -> PayStatusEnum.WAIT.getCode().equals(o.getPayStatus()))
                .filter(o -> !OrderStatusEnum.CANCEL.getCode().equals(o.getOrderStatus()))
                .filter(o -> !OrderStatusEnum.REFUNDED.getCode().equals(o.getOrderStatus()))
                .toList();
    }

    @Override
    public List<OrderMaster> findUnpaidByTableId(Integer tableId) {
        return findUnpaidOrders().stream()
                .filter(o -> tableId == null || tableId.equals(o.getTableId()))
                .toList();
    }

    @Override
    @Transactional
    public OrderDTO offlinePay(String orderId, Integer methodId, String operator) {
        OrderDTO orderDTO = orderService.findOne(orderId);
        addPaymentRecordInternal(orderId, methodId, orderDTO.getOrderAmount(), operator);
        return markPaidIfFullySettled(orderDTO);
    }

    @Override
    @Transactional
    public OrderDTO addPaymentRecord(String orderId, Integer methodId, BigDecimal amount, String operator) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new SellException(1, "支付金额必须 > 0");
        }
        OrderDTO orderDTO = orderService.findOne(orderId);
        addPaymentRecordInternal(orderId, methodId, amount, operator);
        return markPaidIfFullySettled(orderDTO);
    }

    private void addPaymentRecordInternal(String orderId, Integer methodId, BigDecimal amount, String operator) {
        PaymentMethod m = paymentMethodService.findOne(methodId);
        if (m == null) throw new SellException(ResultEnum.PAYMENT_METHOD_NOT_EXIST);
        OrderPaymentRecord r = new OrderPaymentRecord();
        r.setOrderId(orderId);
        r.setMethodId(methodId);
        r.setMethodName(m.getMethodName());
        r.setAmount(amount);
        r.setOperator(operator);
        r.setCreateTime(new Date());
        paymentRecordRepository.save(r);
    }

    private OrderDTO markPaidIfFullySettled(OrderDTO orderDTO) {
        BigDecimal paid = paymentRecordRepository.sumByOrderId(orderDTO.getOrderId());
        // 重新读取订单, 用最新的 payStatus 判断(不用调用方传入的旧值), 避免拆分付款下的
        // 重复标记/漏标; 只在仍为 WAIT 时 WAIT→SUCCESS 一次, 天然幂等.
        OrderMaster om = orderMasterRepository.findById(orderDTO.getOrderId()).orElse(null);
        if (om != null
                && PayStatusEnum.WAIT.getCode().equals(om.getPayStatus())
                // 用重新读到的最新订单金额对比(而非可能已被并发改价的旧 orderDTO 金额), 避免错标已付
                && paid.compareTo(om.getOrderAmount()) >= 0) {
            // 直接更新数据库 (不走 orderService.paid 因为它要求 orderStatus = NEW)
            om.setPayStatus(PayStatusEnum.SUCCESS.getCode());
            om.setUpdateTime(new Date());
            orderMasterRepository.save(om);
            orderDTO.setPayStatus(PayStatusEnum.SUCCESS.getCode());
        }
        return orderDTO;
    }

    @Override
    public List<OrderPaymentRecord> findPaymentsByOrder(String orderId) {
        return paymentRecordRepository.findByOrderIdOrderByPaymentIdAsc(orderId);
    }

    @Override
    @Transactional
    public OrderDTO manualCreateOrder(Integer tableId, String itemsJson, String operator) {
        OrderDTO dto = new OrderDTO();
        dto.setBuyerName(operator != null ? "[收银台]" + operator : "[收银台]");
        dto.setBuyerPhone("");
        dto.setBuyerAddress("");
        dto.setBuyerOpenid("cashier-manual");
        dto.setDiningType(0); // 堂食
        dto.setPayType(0);
        dto.setOrderRemark("[收银台手动建单]");

        if (tableId != null) {
            dto.setTableId(tableId);
            RestaurantTable t = tableService.findOne(tableId);
            if (t != null) dto.setTableNumber(t.getTableCode());
        }

        Gson gson = new Gson();
        List<OrderDetail> details;
        try {
            details = gson.fromJson(itemsJson, new TypeToken<List<OrderDetail>>(){}.getType());
        } catch (Exception e) {
            throw new SellException(ResultEnum.PARAM_ERROR);
        }
        dto.setOrderDetailList(details != null ? details : new ArrayList<>());
        return orderService.create(dto);
    }
}
