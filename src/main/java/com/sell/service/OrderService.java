package com.sell.service;

import com.sell.dto.OrderDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 2017-06-11 18:23
 */
public interface OrderService {

    /** 创建订单. */
    OrderDTO create(OrderDTO orderDTO);

    /** 查询单个订单. */
    OrderDTO findOne(String orderId);

    /** 查询订单列表. */
    Page<OrderDTO> findList(String buyerOpenid, Pageable pageable);

    /** 取消订单. */
    OrderDTO cancel(OrderDTO orderDTO);

    /** 开始制作. */
    OrderDTO making(OrderDTO orderDTO);

    /** 制作完成, 待取餐. */
    OrderDTO ready(OrderDTO orderDTO);

    /** 完结订单. */
    OrderDTO finish(OrderDTO orderDTO);

    /** 支付订单. */
    OrderDTO paid(OrderDTO orderDTO);

    /** 查询订单列表. */
    Page<OrderDTO> findList(Pageable pageable);

    /** 退款 (已支付订单, 状态变为已退款). */
    OrderDTO refund(OrderDTO orderDTO);

    /** 改价 (修改订单金额). */
    OrderDTO updateAmount(String orderId, java.math.BigDecimal newAmount);

    /** 打折 (按折扣率, 0~100, 例如 80=8折). */
    OrderDTO applyDiscount(String orderId, int discountRate);

    /** 免单 (金额置 0). */
    OrderDTO freeOrder(String orderId);

}
