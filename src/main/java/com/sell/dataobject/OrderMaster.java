package com.sell.dataobject;

import com.sell.enums.DiningTypeEnum;
import com.sell.enums.OrderStatusEnum;
import com.sell.enums.PayStatusEnum;
import com.sell.enums.PayTypeEnum;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 2017-06-11 17:08
 */
@Entity
@Data
@DynamicUpdate
public class OrderMaster {

    /** 订单id. */
    @Id
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
    private Integer orderStatus = OrderStatusEnum.NEW.getCode();

    /** 支付状态, 默认为0未支付. */
    private Integer payStatus = PayStatusEnum.WAIT.getCode();

    /** 支付方式, 默认微信支付. */
    private Integer payType = PayTypeEnum.WECHAT.getCode();

    /** 就餐方式, 0堂食 1外带. */
    private Integer diningType = DiningTypeEnum.DINE_IN.getCode();

    /** 桌号(堂食时使用). */
    private String tableNumber;

    /** 关联桌台id (PRD 5: 堂食扫桌台二维码下单时绑定). */
    private Integer tableId;

    /** 取餐号(每日流水号, 如001). */
    private String pickupNumber;

    /** 订单备注(少冰/不要香菜等). */
    private String orderRemark;

    /** 创建时间. */
    private Date createTime;

    /** 更新时间. */
    private Date updateTime;

}
