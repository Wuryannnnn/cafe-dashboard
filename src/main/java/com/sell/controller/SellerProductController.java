package com.sell.controller;

import com.sell.dataobject.ProductCategory;
import com.sell.dataobject.ProductInfo;
import com.sell.dataobject.ProductSku;
import com.sell.exception.SellException;
import com.sell.form.ProductForm;
import com.sell.repository.ProductInfoRepository;
import com.sell.repository.ProductSkuRepository;
import com.sell.service.CategoryService;
import com.sell.service.ProductService;
import com.sell.utils.KeyUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import jakarta.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/seller/product")
public class SellerProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductInfoRepository productInfoRepository;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    /**
     * 菜品管理 - 类似美团管家
     */
    @GetMapping("/list")
    public ModelAndView list(@RequestParam(value = "categoryType", required = false) Integer categoryType,
                             @RequestParam(value = "keyword", required = false) String keyword,
                             Map<String, Object> map) {
        // 所有类目(左侧导航)
        List<ProductCategory> categoryList = categoryService.findAll();
        categoryList.sort(Comparator.comparingInt(c -> c.getSortOrder() != null ? c.getSortOrder() : 0));
        map.put("categoryList", categoryList);
        map.put("currentCategoryType", categoryType);
        map.put("keyword", keyword != null ? keyword : "");

        // 按条件查商品
        List<ProductInfo> productList;
        if (keyword != null && !keyword.trim().isEmpty()) {
            productList = productInfoRepository.findByProductNameContaining(keyword.trim());
        } else if (categoryType != null) {
            productList = productInfoRepository.findByCategoryType(categoryType);
        } else {
            productList = productInfoRepository.findAll();
        }
        productList.sort(Comparator.comparingInt(p -> p.getSortOrder() != null ? p.getSortOrder() : 0));
        map.put("productList", productList);

        // 批量查SKU
        List<String> productIds = productList.stream().map(ProductInfo::getProductId).collect(Collectors.toList());
        Map<String, List<ProductSku>> skuMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<ProductSku> allSkus = productSkuRepository.findByProductIdIn(productIds);
            skuMap = allSkus.stream().collect(Collectors.groupingBy(ProductSku::getProductId));
        }
        map.put("skuMap", skuMap);

        // 类目名映射
        Map<Integer, String> categoryNameMap = categoryList.stream()
                .collect(Collectors.toMap(ProductCategory::getCategoryType, ProductCategory::getCategoryName, (a, b) -> a));
        map.put("categoryNameMap", categoryNameMap);

        return new ModelAndView("product/list", map);
    }

    @RequestMapping("/on_sale")
    public ModelAndView onSale(@RequestParam("productId") String productId,
                               @RequestParam(value = "categoryType", required = false) Integer categoryType,
                               Map<String, Object> map) {
        try {
            productService.onSale(productId);
        } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/product/list" + (categoryType != null ? "?categoryType=" + categoryType : ""));
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/product/list" + (categoryType != null ? "?categoryType=" + categoryType : ""));
        return new ModelAndView("common/success", map);
    }

    @RequestMapping("/off_sale")
    public ModelAndView offSale(@RequestParam("productId") String productId,
                                @RequestParam(value = "categoryType", required = false) Integer categoryType,
                                Map<String, Object> map) {
        try {
            productService.offSale(productId);
        } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/product/list" + (categoryType != null ? "?categoryType=" + categoryType : ""));
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/product/list" + (categoryType != null ? "?categoryType=" + categoryType : ""));
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/index")
    public ModelAndView index(@RequestParam(value = "productId", required = false) String productId,
                      Map<String, Object> map) {
        if (!StringUtils.isEmpty(productId)) {
            ProductInfo productInfo = productService.findOne(productId);
            map.put("productInfo", productInfo);
            // 加载该商品的SKU
            List<ProductSku> skuList = productSkuRepository.findByProductId(productId);
            map.put("skuList", skuList);
        }
        List<ProductCategory> categoryList = categoryService.findAll();
        map.put("categoryList", categoryList);
        return new ModelAndView("product/index", map);
    }

    @PostMapping("/save")
    @CacheEvict(cacheNames = "product", allEntries = true, beforeInvocation = true)
    public ModelAndView save(@Valid ProductForm form,
                             BindingResult bindingResult,
                             Map<String, Object> map) {
        if (bindingResult.hasErrors()) {
            map.put("msg", bindingResult.getFieldError().getDefaultMessage());
            map.put("url", "/sell/seller/product/index");
            return new ModelAndView("common/error", map);
        }
        ProductInfo productInfo = new ProductInfo();
        try {
            if (!StringUtils.isEmpty(form.getProductId())) {
                productInfo = productService.findOne(form.getProductId());
            } else {
                form.setProductId(KeyUtil.genUniqueKey());
            }
            BeanUtils.copyProperties(form, productInfo);
            productService.save(productInfo);
        } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/product/index");
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/product/list");
        return new ModelAndView("common/success", map);
    }

    /**
     * 保存商品SKU(AJAX)
     */
    @PostMapping("/saveSku")
    @ResponseBody
    public Map<String, Object> saveSku(@RequestBody Map<String, Object> body) {
        String productId = (String) body.get("productId");
        List<Map<String, Object>> skus = (List<Map<String, Object>>) body.get("skus");

        // 删除旧SKU再重建
        List<ProductSku> oldSkus = productSkuRepository.findByProductId(productId);
        productSkuRepository.deleteAll(oldSkus);

        for (Map<String, Object> s : skus) {
            ProductSku sku = new ProductSku();
            sku.setSkuId(KeyUtil.genUniqueKey());
            sku.setProductId(productId);
            sku.setSkuName((String) s.get("name"));
            sku.setSkuPrice(new java.math.BigDecimal(s.get("price").toString()));
            sku.setSkuStock(s.get("stock") != null ? Integer.parseInt(s.get("stock").toString()) : 999);
            productSkuRepository.save(sku);
        }

        return Map.of("code", 0, "msg", "规格已保存");
    }

    /** 真删商品 (含其下 SKU); 已被订单引用的商品仅做停售标记保留以免破坏报表 */
    @GetMapping("/delete")
    public ModelAndView delete(@RequestParam("productId") String productId,
                               Map<String, Object> map) {
        ProductInfo p = productInfoRepository.findById(productId).orElse(null);
        if (p == null) {
            map.put("msg", "商品不存在");
            map.put("url", "/sell/seller/product/list");
            return new ModelAndView("common/error", map);
        }
        // 先清掉规格
        List<ProductSku> skus = productSkuRepository.findByProductId(productId);
        if (!skus.isEmpty()) productSkuRepository.deleteAll(skus);
        try {
            productInfoRepository.deleteById(productId);
        } catch (Exception e) {
            // 被订单等外键引用, 退化为停售
            p.setProductStatus(1);
            productInfoRepository.save(p);
            map.put("msg", "该商品已有订单记录, 已改为停售保留");
            map.put("url", "/sell/seller/product/list");
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/product/list");
        return new ModelAndView("common/success", map);
    }
}
