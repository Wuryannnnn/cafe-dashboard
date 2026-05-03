package com.sell.service;

import com.sell.dataobject.Recipe;

import java.util.List;

public interface RecipeService {
    List<Recipe> findByProduct(String productId);
    List<Recipe> findByProductSku(String productId, String skuId);
    Recipe save(Recipe recipe);
    void delete(Long recipeId);
    List<Recipe> findAll();
}
