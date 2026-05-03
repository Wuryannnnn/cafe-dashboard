package com.sell.service;

import com.sell.dto.OrderDTO;

public interface PrinterService {

    /**
     * 按工位分单打印 - 订单创建后调用
     * 自动将饮品打到吧台打印机, 轻食打到后厨打印机
     */
    void printOrder(OrderDTO orderDTO);

    /**
     * 打印顾客消费小票(带明细和金额)
     */
    void printCustomerReceipt(OrderDTO orderDTO);

    /**
     * 补打某个订单的小票
     */
    void reprintOrder(String orderId);
}
