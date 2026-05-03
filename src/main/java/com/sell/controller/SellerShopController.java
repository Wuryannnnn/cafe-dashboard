package com.sell.controller;

import com.sell.dataobject.ProductCategory;
import com.sell.dataobject.ProductInfo;
import com.sell.dataobject.ShopConfig;
import com.sell.repository.ShopConfigRepository;
import com.sell.service.CategoryService;
import com.sell.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 卖家后台 - 店铺装修
 */
@Controller
@RequestMapping("/seller/shop")
@Slf4j
public class SellerShopController {

    @Autowired
    private ShopConfigRepository shopConfigRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    /**
     * 店铺装修主页
     */
    @GetMapping("/design")
    public ModelAndView design(Map<String, Object> map) {
        // 加载所有配置
        List<ShopConfig> configs = shopConfigRepository.findAll();
        for (ShopConfig c : configs) {
            map.put(c.getConfigKey(), c.getConfigValue());
        }

        // 加载类目(按排序)
        List<ProductCategory> categoryList = categoryService.findAll();
        categoryList.sort(Comparator.comparingInt(c -> c.getSortOrder() != null ? c.getSortOrder() : 0));
        map.put("categoryList", categoryList);

        // 加载全部商品(按排序)
        List<ProductInfo> productList = productService.findUpAll();
        productList.sort(Comparator.comparingInt(p -> p.getSortOrder() != null ? p.getSortOrder() : 0));
        map.put("productList", productList);

        return new ModelAndView("shop/design", map);
    }

    /**
     * 保存店铺基本信息
     */
    @PostMapping("/saveBasic")
    public ModelAndView saveBasic(@RequestParam("shopName") String shopName,
                                  @RequestParam(value = "shopLogo", required = false) String shopLogo,
                                  @RequestParam(value = "announcement", required = false) String announcement,
                                  @RequestParam(value = "themeColor", required = false) String themeColor,
                                  Map<String, Object> map) {
        saveConfig("shopName", shopName);
        saveConfig("shopLogo", shopLogo);
        saveConfig("announcement", announcement);
        saveConfig("themeColor", themeColor != null ? themeColor : "#6b4226");

        map.put("msg", "保存成功");
        map.put("url", "/sell/seller/shop/design");
        return new ModelAndView("common/success", map);
    }

    /**
     * 保存Banner
     */
    @PostMapping("/saveBanners")
    public ModelAndView saveBanners(@RequestParam("banners") String banners,
                                    Map<String, Object> map) {
        saveConfig("banners", banners);
        map.put("msg", "Banner保存成功");
        map.put("url", "/sell/seller/shop/design");
        return new ModelAndView("common/success", map);
    }

    /**
     * 保存类目排序
     */
    @PostMapping("/saveCategorySort")
    @ResponseBody
    public Map<String, Object> saveCategorySort(@RequestBody List<Map<String, Object>> sortData) {
        for (Map<String, Object> item : sortData) {
            Integer categoryId = (Integer) item.get("id");
            Integer sortOrder = (Integer) item.get("sort");
            ProductCategory cat = categoryService.findOne(categoryId);
            if (cat != null) {
                cat.setSortOrder(sortOrder);
                categoryService.save(cat);
            }
        }
        return Map.of("code", 0, "msg", "排序已保存");
    }

    /**
     * 保存商品排序
     */
    @PostMapping("/saveProductSort")
    @ResponseBody
    public Map<String, Object> saveProductSort(@RequestBody List<Map<String, Object>> sortData) {
        for (Map<String, Object> item : sortData) {
            String productId = (String) item.get("id");
            Integer sortOrder = (Integer) item.get("sort");
            ProductInfo product = productService.findOne(productId);
            if (product != null) {
                product.setSortOrder(sortOrder);
                productService.save(product);
            }
        }
        return Map.of("code", 0, "msg", "排序已保存");
    }

    private void saveConfig(String key, String value) {
        ShopConfig config = shopConfigRepository.findById(key).orElse(new ShopConfig());
        config.setConfigKey(key);
        config.setConfigValue(value != null ? value : "");
        shopConfigRepository.save(config);
    }
}
