package com.sell.dto;

import com.sell.dataobject.OrderDetail;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 一张打印小票的内容
 */
@Data
public class PrintTicketDTO {

    /** 工位名称: 吧台/后厨. */
    private String stationName;

    /** 订单号. */
    private String orderId;

    /** 就餐方式: 堂食/外带. */
    private String diningType;

    /** 桌号. */
    private String tableNumber;

    /** 下单时间. */
    private Date createTime;

    /** 该工位需要制作的商品明细. */
    private List<OrderDetail> items;

    /** 取餐号. */
    private String pickupNumber;

    /** 备注. */
    private String remark;
}
