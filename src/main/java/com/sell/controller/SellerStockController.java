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
import org.springframework.transaction.annotation.Transactional;
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

    /** 商品级调整 (无 SKU 时使用). 原子更新 + 同事务写流水, 避免并发"读-改-写"丢失更新、库存与流水劈叉. */
    @PostMapping("/adjust")
    @Transactional
    public ModelAndView adjust(@RequestParam("productId") String productId,
                               @RequestParam("delta") Integer delta,
                               @RequestParam(value = "remark", required = false) String remark,
                               @RequestParam(value = "recordType", defaultValue = "4") Integer recordType,
                               Map<String, Object> mapModel) {
        ProductInfo p = productRepo.findById(productId).orElse(null);
        if (p == null) {
            return err(mapModel, "商品不存在");
        }
        // 原子调整: 仅当调整后不为负才更新; 0 行表示会变负
        if (productRepo.adjustStockAtomic(productId, delta) == 0) {
            return err(mapModel, "调整后库存不能 < 0");
        }
        Integer after = productRepo.findById(productId).map(ProductInfo::getProductStock).orElse(0);
        recordRepo.save(buildRecord(recordType, productId, p.getProductName(), null, null, delta, after == null ? 0 : after, remark));
        mapModel.put("url", "/sell/seller/stock/list");
        return new ModelAndView("common/success", mapModel);
    }

    /** SKU 级调整. 原子更新 + 联动商品级库存(保持"商品级=各SKU之和"一致) + 同事务写流水. */
    @PostMapping("/adjustSku")
    @Transactional
    public ModelAndView adjustSku(@RequestParam("skuId") String skuId,
                                  @RequestParam("delta") Integer delta,
                                  @RequestParam(value = "remark", required = false) String remark,
                                  @RequestParam(value = "recordType", defaultValue = "4") Integer recordType,
                                  Map<String, Object> mapModel) {
        ProductSku s = skuRepo.findById(skuId).orElse(null);
        if (s == null) {
            return err(mapModel, "规格不存在");
        }
        // 原子调整 SKU 库存
        if (skuRepo.adjustSkuStockAtomic(skuId, delta) == 0) {
            return err(mapModel, "调整后库存不能 < 0");
        }
        // 两层联动: 把同样的 delta 同步到商品级库存, 保持两层不漂移,
        // 也避免商品级库存为 0/空时把该 SKU 的下单挡住(下单要先过商品级原子扣减).
        // delta 为负且商品级不足时原子更新返回 0(不更新、不会变负), 属可接受的轻微偏差.
        productRepo.adjustStockAtomic(s.getProductId(), delta);
        Integer skuAfter = skuRepo.findById(skuId).map(ProductSku::getSkuStock).orElse(0);
        ProductInfo p = productRepo.findById(s.getProductId()).orElse(null);
        recordRepo.save(buildRecord(recordType, s.getProductId(),
                p != null ? p.getProductName() : "",
                s.getSkuId(), s.getSkuName(), delta, skuAfter == null ? 0 : skuAfter, remark));
        mapModel.put("url", "/sell/seller/stock/list");
        return new ModelAndView("common/success", mapModel);
    }

    private ModelAndView err(Map<String, Object> mapModel, String msg) {
        mapModel.put("msg", msg);
        mapModel.put("url", "/sell/seller/stock/list");
        return new ModelAndView("common/error", mapModel);
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
