package com.sell.repository;

import com.sell.dataobject.ProductInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 2017-05-09 11:39
 */
public interface ProductInfoRepository extends JpaRepository<ProductInfo, String> {

    List<ProductInfo> findByProductStatus(Integer productStatus);

    List<ProductInfo> findByCategoryType(Integer categoryType);

    long countByCategoryType(Integer categoryType);

    List<ProductInfo> findByProductNameContaining(String keyword);

    /** 原子条件扣减库存: 仅当库存充足时才扣减, 避免并发读-改-写导致超卖. 返回受影响行数 (1=成功, 0=库存不足/商品不存在). */
    @Modifying
    @Query("UPDATE ProductInfo p SET p.productStock = p.productStock - :quantity "
            + "WHERE p.productId = :productId AND p.productStock >= :quantity")
    int decreaseStockAtomic(@Param("productId") String productId, @Param("quantity") Integer quantity);

    /** 原子增加库存 (取消/退款返还). 返回受影响行数 (1=成功, 0=商品不存在). */
    @Modifying
    @Query("UPDATE ProductInfo p SET p.productStock = p.productStock + :quantity WHERE p.productId = :productId")
    int increaseStockAtomic(@Param("productId") String productId, @Param("quantity") Integer quantity);

    /**
     * 原子调整库存 (手动盘点, delta 可正可负): 仅当调整后不为负时才更新, null 视为 0.
     * 避免"读-改-写"被并发覆盖(丢失更新). 返回受影响行数 (1=成功, 0=调整后会变负).
     * clearAutomatically: 更新后清持久化上下文, 使随后再读到的是最新值(用于写流水的 stockAfter).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductInfo p SET p.productStock = COALESCE(p.productStock, 0) + :delta "
            + "WHERE p.productId = :productId AND COALESCE(p.productStock, 0) + :delta >= 0")
    int adjustStockAtomic(@Param("productId") String productId, @Param("delta") Integer delta);
}
