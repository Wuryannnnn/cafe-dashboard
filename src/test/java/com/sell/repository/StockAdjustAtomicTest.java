package com.sell.repository;

import com.sell.dataobject.ProductInfo;
import com.sell.dataobject.ProductSku;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * 验证手动盘点用的原子调整库存 SQL:
 * - 调整后不为负才更新 (返回 1), 否则不动 (返回 0)
 * - null 库存按 0 处理 (COALESCE), 可被正向调整初始化
 * 事务测试, 结束自动回滚, 不污染库.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class StockAdjustAtomicTest {

    @Autowired private ProductInfoRepository productRepo;
    @Autowired private ProductSkuRepository skuRepo;

    @Test
    public void adjustProductStock_positiveAndNegative() {
        productRepo.save(product("ATOMIC_T1", 10));

        // +3 → 13
        assertEquals(1, productRepo.adjustStockAtomic("ATOMIC_T1", 3));
        assertEquals(Integer.valueOf(13), productRepo.findById("ATOMIC_T1").get().getProductStock());

        // -5 → 8
        assertEquals(1, productRepo.adjustStockAtomic("ATOMIC_T1", -5));
        assertEquals(Integer.valueOf(8), productRepo.findById("ATOMIC_T1").get().getProductStock());

        // -100 会变负 → 拒绝(0 行), 库存不变
        assertEquals(0, productRepo.adjustStockAtomic("ATOMIC_T1", -100));
        assertEquals(Integer.valueOf(8), productRepo.findById("ATOMIC_T1").get().getProductStock());
    }

    @Test
    public void adjustProductStock_nullTreatedAsZero() {
        productRepo.save(product("ATOMIC_T2", null));
        // null + 5 → 5
        assertEquals(1, productRepo.adjustStockAtomic("ATOMIC_T2", 5));
        assertEquals(Integer.valueOf(5), productRepo.findById("ATOMIC_T2").get().getProductStock());
    }

    @Test
    public void adjustSkuStock_positiveAndNegative() {
        skuRepo.save(sku("ATOMIC_SKU1", "ATOMIC_P", 5));

        assertEquals(1, skuRepo.adjustSkuStockAtomic("ATOMIC_SKU1", 2));
        assertEquals(Integer.valueOf(7), skuRepo.findById("ATOMIC_SKU1").get().getSkuStock());

        // -100 会变负 → 拒绝
        assertEquals(0, skuRepo.adjustSkuStockAtomic("ATOMIC_SKU1", -100));
        assertEquals(Integer.valueOf(7), skuRepo.findById("ATOMIC_SKU1").get().getSkuStock());
    }

    private ProductInfo product(String id, Integer stock) {
        ProductInfo p = new ProductInfo();
        p.setProductId(id);
        p.setProductName("原子测试商品");
        p.setProductPrice(new BigDecimal("10.00"));
        p.setProductStock(stock);
        p.setProductStatus(0);
        p.setCategoryType(1);
        return p;
    }

    private ProductSku sku(String skuId, String productId, Integer stock) {
        ProductSku s = new ProductSku();
        s.setSkuId(skuId);
        s.setProductId(productId);
        s.setSkuName("大杯");
        s.setSkuPrice(new BigDecimal("12.00"));
        s.setSkuStock(stock);
        return s;
    }
}
