package com.sell.controller;

import com.sell.VO.ProductAddonVO;
import com.sell.VO.ProductInfoVO;
import com.sell.VO.ProductSkuVO;
import com.sell.VO.ProductVO;
import com.sell.VO.ResultVO;
import com.sell.dataobject.ProductAddon;
import com.sell.dataobject.ProductCategory;
import com.sell.dataobject.ProductInfo;
import com.sell.dataobject.ProductSku;
import com.sell.repository.ProductAddonRepository;
import com.sell.repository.ProductSkuRepository;
import com.sell.service.CategoryService;
import com.sell.service.ProductService;
import com.sell.utils.ResultVOUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 买家商品
 */
@RestController
@RequestMapping("/buyer/product")
public class BuyerProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    @Autowired
    private ProductAddonRepository productAddonRepository;

    @GetMapping("/list")
    public ResultVO list(@RequestParam(value = "sellerId", required = false) String sellerId) {
        //1. 查询所有的上架商品(按排序); 过滤掉"顾客端隐藏"的商品(h5Display=0), null 视为展示
        List<ProductInfo> productInfoList = productService.findUpAll().stream()
                .filter(p -> p.getH5Display() == null || p.getH5Display() != 0)
                .collect(Collectors.toList());
        productInfoList.sort(java.util.Comparator.comparingInt(p -> p.getSortOrder() != null ? p.getSortOrder() : 0));

        //2. 查询类目(按排序)
        List<Integer> categoryTypeList = productInfoList.stream()
                .map(e -> e.getCategoryType())
                .collect(Collectors.toList());
        List<ProductCategory> productCategoryList = categoryService.findByCategoryTypeIn(categoryTypeList);
        productCategoryList.sort(java.util.Comparator.comparingInt(c -> c.getSortOrder() != null ? c.getSortOrder() : 0));

        //3. 查询所有商品的SKU
        List<String> productIds = productInfoList.stream()
                .map(ProductInfo::getProductId)
                .collect(Collectors.toList());
        List<ProductSku> allSkus = productIds.isEmpty() ? new ArrayList<>() : productSkuRepository.findByProductIdIn(productIds);
        Map<String, List<ProductSku>> skuMap = allSkus.stream()
                .collect(Collectors.groupingBy(ProductSku::getProductId));

        //4. 查询加料选项
        List<ProductAddon> globalAddons = productAddonRepository.findByCategoryTypeIsNull();
        List<ProductAddon> categoryAddons = categoryTypeList.isEmpty() ? new ArrayList<>() : productAddonRepository.findByCategoryTypeIn(categoryTypeList);
        Map<Integer, List<ProductAddon>> categoryAddonMap = categoryAddons.stream()
                .collect(Collectors.groupingBy(ProductAddon::getCategoryType));

        //5. 数据拼装
        List<ProductVO> productVOList = new ArrayList<>();
        for (ProductCategory productCategory: productCategoryList) {
            ProductVO productVO = new ProductVO();
            productVO.setCategoryType(productCategory.getCategoryType());
            productVO.setCategoryName(productCategory.getCategoryName());

            List<ProductInfoVO> productInfoVOList = new ArrayList<>();
            for (ProductInfo productInfo: productInfoList) {
                if (productInfo.getCategoryType().equals(productCategory.getCategoryType())) {
                    ProductInfoVO productInfoVO = new ProductInfoVO();
                    BeanUtils.copyProperties(productInfo, productInfoVO);

                    // 填充SKU列表
                    List<ProductSku> skus = skuMap.get(productInfo.getProductId());
                    if (skus != null) {
                        List<ProductSkuVO> skuVOs = skus.stream().map(sku -> {
                            ProductSkuVO vo = new ProductSkuVO();
                            BeanUtils.copyProperties(sku, vo);
                            return vo;
                        }).collect(Collectors.toList());
                        productInfoVO.setSkuList(skuVOs);
                    }

                    // 填充加料列表(全局 + 当前类目)
                    List<ProductAddon> addons = new ArrayList<>(globalAddons);
                    List<ProductAddon> catAddons = categoryAddonMap.get(productCategory.getCategoryType());
                    if (catAddons != null) {
                        addons.addAll(catAddons);
                    }
                    if (!addons.isEmpty()) {
                        List<ProductAddonVO> addonVOs = addons.stream().map(addon -> {
                            ProductAddonVO vo = new ProductAddonVO();
                            BeanUtils.copyProperties(addon, vo);
                            return vo;
                        }).collect(Collectors.toList());
                        productInfoVO.setAddonList(addonVOs);
                    }

                    productInfoVOList.add(productInfoVO);
                }
            }
            productVO.setProductInfoVOList(productInfoVOList);
            productVOList.add(productVO);
        }

        return ResultVOUtil.success(productVOList);
    }
}
