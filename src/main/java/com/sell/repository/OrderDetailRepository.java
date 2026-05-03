package com.sell.repository;

import com.sell.dataobject.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, String> {

    List<OrderDetail> findByOrderId(String orderId);

    /** 热销排行: 按商品销量降序(只统计有效订单) */
    @Query("SELECT d.productName, SUM(d.productQuantity) as qty FROM OrderDetail d WHERE d.orderId IN :orderIds GROUP BY d.productName ORDER BY qty DESC")
    List<Object[]> findTopProducts(@Param("orderIds") List<String> orderIds);

    /** 菜品销售明细: 商品名, 销量, 销售额. */
    @Query("SELECT d.productName, SUM(d.productQuantity), SUM(d.productPrice * d.productQuantity) " +
           "FROM OrderDetail d WHERE d.orderId IN :orderIds GROUP BY d.productName ORDER BY SUM(d.productQuantity) DESC")
    List<Object[]> findProductSalesDetail(@Param("orderIds") List<String> orderIds);

    /** 按 productId 聚合 (用于按分类统计). */
    @Query("SELECT d.productId, d.productName, SUM(d.productQuantity), SUM(d.productPrice * d.productQuantity) " +
           "FROM OrderDetail d WHERE d.orderId IN :orderIds GROUP BY d.productId, d.productName")
    List<Object[]> findProductSalesByProductId(@Param("orderIds") List<String> orderIds);
}
