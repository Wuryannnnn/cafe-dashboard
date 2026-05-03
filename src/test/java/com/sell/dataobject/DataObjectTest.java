package com.sell.dataobject;

import com.sell.enums.DiningTypeEnum;
import com.sell.enums.OrderStatusEnum;
import com.sell.enums.PayTypeEnum;
import com.sell.enums.PrintStationEnum;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * 测试新增的数据模型字段和默认值
 */
public class DataObjectTest {

    @Test
    public void orderMaster_defaults() {
        OrderMaster om = new OrderMaster();
        assertEquals(OrderStatusEnum.NEW.getCode(), om.getOrderStatus());
        assertEquals(PayTypeEnum.WECHAT.getCode(), om.getPayType());
        assertEquals(DiningTypeEnum.DINE_IN.getCode(), om.getDiningType());
        assertNull(om.getTableNumber());
        assertNull(om.getPickupNumber());
        assertNull(om.getOrderRemark());
    }

    @Test
    public void orderMaster_newFields() {
        OrderMaster om = new OrderMaster();
        om.setTableNumber("A3");
        om.setPickupNumber("001");
        om.setOrderRemark("少冰");
        om.setPayType(PayTypeEnum.ALIPAY.getCode());
        om.setDiningType(DiningTypeEnum.TAKEAWAY.getCode());

        assertEquals("A3", om.getTableNumber());
        assertEquals("001", om.getPickupNumber());
        assertEquals("少冰", om.getOrderRemark());
        assertEquals(PayTypeEnum.ALIPAY.getCode(), om.getPayType());
        assertEquals(DiningTypeEnum.TAKEAWAY.getCode(), om.getDiningType());
    }

    @Test
    public void orderDetail_newFields() {
        OrderDetail od = new OrderDetail();
        od.setSkuId("sku001");
        od.setSkuName("大杯/冰");
        od.setAddons("[{\"name\":\"加浓\",\"price\":3}]");
        od.setAddonFee(new BigDecimal("3.00"));

        assertEquals("sku001", od.getSkuId());
        assertEquals("大杯/冰", od.getSkuName());
        assertNotNull(od.getAddons());
        assertEquals(new BigDecimal("3.00"), od.getAddonFee());
    }

    @Test
    public void productSku_fields() {
        ProductSku sku = new ProductSku();
        sku.setSkuId("sku001");
        sku.setProductId("prod001");
        sku.setSkuName("大杯/冰");
        sku.setSkuPrice(new BigDecimal("26.00"));
        sku.setSkuStock(999);

        assertEquals("sku001", sku.getSkuId());
        assertEquals("prod001", sku.getProductId());
        assertEquals(new BigDecimal("26.00"), sku.getSkuPrice());
    }

    @Test
    public void productAddon_fields() {
        ProductAddon addon = new ProductAddon();
        addon.setAddonId("addon001");
        addon.setAddonName("加浓");
        addon.setAddonPrice(new BigDecimal("3.00"));
        addon.setCategoryType(null);

        assertEquals("addon001", addon.getAddonId());
        assertEquals("加浓", addon.getAddonName());
        assertNull(addon.getCategoryType());
    }

    @Test
    public void productCategory_printStation() {
        ProductCategory cat = new ProductCategory();
        assertEquals(PrintStationEnum.BAR.getCode(), cat.getPrintStation());

        cat.setPrintStation(PrintStationEnum.KITCHEN.getCode());
        assertEquals(PrintStationEnum.KITCHEN.getCode(), cat.getPrintStation());
    }

    @Test
    public void printerConfig_defaults() {
        PrinterConfig p = new PrinterConfig();
        assertEquals(PrintStationEnum.BAR.getCode(), Integer.valueOf(p.getStation()));
        assertEquals(Integer.valueOf(9100), p.getPort());
        assertTrue(p.getEnabled());
    }
}
