package com.sell.repository;

import com.sell.dataobject.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductSkuRepository extends JpaRepository<ProductSku, String> {

    List<ProductSku> findByProductId(String productId);

    List<ProductSku> findByProductIdIn(List<String> productIds);
}
