package com.sell.service.impl;

import com.sell.dataobject.Recipe;
import com.sell.repository.RecipeRepository;
import com.sell.service.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class RecipeServiceImpl implements RecipeService {

    @Autowired
    private RecipeRepository repo;

    @Override
    public List<Recipe> findByProduct(String productId) {
        if (productId == null) return java.util.Collections.emptyList();
        return repo.findByProductId(productId);
    }

    @Override
    public List<Recipe> findByProductSku(String productId, String skuId) {
        if (skuId == null) return findByProduct(productId);
        // 优先精确匹配 (productId + skuId), 否则用通用 (skuId 为空) 配方
        List<Recipe> exact = repo.findByProductIdAndSkuId(productId, skuId);
        if (!exact.isEmpty()) return exact;
        return repo.findByProductIdAndSkuId(productId, null);
    }

    @Override
    public Recipe save(Recipe recipe) {
        Date now = new Date();
        if (recipe.getCreateTime() == null) recipe.setCreateTime(now);
        recipe.setUpdateTime(now);
        return repo.save(recipe);
    }

    @Override
    public void delete(Long recipeId) {
        repo.deleteById(recipeId);
    }

    @Override
    public List<Recipe> findAll() {
        return repo.findAll();
    }
}
