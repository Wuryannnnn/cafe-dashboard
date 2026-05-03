package com.sell.repository;

import com.sell.dataobject.ProductInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 2017-05-09 11:39
 */
public interface ProductInfoRepository extends JpaRepository<ProductInfo, String> {

    List<ProductInfo> findByProductStatus(Integer productStatus);

    List<ProductInfo> findByCategoryType(Integer categoryType);

    long countByCategoryType(Integer categoryType);

    List<ProductInfo> findByProductNameContaining(String keyword);
}
