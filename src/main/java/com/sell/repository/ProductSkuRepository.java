package com.sell.repository;

import com.sell.dataobject.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductSkuRepository extends JpaRepository<ProductSku, String> {

    List<ProductSku> findByProductId(String productId);

    List<ProductSku> findByProductIdIn(List<String> productIds);

    /** 原子条件扣减 SKU 库存 (仅 skuStock 非空且充足时才扣). 返回受影响行数 (1=成功, 0=不足/不存在/未跟踪库存). */
    @Modifying
    @Query("UPDATE ProductSku s SET s.skuStock = s.skuStock - :quantity "
            + "WHERE s.skuId = :skuId AND s.skuStock >= :quantity")
    int decreaseSkuStock(@Param("skuId") String skuId, @Param("quantity") Integer quantity);

    /** 原子返还 SKU 库存 (取消/退款); 仅对跟踪库存(skuStock 非空)的 SKU 生效. */
    @Modifying
    @Query("UPDATE ProductSku s SET s.skuStock = s.skuStock + :quantity "
            + "WHERE s.skuId = :skuId AND s.skuStock IS NOT NULL")
    int increaseSkuStock(@Param("skuId") String skuId, @Param("quantity") Integer quantity);

    /**
     * 原子调整 SKU 库存 (手动盘点, delta 可正可负): 仅当调整后不为负时才更新, null 视为 0.
     * 避免并发"读-改-写"丢失更新. 返回受影响行数 (1=成功, 0=调整后会变负).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductSku s SET s.skuStock = COALESCE(s.skuStock, 0) + :delta "
            + "WHERE s.skuId = :skuId AND COALESCE(s.skuStock, 0) + :delta >= 0")
    int adjustSkuStockAtomic(@Param("skuId") String skuId, @Param("delta") Integer delta);
}
