package com.sell.service;

import com.sell.dataobject.ProductCategory;

import java.util.List;

/**
 * 类目
 * 2017-05-09 10:12
 */
public interface CategoryService {

    ProductCategory findOne(Integer categoryId);

    List<ProductCategory> findAll();

    List<ProductCategory> findByCategoryTypeIn(List<Integer> categoryTypeList);

    ProductCategory save(ProductCategory productCategory);

    void delete(Integer categoryId);
}
