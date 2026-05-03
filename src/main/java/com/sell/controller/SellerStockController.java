package com.sell.controller;

import com.sell.dataobject.ProductInfo;
import com.sell.dataobject.ProductSku;
import com.sell.dataobject.StockRecord;
import com.sell.exception.SellException;
import com.sell.repository.ProductInfoRepository;
import com.sell.repository.ProductSkuRepository;
import com.sell.repository.StockRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;

/** 库存管理 */
@Controller
@RequestMapping("/seller/stock")
public class SellerStockController {

    @Autowired private ProductInfoRepository productRepo;
    @Autowired private ProductSkuRepository skuRepo;
    @Autowired private StockRecordRepository recordRepo;

    /** 库存总览: 商品级库存 + 各 SKU 库存. */
    @GetMapping("/list")
    public ModelAndView list(@RequestParam(value = "keyword", required = false) String keyword,
                             @RequestParam(value = "lowStock", defaultValue = "false") Boolean lowStock,
                             @RequestParam(value = "threshold", defaultValue = "10") Integer threshold,
                             Map<String, Object> map) {
        List<ProductInfo> products = (keyword != null && !keyword.trim().isEmpty())
                ? productRepo.findByProductNameContaining(keyword)
                : productRepo.findAll();

        if (lowStock) {
            products = products.stream().filter(p -> p.getProductStock() != null && p.getProductStock() <= threshold)
                    .toList();
        }

        // 取所有 SKU
        List<String> ids = products.stream().map(ProductInfo::getProductId).toList();
        Map<String, List<ProductSku>> skuMap = new HashMap<>();
        if (!ids.isEmpty()) {
            for (ProductSku s : skuRepo.findByProductIdIn(ids)) {
                skuMap.computeIfAbsent(s.getProductId(), k -> new ArrayList<>()).add(s);
            }
        }

        // 统计
        int totalProducts = products.size();
        int outOfStock = (int) products.stream().filter(p -> p.getProductStock() != null && p.getProductStock() == 0).count();
        int lowCount = (int) products.stream().filter(p -> p.getProductStock() != null && p.getProductStock() > 0 && p.getProductStock() <= threshold).count();

        map.put("products", products);
        map.put("skuMap", skuMap);
        map.put("keyword", keyword);
        map.put("lowStock", lowStock);
        map.put("threshold", threshold);
        map.put("totalProducts", totalProducts);
        map.put("outOfStock", outOfStock);
        map.put("lowCount", lowCount);
        return new ModelAndView("stock/list", map);
    }

    /** 商品级调整 (无 SKU 时使用). */
    @PostMapping("/adjust")
    public ModelAndView adjust(@RequestParam("productId") String productId,
                               @RequestParam("delta") Integer delta,
                               @RequestParam(value = "remark", required = false) String remark,
                               @RequestParam(value = "recordType", defaultValue = "4") Integer recordType,
                               Map<String, Object> mapModel) {
        ProductInfo p = productRepo.findById(productId).orElse(null);
        if (p == null) {
            mapModel.put("msg", "商品不存在");
            mapModel.put("url", "/sell/seller/stock/list");
            return new ModelAndView("common/error", mapModel);
        }
        int newStock = (p.getProductStock() == null ? 0 : p.getProductStock()) + delta;
        if (newStock < 0) {
            mapModel.put("msg", "调整后库存不能 < 0");
            mapModel.put("url", "/sell/seller/stock/list");
            return new ModelAndView("common/error", mapModel);
        }
        p.setProductStock(newStock);
        productRepo.save(p);
        recordRepo.save(buildRecord(recordType, p.getProductId(), p.getProductName(), null, null, delta, newStock, remark));
        mapModel.put("url", "/sell/seller/stock/list");
        return new ModelAndView("common/success", mapModel);
    }

    /** SKU 级调整. */
    @PostMapping("/adjustSku")
    public ModelAndView adjustSku(@RequestParam("skuId") String skuId,
                                  @RequestParam("delta") Integer delta,
                                  @RequestParam(value = "remark", required = false) String remark,
                                  @RequestParam(value = "recordType", defaultValue = "4") Integer recordType,
                                  Map<String, Object> mapModel) {
        ProductSku s = skuRepo.findById(skuId).orElse(null);
        if (s == null) {
            mapModel.put("msg", "规格不存在");
            mapModel.put("url", "/sell/seller/stock/list");
            return new ModelAndView("common/error", mapModel);
        }
        int newStock = (s.getSkuStock() == null ? 0 : s.getSkuStock()) + delta;
        if (newStock < 0) {
            mapModel.put("msg", "调整后库存不能 < 0");
            mapModel.put("url", "/sell/seller/stock/list");
            return new ModelAndView("common/error", mapModel);
        }
        s.setSkuStock(newStock);
        skuRepo.save(s);
        ProductInfo p = productRepo.findById(s.getProductId()).orElse(null);
        recordRepo.save(buildRecord(recordType, s.getProductId(),
                p != null ? p.getProductName() : "",
                s.getSkuId(), s.getSkuName(), delta, newStock, remark));
        mapModel.put("url", "/sell/seller/stock/list");
        return new ModelAndView("common/success", mapModel);
    }

    /** 单商品库存历史. */
    @GetMapping("/history")
    public ModelAndView history(@RequestParam("productId") String productId,
                                @RequestParam(value = "page", defaultValue = "1") Integer page,
                                Map<String, Object> map) {
        Page<StockRecord> records = recordRepo.findByProductId(productId,
                PageRequest.of(page - 1, 50, Sort.by(Sort.Direction.DESC, "recordId")));
        ProductInfo p = productRepo.findById(productId).orElse(null);
        map.put("records", records);
        map.put("product", p);
        map.put("currentPage", page);
        return new ModelAndView("stock/history", map);
    }

    private StockRecord buildRecord(int type, String productId, String productName,
                                    String skuId, String skuName, int delta, int stockAfter, String remark) {
        StockRecord r = new StockRecord();
        r.setRecordType(type);
        r.setProductId(productId);
        r.setProductName(productName);
        r.setSkuId(skuId);
        r.setSkuName(skuName);
        r.setDelta(delta);
        r.setStockAfter(stockAfter);
        r.setRemark(remark);
        r.setOperator("seller");
        r.setCreateTime(new Date());
        return r;
    }
}
