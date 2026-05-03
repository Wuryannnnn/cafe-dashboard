package com.sell.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sell.dataobject.OrderDetail;
import com.sell.enums.DiningTypeEnum;
import com.sell.enums.OrderStatusEnum;
import com.sell.enums.PayStatusEnum;
import com.sell.enums.PayTypeEnum;
import com.sell.utils.EnumUtil;
import com.sell.utils.serializer.Date2LongSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 2017-06-11 18:30
 */
@Data
//@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
//@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderDTO {

    /** 订单id. */
    private String orderId;

    /** 买家名字. */
    private String buyerName;

    /** 买家手机号. */
    private String buyerPhone;

    /** 买家地址. */
    private String buyerAddress;

    /** 买家微信Openid. */
    private String buyerOpenid;

    /** 订单总金额. */
    private BigDecimal orderAmount;

    /** 订单状态, 默认为0新下单. */
    private Integer orderStatus;

    /** 支付状态, 默认为0未支付. */
    private Integer payStatus;

    /** 支付方式. */
    private Integer payType;

    /** 就餐方式. */
    private Integer diningType;

    /** 桌号. */
    private String tableNumber;

    /** 关联桌台id. */
    private Integer tableId;

    /** 取餐号. */
    private String pickupNumber;

    /** 订单备注. */
    private String orderRemark;

    /** 创建时间. */
    @JsonSerialize(using = Date2LongSerializer.class)
    private Date createTime;

    /** 更新时间. */
    @JsonSerialize(using = Date2LongSerializer.class)
    private Date updateTime;

    List<OrderDetail> orderDetailList;

    @JsonIgnore
    public OrderStatusEnum getOrderStatusEnum() {
        return EnumUtil.getByCode(orderStatus, OrderStatusEnum.class);
    }

    @JsonIgnore
    public PayStatusEnum getPayStatusEnum() {
        return EnumUtil.getByCode(payStatus, PayStatusEnum.class);
    }

    @JsonIgnore
    public PayTypeEnum getPayTypeEnum() {
        return EnumUtil.getByCode(payType, PayTypeEnum.class);
    }

    @JsonIgnore
    public DiningTypeEnum getDiningTypeEnum() {
        return EnumUtil.getByCode(diningType, DiningTypeEnum.class);
    }
}
