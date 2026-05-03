package com.sell.service;

import com.sell.dataobject.OrderMaster;
import com.sell.dataobject.OrderPaymentRecord;
import com.sell.dto.OrderDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 收银台 - PRD 7.3
 */
public interface CashierService {

    /** 当前桌台未结账订单 (status NEW + payStatus WAIT). */
    List<OrderMaster> findUnpaidOrders();

    /** 单桌台未结账订单. */
    List<OrderMaster> findUnpaidByTableId(Integer tableId);

    /** 标记订单为线下已收款 (单笔全额, 不走第三方通道). */
    OrderDTO offlinePay(String orderId, Integer methodId, String operator);

    /** 添加一笔组合支付记录, 累计达到订单总额时自动标记为已支付. */
    OrderDTO addPaymentRecord(String orderId, Integer methodId, BigDecimal amount, String operator);

    /** 查询订单的所有支付记录. */
    List<OrderPaymentRecord> findPaymentsByOrder(String orderId);

    /** 收银台手动创建订单 (堂食, 无 openid). */
    OrderDTO manualCreateOrder(Integer tableId, String items, String operator);
}
