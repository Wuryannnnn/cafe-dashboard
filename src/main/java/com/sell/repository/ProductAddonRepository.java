package com.sell.repository;

import com.sell.dataobject.ProductAddon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductAddonRepository extends JpaRepository<ProductAddon, String> {

    List<ProductAddon> findByCategoryType(Integer categoryType);

    List<ProductAddon> findByCategoryTypeIn(List<Integer> categoryTypes);

    List<ProductAddon> findByCategoryTypeIsNull();
}
