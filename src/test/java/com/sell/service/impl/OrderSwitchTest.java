package com.sell.service.impl;

import com.sell.dataobject.OrderDetail;
import com.sell.dataobject.ProductInfo;
import com.sell.dataobject.ShopConfig;
import com.sell.dto.OrderDTO;
import com.sell.enums.OrderStatusEnum;
import com.sell.exception.SellException;
import com.sell.repository.ProductInfoRepository;
import com.sell.repository.ShopConfigRepository;
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
 * 验证顾客扫码点餐的三个业务开关在下单流程里真正生效:
 * - switch.qrOrder=false → 拒绝下单
 * - switch.autoAcceptOrder=true + 先食后付 → 下单即转"制作中"
 * - switch.autoAcceptOrder=true + 先付后食 → 下单仍为"新订单"(待付款后再接)
 * 事务测试, 结束回滚.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class OrderSwitchTest {

    @Autowired private OrderServiceImpl orderService;
    @Autowired private ProductInfoRepository productRepo;
    @Autowired private ShopConfigRepository configRepo;

    private static final String PID = "SWITCH_TEST_P1";

    private void seedProduct() {
        ProductInfo p = new ProductInfo();
        p.setProductId(PID);
        p.setProductName("开关测试拿铁");
        p.setProductPrice(new BigDecimal("18.00"));
        p.setProductStock(100);
        p.setProductStatus(0);
        p.setCategoryType(1);
        productRepo.save(p);
    }

    private void setSwitch(String key, boolean on) {
        ShopConfig c = configRepo.findById(key).orElse(new ShopConfig());
        c.setConfigKey(key);
        c.setConfigValue(on ? "true" : "false");
        configRepo.save(c);
    }

    private OrderDTO buildOrder() {
        OrderDTO dto = new OrderDTO();
        dto.setBuyerName("测试顾客");
        dto.setBuyerOpenid("switch_test_openid");
        dto.setBuyerPhone("13800138000");
        dto.setBuyerAddress("");
        OrderDetail d = new OrderDetail();
        d.setProductId(PID);
        d.setProductQuantity(1);
        List<OrderDetail> list = new ArrayList<>();
        list.add(d);
        dto.setOrderDetailList(list);
        return dto;
    }

    @Test(expected = SellException.class)
    public void qrOrderOff_rejectsCreate() {
        seedProduct();
        setSwitch("switch.qrOrder", false);
        orderService.create(buildOrder()); // 应抛"扫码点餐已关闭"
    }

    /**
     * 扫码点餐总开关关闭时, 收银台手动建单(buyerOpenid=cashier-manual)仍须放行——
     * 否则店主一关扫码点餐, 收银台开单会被一并锁死. 回归 B1.
     */
    @Test
    public void qrOrderOff_cashierManualStillWorks() {
        seedProduct();
        setSwitch("switch.qrOrder", false);
        OrderDTO dto = buildOrder();
        dto.setBuyerOpenid("cashier-manual"); // 收银台来源
        OrderDTO result = orderService.create(dto); // 不应抛异常
        assertNotNull("收银台手动建单应不受扫码点餐开关影响", result.getOrderId());
        OrderDTO persisted = orderService.findOne(result.getOrderId());
        assertEquals(OrderStatusEnum.NEW.getCode(), persisted.getOrderStatus());
    }

    @Test
    public void autoAccept_payAfter_createGoesMaking() {
        seedProduct();
        setSwitch("switch.qrOrder", true);
        setSwitch("switch.autoAcceptOrder", true);
        setSwitch("switch.payBeforeServe", false); // 先食后付
        OrderDTO result = orderService.create(buildOrder());
        // 以持久化的真实状态为准 (create 返回的 DTO 状态字段非闭环关注点)
        OrderDTO persisted = orderService.findOne(result.getOrderId());
        assertEquals("先食后付+自动接单: 下单即制作中",
                OrderStatusEnum.MAKING.getCode(), persisted.getOrderStatus());
    }

    @Test
    public void autoAccept_payBefore_createStaysNew() {
        seedProduct();
        setSwitch("switch.qrOrder", true);
        setSwitch("switch.autoAcceptOrder", true);
        setSwitch("switch.payBeforeServe", true); // 先付后食
        OrderDTO result = orderService.create(buildOrder());
        OrderDTO persisted = orderService.findOne(result.getOrderId());
        assertEquals("先付后食: 下单仍为新订单, 待付款后再接",
                OrderStatusEnum.NEW.getCode(), persisted.getOrderStatus());
    }
}
