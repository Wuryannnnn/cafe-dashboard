package com.sell.repository;

import com.sell.dataobject.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    List<Recipe> findByProductId(String productId);

    List<Recipe> findByProductIdAndSkuId(String productId, String skuId);

    List<Recipe> findByProductIdIn(List<String> productIds);
}
