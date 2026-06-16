package com.sell.service.impl;

import com.sell.dataobject.OrderDetail;
import com.sell.dataobject.PaymentMethod;
import com.sell.dataobject.ProductInfo;
import com.sell.dto.OrderDTO;
import com.sell.enums.OrderStatusEnum;
import com.sell.enums.PayStatusEnum;
import com.sell.exception.SellException;
import com.sell.repository.PaymentMethodRepository;
import com.sell.repository.ProductInfoRepository;
import com.sell.service.CashierService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 回归: 收银台收款守卫(B3) + 退款返还库存 best-effort(B5).
 * 事务测试, 结束回滚.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class CashierGuardRefundTest {

    @Autowired private OrderServiceImpl orderService;
    @Autowired private CashierService cashierService;
    @Autowired private ProductInfoRepository productRepo;
    @Autowired private PaymentMethodRepository paymentMethodRepo;

    private static final String PID = "CASHIER_TEST_P1";
    private Integer methodId; // PaymentMethod.methodId 是 @GeneratedValue, 保存后取真实生成值

    @Before
    public void seed() {
        ProductInfo p = new ProductInfo();
        p.setProductId(PID);
        p.setProductName("收银测试美式");
        p.setProductPrice(new BigDecimal("20.00"));
        p.setProductStock(100);
        p.setProductStatus(0);
        p.setCategoryType(1);
        productRepo.save(p);

        PaymentMethod m = new PaymentMethod();
        m.setMethodName("现金");
        m.setMethodCode("CASH");
        m.setEnabled(true);
        methodId = paymentMethodRepo.save(m).getMethodId();
    }

    private OrderDTO createOrder() {
        OrderDTO dto = new OrderDTO();
        dto.setBuyerName("测试顾客");
        dto.setBuyerOpenid("cashier_guard_openid");
        dto.setBuyerPhone("13800138000");
        dto.setBuyerAddress("");
        OrderDetail d = new OrderDetail();
        d.setProductId(PID);
        d.setProductQuantity(1);
        List<OrderDetail> list = new ArrayList<>();
        list.add(d);
        dto.setOrderDetailList(list);
        return orderService.create(dto);
    }

    /** B3: 已取消订单不能再被收银台收款. */
    @Test(expected = SellException.class)
    public void offlinePay_onCancelledOrder_rejected() {
        OrderDTO order = createOrder();
        orderService.cancel(orderService.findOne(order.getOrderId())); // 先取消
        cashierService.offlinePay(order.getOrderId(), methodId, "tester"); // 应抛 订单状态不正确
    }

    /** B3: 已全额结清(SUCCESS)的订单不能再追加收款流水, 防止虚增报表. */
    @Test(expected = SellException.class)
    public void addPaymentRecord_onPaidOrder_rejected() {
        OrderDTO order = createOrder();
        cashierService.offlinePay(order.getOrderId(), methodId, "tester"); // 全额收款 → SUCCESS
        cashierService.addPaymentRecord(order.getOrderId(), methodId, new BigDecimal("5.00"), "tester"); // 应抛
    }

    /**
     * B5: 退款前商品被硬删除, 退款仍须把订单标记为已退款(库存返还 best-effort, 不回滚已退款).
     * 否则钱已退但订单状态回滚, 可被二次退款.
     */
    @Test
    public void refund_afterProductDeleted_stillMarksRefunded() {
        OrderDTO order = createOrder();
        cashierService.offlinePay(order.getOrderId(), methodId, "tester"); // 线下收款 → SUCCESS (退款不走网关)
        productRepo.deleteById(PID); // 商品被硬删除

        orderService.refund(orderService.findOne(order.getOrderId())); // 不应抛异常

        OrderDTO persisted = orderService.findOne(order.getOrderId());
        assertEquals("退款后订单应为已退款", OrderStatusEnum.REFUNDED.getCode(), persisted.getOrderStatus());
        assertEquals("退款后支付状态应为已退款", PayStatusEnum.REFUND.getCode(), persisted.getPayStatus());
    }
}
