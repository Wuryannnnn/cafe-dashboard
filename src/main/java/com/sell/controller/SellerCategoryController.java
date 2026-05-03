package com.sell.controller;

import com.sell.dataobject.ProductCategory;
import com.sell.exception.SellException;
import com.sell.form.CategoryForm;
import com.sell.repository.ProductInfoRepository;
import com.sell.service.CategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 卖家类目
 * 2017-07-23 21:06
 */
@Controller
@RequestMapping("/seller/category")
public class SellerCategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductInfoRepository productInfoRepository;

    /**
     * 类目列表
     * @param map
     * @return
     */
    @GetMapping("/list")
    public ModelAndView list(Map<String, Object> map) {
        List<ProductCategory> categoryList = categoryService.findAll();
        map.put("categoryList", categoryList);
        return new ModelAndView("category/list", map);
    }

    /**
     * 展示
     * @param categoryId
     * @param map
     * @return
     */
    @GetMapping("/index")
    public ModelAndView index(@RequestParam(value = "categoryId", required = false) Integer categoryId,
                              Map<String, Object> map) {
        if (categoryId != null) {
            ProductCategory productCategory = categoryService.findOne(categoryId);
            map.put("category", productCategory);
        }

        return new ModelAndView("category/index", map);
    }

    /**
     * 保存/更新
     * @param form
     * @param bindingResult
     * @param map
     * @return
     */
    @PostMapping("/save")
    public ModelAndView save(@Valid CategoryForm form,
                             BindingResult bindingResult,
                             Map<String, Object> map) {
        if (bindingResult.hasErrors()) {
            map.put("msg", bindingResult.getFieldError().getDefaultMessage());
            map.put("url", "/sell/seller/category/index");
            return new ModelAndView("common/error", map);
        }

        ProductCategory productCategory = new ProductCategory();
        try {
            if (form.getCategoryId() != null) {
                productCategory = categoryService.findOne(form.getCategoryId());
            }
            BeanUtils.copyProperties(form, productCategory);
            categoryService.save(productCategory);
        } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/category/index");
            return new ModelAndView("common/error", map);
        }

        map.put("url", "/sell/seller/category/list");
        return new ModelAndView("common/success", map);
    }

    /** 删除分类: 该分类 categoryType 下还有商品时拒绝删除 */
    @GetMapping("/delete")
    public ModelAndView delete(@RequestParam("categoryId") Integer categoryId,
                               Map<String, Object> map) {
        ProductCategory cat = categoryService.findOne(categoryId);
        if (cat == null) {
            map.put("msg", "分类不存在");
            map.put("url", "/sell/seller/category/list");
            return new ModelAndView("common/error", map);
        }
        long inUse = productInfoRepository.countByCategoryType(cat.getCategoryType());
        if (inUse > 0) {
            map.put("msg", "该分类下还有 " + inUse + " 件商品, 请先转移或删除");
            map.put("url", "/sell/seller/category/list");
            return new ModelAndView("common/error", map);
        }
        categoryService.delete(categoryId);
        map.put("url", "/sell/seller/category/list");
        return new ModelAndView("common/success", map);
    }
}
