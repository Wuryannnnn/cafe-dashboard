package com.sell.service.impl;

import com.sell.dataobject.OrderDetail;
import com.sell.dataobject.OrderMaster;
import com.sell.dataobject.OrderPaymentRecord;
import com.sell.dataobject.ProductInfo;
import com.sell.dto.OrderDTO;
import com.sell.enums.OrderStatusEnum;
import com.sell.enums.PayStatusEnum;
import com.sell.repository.OrderMasterRepository;
import com.sell.repository.OrderPaymentRecordRepository;
import com.sell.repository.ProductInfoRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * 验证: 线下(现金/会员)收款的订单退款应本地标记成功, 不走支付网关.
 * 若 refund 仍无脑调网关, 无真实凭据会抛 ORDER_REFUND_FAIL, 本测试即失败.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class CashRefundTest {

    @Autowired private OrderServiceImpl orderService;
    @Autowired private ProductInfoRepository productRepo;
    @Autowired private OrderMasterRepository orderMasterRepository;
    @Autowired private OrderPaymentRecordRepository paymentRepo;

    @Test
    public void cashOrderRefund_skipsGateway() {
        String pid = "CASH_REFUND_P";
        ProductInfo p = new ProductInfo();
        p.setProductId(pid);
        p.setProductName("现金退款测试");
        p.setProductPrice(new BigDecimal("20.00"));
        p.setProductStock(50);
        p.setProductStatus(0);
        p.setCategoryType(1);
        productRepo.save(p);

        OrderDTO dto = new OrderDTO();
        dto.setBuyerName("t");
        dto.setBuyerOpenid("cash");
        dto.setBuyerPhone("13800138000");
        dto.setBuyerAddress("");
        OrderDetail d = new OrderDetail();
        d.setProductId(pid);
        d.setProductQuantity(2);
        List<OrderDetail> list = new ArrayList<>();
        list.add(d);
        dto.setOrderDetailList(list);
        String oid = orderService.create(dto).getOrderId();

        // 模拟收银台现金收款: 订单标记已支付 + 写一条收款流水(=线下收款标记)
        OrderMaster om = orderMasterRepository.findById(oid).get();
        om.setPayStatus(PayStatusEnum.SUCCESS.getCode());
        orderMasterRepository.save(om);
        OrderPaymentRecord rec = new OrderPaymentRecord();
        rec.setOrderId(oid);
        rec.setMethodId(3);
        rec.setMethodName("现金");
        rec.setAmount(new BigDecimal("40.00"));
        rec.setOperator("test");
        rec.setCreateTime(new Date());
        paymentRepo.save(rec);

        // 退款: 现金单 → 本地标记退款成功(不调网关). 若调网关(无凭据)会抛异常.
        OrderDTO refunded = orderService.refund(orderService.findOne(oid));
        assertEquals("现金单退款应本地成功", OrderStatusEnum.REFUNDED.getCode(), refunded.getOrderStatus());
        assertEquals(PayStatusEnum.REFUND.getCode(), refunded.getPayStatus());
    }
}
