package com.sell.service.impl;

import com.sell.converter.OrderMaster2OrderDTOConverter;
import com.sell.dataobject.OrderDetail;
import com.sell.dataobject.OrderMaster;
import com.sell.dataobject.ProductInfo;
import com.sell.dataobject.ProductSku;
import com.sell.dto.CartDTO;
import com.sell.dto.OrderDTO;
import com.sell.enums.OrderStatusEnum;
import com.sell.enums.PayStatusEnum;
import com.sell.enums.ResultEnum;
import com.sell.exception.SellException;
import com.sell.repository.OrderDetailRepository;
import com.sell.repository.OrderMasterRepository;
import com.sell.repository.ProductSkuRepository;
import com.sell.service.*;
import com.sell.utils.KeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 2017-06-11 18:43
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private OrderMasterRepository orderMasterRepository;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    @Autowired
    private PayService payService;

    @Autowired
    private PushMessageService pushMessageService;

    @Autowired
    private PrinterService printerService;

    @Autowired
    private PickupNumberService pickupNumberService;

    @Autowired
    private WebSocket webSocket;

    @Autowired
    private com.sell.service.RestaurantTableService restaurantTableService;

    @Autowired
    private com.sell.service.RecipeService recipeService;

    @Autowired
    private com.sell.service.WmsClient wmsClient;

    @Autowired
    private com.sell.service.WmsRetryService wmsRetryService;

    @Autowired
    private com.sell.repository.ShopConfigRepository shopConfigRepoForWms;

    @Autowired
    private com.sell.repository.StockRecordRepository stockRecordRepository;

    @Autowired
    private com.sell.repository.OrderPaymentRecordRepository orderPaymentRecordRepository;

    /**
     * 记一条库存流水 (订单消耗/返还), 关联 orderId 便于对账.
     * recordType: 2=出库(下单消耗), 1=入库(取消/退款返还). delta 为带符号变动量.
     * 流水属审计旁路, 记录失败仅告警, 不回滚也不影响下单/退款主流程.
     */
    private void writeStockLedger(int recordType, String orderId, OrderDetail d, int delta) {
        try {
            com.sell.dataobject.StockRecord r = new com.sell.dataobject.StockRecord();
            r.setRecordType(recordType);
            r.setProductId(d.getProductId());
            r.setProductName(d.getProductName());
            r.setSkuId(d.getSkuId() != null && !d.getSkuId().isEmpty() ? d.getSkuId() : null);
            r.setSkuName(d.getSkuName());
            r.setDelta(delta);
            r.setOrderId(orderId);
            r.setOperator("system");
            r.setCreateTime(new java.util.Date());
            stockRecordRepository.save(r);
        } catch (Exception e) {
            log.warn("[库存流水] 记录失败 order={}, product={}: {}", orderId, d.getProductId(), e.getMessage());
        }
    }

    /** 读店铺布尔业务开关; 未配置(键不存在/空)时用 defaultOn 兜底, 保持改造前的既有行为. */
    private boolean isSwitchOn(String key, boolean defaultOn) {
        try {
            com.sell.dataobject.ShopConfig c = shopConfigRepoForWms.findById(key).orElse(null);
            if (c == null || c.getConfigValue() == null || c.getConfigValue().trim().isEmpty()) return defaultOn;
            return "true".equalsIgnoreCase(c.getConfigValue().trim());
        } catch (Exception e) { return defaultOn; }
    }

    @Override
    @Transactional
    public OrderDTO create(OrderDTO orderDTO) {

        // 扫码点餐总开关(switch.qrOrder): 关闭则拒绝顾客下单 (默认开, 未配置不拦, 不影响既有行为).
        // 仅 BuyerOrderController 走本方法, 故只作用于顾客扫码单, 不影响收银台.
        if (!isSwitchOn("switch.qrOrder", true)) {
            throw new SellException(ResultEnum.PARAM_ERROR.getCode(), "扫码点餐已关闭, 暂不接受下单");
        }

        // 桌台与桌号双向回填: 堂食扫码点餐时, tableId 和 tableNumber 任填一个均可
        if (orderDTO.getTableId() != null && (orderDTO.getTableNumber() == null || orderDTO.getTableNumber().isEmpty())) {
            com.sell.dataobject.RestaurantTable t = restaurantTableService.findOne(orderDTO.getTableId());
            if (t != null) orderDTO.setTableNumber(t.getTableCode());
        } else if (orderDTO.getTableId() == null && orderDTO.getTableNumber() != null && !orderDTO.getTableNumber().isEmpty()) {
            com.sell.dataobject.RestaurantTable t = restaurantTableService.findByTableCode(orderDTO.getTableNumber());
            if (t != null) orderDTO.setTableId(t.getTableId());
        }

        // 库存预检 (PreCheck): 配置开启 wms.preCheckStock=true 时, 下单前查 WMS 物料库存
        if (isPreCheckEnabled() && wmsClient.isConfigured()) {
            String shortageItem = checkWmsStockSufficient(orderDTO);
            if (shortageItem != null) {
                log.warn("【下单】WMS 库存不足: {}", shortageItem);
                throw new SellException(ResultEnum.PRODUCT_STOCK_ERROR.getCode(), "原料 " + shortageItem + " 库存不足");
            }
        }

        String orderId = KeyUtil.genUniqueKey();
        BigDecimal orderAmount = new BigDecimal(BigInteger.ZERO);

        // 需要扣减库存的 SKU (skuId -> 总数量); 仅收集 skuStock 非空(跟踪库存)的 SKU
        java.util.Map<String, Integer> skuDecrements = new java.util.HashMap<>();

        //1. 查询商品（数量, 价格）
        for (OrderDetail orderDetail: orderDTO.getOrderDetailList()) {
            ProductInfo productInfo =  productService.findOne(orderDetail.getProductId());
            if (productInfo == null) {
                throw new SellException(ResultEnum.PRODUCT_NOT_EXIST);
            }

            //2. 计算单价: 如果指定了SKU, 用SKU价格; 否则用商品基础价格
            BigDecimal unitPrice = productInfo.getProductPrice();
            if (orderDetail.getSkuId() != null && !orderDetail.getSkuId().isEmpty()) {
                ProductSku sku = productSkuRepository.findById(orderDetail.getSkuId()).orElse(null);
                if (sku == null) {
                    throw new SellException(ResultEnum.PRODUCT_SKU_NOT_EXIST);
                }
                unitPrice = sku.getSkuPrice();
                orderDetail.setSkuName(sku.getSkuName());
                // 收集需扣减的 SKU 库存 (skuStock 为空表示该 SKU 不跟踪库存, 跳过)
                if (sku.getSkuStock() != null) {
                    skuDecrements.merge(sku.getSkuId(), orderDetail.getProductQuantity(), Integer::sum);
                }
            }

            //加料费用
            BigDecimal addonFee = orderDetail.getAddonFee() != null ? orderDetail.getAddonFee() : BigDecimal.ZERO;

            //计算订单总价 = (单价 + 加料费) * 数量
            orderAmount = unitPrice.add(addonFee)
                    .multiply(new BigDecimal(orderDetail.getProductQuantity()))
                    .add(orderAmount);

            //订单详情入库
            String skuId = orderDetail.getSkuId();
            String skuName = orderDetail.getSkuName();
            String addons = orderDetail.getAddons();
            BigDecimal savedAddonFee = orderDetail.getAddonFee();
            orderDetail.setDetailId(KeyUtil.genUniqueKey());
            orderDetail.setOrderId(orderId);
            BeanUtils.copyProperties(productInfo, orderDetail);
            orderDetail.setSkuId(skuId);
            orderDetail.setSkuName(skuName);
            orderDetail.setAddons(addons);
            orderDetail.setAddonFee(savedAddonFee);
            // copyProperties 会用商品基础价覆盖明细单价, 这里改回实际成交单价(SKU价或基础价),
            // 否则 SKU 订单明细记的是基础价, 与订单总额、小票、退款依据不一致
            orderDetail.setProductPrice(unitPrice);
            orderDetailRepository.save(orderDetail);
        }


        //3. 生成取餐号
        String pickupNumber = pickupNumberService.generatePickupNumber();
        orderDTO.setPickupNumber(pickupNumber);

        //4. 写入订单数据库（orderMaster和orderDetail）
        OrderMaster orderMaster = new OrderMaster();
        orderDTO.setOrderId(orderId);
        BeanUtils.copyProperties(orderDTO, orderMaster);
        orderMaster.setOrderAmount(orderAmount);
        orderMaster.setOrderStatus(OrderStatusEnum.NEW.getCode());
        orderMaster.setPayStatus(PayStatusEnum.WAIT.getCode());
        orderMaster.setPickupNumber(pickupNumber);
        orderMaster.setCreateTime(new java.util.Date());
        orderMaster.setUpdateTime(new java.util.Date());
        orderMasterRepository.save(orderMaster);

        //4. 扣库存 (商品级 + SKU 级)
        List<CartDTO> cartDTOList = orderDTO.getOrderDetailList().stream().map(e ->
                new CartDTO(e.getProductId(), e.getProductQuantity())
        ).collect(Collectors.toList());
        productService.decreaseStock(cartDTOList);
        // SKU 级库存原子扣减 (skuStock>=数量 才扣, 防止 SKU 超卖)
        for (java.util.Map.Entry<String, Integer> e : skuDecrements.entrySet()) {
            int updated = productSkuRepository.decreaseSkuStock(e.getKey(), e.getValue());
            if (updated == 0) {
                throw new SellException(ResultEnum.PRODUCT_SKU_STOCK_ERROR);
            }
        }

        // 库存流水: 每条明细记一笔出库, 关联 orderId, 便于事后对账"库存为何变动"
        for (OrderDetail d : orderDTO.getOrderDetailList()) {
            writeStockLedger(2, orderId, d, d.getProductQuantity() == null ? 0 : -d.getProductQuantity());
        }

        // 自动接单(switch.autoAcceptOrder): 新订单自动转"制作中", 免去人工接单.
        // - 先食后付(switch.payBeforeServe 关): 下单即接 → 这里直接转制作中
        // - 先付后食(switch.payBeforeServe 开): 需先付款, 这里不转, 由 paid() 付款成功后再转
        boolean autoAccept = isSwitchOn("switch.autoAcceptOrder", false);
        boolean payBeforeServe = isSwitchOn("switch.payBeforeServe", false);
        if (autoAccept && !payBeforeServe) {
            orderMaster.setOrderStatus(OrderStatusEnum.MAKING.getCode());
            orderMasterRepository.save(orderMaster);
            orderDTO.setOrderStatus(OrderStatusEnum.MAKING.getCode());
        }

        //发送websocket消息
        webSocket.sendMessage(orderDTO.getOrderId());

        //按工位分单打印小票(异步, 不阻塞下单)
        printerService.printOrder(orderDTO);
        //打印顾客消费小票
        printerService.printCustomerReceipt(orderDTO);

        // WMS 出库 (按 BOM 配方扣减原料)
        // - OUT_OF_STOCK: WMS 原子扣减失败 → 回滚整个订单
        // - RETRYABLE / WMS 未配置: 入补偿队列, 订单照常完成
        triggerWmsShipment(orderDTO);

        return orderDTO;
    }

    /** 返还订单中各 SKU 的库存 (仅对跟踪库存的 SKU; 与下单时的 SKU 扣减对称). */
    private void restoreSkuStock(OrderDTO orderDTO) {
        if (CollectionUtils.isEmpty(orderDTO.getOrderDetailList())) return;
        java.util.Map<String, Integer> agg = new java.util.HashMap<>();
        for (OrderDetail d : orderDTO.getOrderDetailList()) {
            if (d.getSkuId() != null && !d.getSkuId().isEmpty() && d.getProductQuantity() != null) {
                agg.merge(d.getSkuId(), d.getProductQuantity(), Integer::sum);
            }
        }
        for (java.util.Map.Entry<String, Integer> e : agg.entrySet()) {
            productSkuRepository.increaseSkuStock(e.getKey(), e.getValue());
        }
    }

    /** 按 BOM 配方汇总订单消耗的 WMS 物料, 调 WMS 出库接口. */
    private void triggerWmsShipment(OrderDTO orderDTO) {
        java.util.List<java.util.Map<String, Object>> details = buildShipmentDetails(orderDTO);
        if (details.isEmpty()) return;

        com.sell.service.WmsClient.ShipmentResult result =
                wmsClient.createShipment(orderDTO.getOrderId(), details);

        if (result.isOk()) return;

        if (result.outOfStock()) {
            // 原料不足: 抛异常触发 @Transactional 回滚, 订单不入库
            throw new SellException(ResultEnum.PRODUCT_STOCK_ERROR.getCode(),
                    result.message != null ? result.message : "原料库存不足");
        }

        // 网络/配置问题: 入补偿队列, 订单照常完成, 由定时任务重试
        if (result.shouldRetry()) {
            try {
                wmsRetryService.enqueue(orderDTO.getOrderId(), details, result.message);
                log.warn("[WMS] order {} 进入补偿队列: {}", orderDTO.getOrderId(), result.message);
            } catch (Exception e) {
                log.error("[WMS] 写补偿队列失败 {}: {}", orderDTO.getOrderId(), e.getMessage());
            }
        }
    }

    /** 把 OrderDTO 汇总成 WMS shipment details. */
    private java.util.List<java.util.Map<String, Object>> buildShipmentDetails(OrderDTO orderDTO) {
        java.util.Map<Long, java.math.BigDecimal> agg = new java.util.HashMap<>();
        java.util.Map<Long, com.sell.dataobject.Recipe> sample = new java.util.HashMap<>();
        for (OrderDetail d : orderDTO.getOrderDetailList()) {
            List<com.sell.dataobject.Recipe> recipes = recipeService.findByProductSku(d.getProductId(), d.getSkuId());
            for (com.sell.dataobject.Recipe r : recipes) {
                java.math.BigDecimal need = r.getQuantity()
                        .multiply(java.math.BigDecimal.valueOf(d.getProductQuantity()));
                agg.merge(r.getWmsItemId(), need, java.math.BigDecimal::add);
                sample.putIfAbsent(r.getWmsItemId(), r);
            }
        }
        java.util.List<java.util.Map<String, Object>> details = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Long, java.math.BigDecimal> e : agg.entrySet()) {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            // ID 必须以 String 存; 否则 WmsRetryService 用 GSON 反序列化时
            // 19 位 Long 会被解成 Double, 末位精度丢失 (2045...137 → 2045...136), 导致 WMS 找不到 sku
            m.put("itemId", e.getKey().toString());
            m.put("quantity", e.getValue());
            com.sell.dataobject.Recipe s = sample.get(e.getKey());
            if (s != null && s.getWmsSkuId() != null) m.put("skuId", s.getWmsSkuId().toString());
            details.add(m);
        }
        return details;
    }

    /** 取消/退款时反向入库, 退还原料. */
    private void triggerWmsReturn(OrderDTO orderDTO) {
        if (!wmsClient.isConfigured()) return;
        java.util.Map<Long, java.math.BigDecimal> agg = new java.util.HashMap<>();
        java.util.Map<Long, com.sell.dataobject.Recipe> sample = new java.util.HashMap<>();
        for (OrderDetail d : orderDTO.getOrderDetailList()) {
            List<com.sell.dataobject.Recipe> recipes = recipeService.findByProductSku(d.getProductId(), d.getSkuId());
            for (com.sell.dataobject.Recipe r : recipes) {
                java.math.BigDecimal need = r.getQuantity()
                        .multiply(java.math.BigDecimal.valueOf(d.getProductQuantity()));
                agg.merge(r.getWmsItemId(), need, java.math.BigDecimal::add);
                sample.putIfAbsent(r.getWmsItemId(), r);
            }
        }
        if (agg.isEmpty()) return;
        java.util.List<java.util.Map<String, Object>> details = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Long, java.math.BigDecimal> e : agg.entrySet()) {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("itemId", e.getKey());
            m.put("quantity", e.getValue());
            com.sell.dataobject.Recipe s = sample.get(e.getKey());
            if (s != null && s.getWmsSkuId() != null) m.put("skuId", s.getWmsSkuId());
            details.add(m);
        }
        wmsClient.createReceipt(orderDTO.getOrderId(), details);
    }

    /** 是否开启下单库存预检 (默认关闭). */
    private boolean isPreCheckEnabled() {
        try {
            com.sell.dataobject.ShopConfig c = shopConfigRepoForWms.findById("wms.preCheckStock").orElse(null);
            return c != null && "true".equalsIgnoreCase(c.getConfigValue());
        } catch (Exception e) { return false; }
    }

    /** 检查 BOM 物料库存是否足够本订单消耗. 返回不足的物料名 (null = 足够). */
    private String checkWmsStockSufficient(OrderDTO orderDTO) {
        java.util.Map<Long, java.math.BigDecimal> need = new java.util.HashMap<>();
        java.util.Map<Long, String> nameMap = new java.util.HashMap<>();
        for (OrderDetail d : orderDTO.getOrderDetailList()) {
            for (com.sell.dataobject.Recipe r : recipeService.findByProductSku(d.getProductId(), d.getSkuId())) {
                java.math.BigDecimal req = r.getQuantity().multiply(java.math.BigDecimal.valueOf(d.getProductQuantity()));
                need.merge(r.getWmsItemId(), req, java.math.BigDecimal::add);
                nameMap.putIfAbsent(r.getWmsItemId(), r.getWmsItemName());
            }
        }
        if (need.isEmpty()) return null;
        java.util.List<java.util.Map<String, Object>> inv = wmsClient.listInventory();
        java.util.Map<String, java.math.BigDecimal> stockMap = new java.util.HashMap<>();
        for (java.util.Map<String, Object> i : inv) {
            Object id = i.get("itemId");
            Object qty = i.get("quantity");
            if (id != null && qty instanceof Number) {
                stockMap.merge(id.toString(),
                        new java.math.BigDecimal(qty.toString()),
                        java.math.BigDecimal::add);
            }
        }
        for (java.util.Map.Entry<Long, java.math.BigDecimal> e : need.entrySet()) {
            java.math.BigDecimal have = stockMap.getOrDefault(e.getKey().toString(), java.math.BigDecimal.ZERO);
            if (have.compareTo(e.getValue()) < 0) {
                return nameMap.getOrDefault(e.getKey(), "id=" + e.getKey()) + " (需 " + e.getValue() + ", 仅 " + have + ")";
            }
        }
        return null;
    }

    @Override
    public OrderDTO findOne(String orderId) {

        OrderMaster orderMaster = orderMasterRepository.findById(orderId).orElse(null);
        if (orderMaster == null) {
            throw new SellException(ResultEnum.ORDER_NOT_EXIST);
        }

        List<OrderDetail> orderDetailList = orderDetailRepository.findByOrderId(orderId);
        if (CollectionUtils.isEmpty(orderDetailList)) {
            throw new SellException(ResultEnum.ORDERDETAIL_NOT_EXIST);
        }

        OrderDTO orderDTO = new OrderDTO();
        BeanUtils.copyProperties(orderMaster, orderDTO);
        orderDTO.setOrderDetailList(orderDetailList);

        return orderDTO;
    }

    @Override
    public Page<OrderDTO> findList(String buyerOpenid, Pageable pageable) {
        Page<OrderMaster> orderMasterPage = orderMasterRepository.findByBuyerOpenid(buyerOpenid, pageable);

        List<OrderDTO> orderDTOList = OrderMaster2OrderDTOConverter.convert(orderMasterPage.getContent());

        return new PageImpl<OrderDTO>(orderDTOList, pageable, orderMasterPage.getTotalElements());
    }

    @Override
    @Transactional
    public OrderDTO cancel(OrderDTO orderDTO) {
        // 行锁重读: 串行化取消/退款/支付回调, 避免并发下重复退款 + 重复返还库存. 以锁内最新状态为准.
        OrderMaster orderMaster = orderMasterRepository.findByOrderIdForUpdate(orderDTO.getOrderId())
                .orElseThrow(() -> new SellException(ResultEnum.ORDER_NOT_EXIST));

        //判断订单状态: 已完结 / 已取消 / 已退款 的不能再取消 (防止重复退款 + 重复返库存)
        if (orderMaster.getOrderStatus().equals(OrderStatusEnum.FINISHED.getCode())
                || orderMaster.getOrderStatus().equals(OrderStatusEnum.CANCEL.getCode())
                || orderMaster.getOrderStatus().equals(OrderStatusEnum.REFUNDED.getCode())) {
            log.error("【取消订单】订单状态不正确, orderId={}, orderStatus={}", orderMaster.getOrderId(), orderMaster.getOrderStatus());
            throw new SellException(ResultEnum.ORDER_STATUS_ERROR);
        }

        //订单详情校验提前到外部退款之前, 避免"已退款却因详情为空回滚"导致钱退了但订单没动
        if (CollectionUtils.isEmpty(orderDTO.getOrderDetailList())) {
            log.error("【取消订单】订单中无商品详情, orderId={}", orderMaster.getOrderId());
            throw new SellException(ResultEnum.ORDER_DETAIL_EMPTY);
        }

        //如果已支付, 先退款 —— 退款失败抛异常回滚整个取消, 不会出现"已取消但没退钱"
        if (PayStatusEnum.SUCCESS.getCode().equals(orderMaster.getPayStatus())) {
            try {
                payService.refund(orderDTO);
            } catch (Exception e) {
                log.error("【取消订单】退款失败, orderId={}, msg={}", orderMaster.getOrderId(), e.getMessage());
                throw new SellException(ResultEnum.ORDER_REFUND_FAIL);
            }
            //标记已退款, 防止 refund 接口对同一单二次退款
            orderMaster.setPayStatus(PayStatusEnum.REFUND.getCode());
            orderDTO.setPayStatus(PayStatusEnum.REFUND.getCode());
        }

        //修改订单状态 (在锁定实体上改, 仅动状态字段, 避免 copyProperties 覆盖并发写入的其它字段)
        orderMaster.setOrderStatus(OrderStatusEnum.CANCEL.getCode());
        OrderMaster updateResult = orderMasterRepository.save(orderMaster);
        if (updateResult == null) {
            log.error("【取消订单】更新失败, orderMaster={}", orderMaster);
            throw new SellException(ResultEnum.ORDER_UPDATE_FAIL);
        }
        orderDTO.setOrderStatus(OrderStatusEnum.CANCEL.getCode());

        //返回库存 (取消必为未完结, 货未交付, 应返还)
        List<CartDTO> cartDTOList = orderDTO.getOrderDetailList().stream()
                .map(e -> new CartDTO(e.getProductId(), e.getProductQuantity()))
                .collect(Collectors.toList());
        productService.increaseStock(cartDTOList);
        restoreSkuStock(orderDTO);
        for (OrderDetail d : orderDTO.getOrderDetailList()) {
            writeStockLedger(1, orderDTO.getOrderId(), d, d.getProductQuantity() == null ? 0 : d.getProductQuantity());
        }

        // WMS 退还原料 (按 BOM 反向入库)
        try { triggerWmsReturn(orderDTO); } catch (Exception e) {
            log.warn("[WMS] return trigger failed for canceled order {}: {}", orderDTO.getOrderId(), e.getMessage());
        }

        return orderDTO;
    }

    @Override
    @Transactional
    public OrderDTO making(OrderDTO orderDTO) {
        // 行锁重读: 以锁内最新状态为准, 防止并发(如同时被取消)用过期 DTO 覆盖订单状态
        OrderMaster orderMaster = orderMasterRepository.findByOrderIdForUpdate(orderDTO.getOrderId())
                .orElseThrow(() -> new SellException(ResultEnum.ORDER_NOT_EXIST));
        //只有新订单才能开始制作
        if (!OrderStatusEnum.NEW.getCode().equals(orderMaster.getOrderStatus())) {
            log.error("【开始制作】订单状态不正确, orderId={}, orderStatus={}", orderMaster.getOrderId(), orderMaster.getOrderStatus());
            throw new SellException(ResultEnum.ORDER_STATUS_ERROR);
        }
        orderMaster.setOrderStatus(OrderStatusEnum.MAKING.getCode());
        orderMasterRepository.save(orderMaster);

        orderDTO.setOrderStatus(OrderStatusEnum.MAKING.getCode());
        return orderDTO;
    }

    @Override
    @Transactional
    public OrderDTO ready(OrderDTO orderDTO) {
        // 行锁重读: 以锁内最新状态为准, 防止并发取消后仍被改成待取餐
        OrderMaster orderMaster = orderMasterRepository.findByOrderIdForUpdate(orderDTO.getOrderId())
                .orElseThrow(() -> new SellException(ResultEnum.ORDER_NOT_EXIST));
        //只有制作中才能变为待取餐
        if (!OrderStatusEnum.MAKING.getCode().equals(orderMaster.getOrderStatus())) {
            log.error("【待取餐】订单状态不正确, orderId={}, orderStatus={}", orderMaster.getOrderId(), orderMaster.getOrderStatus());
            throw new SellException(ResultEnum.ORDER_STATUS_ERROR);
        }
        orderMaster.setOrderStatus(OrderStatusEnum.READY.getCode());
        orderMasterRepository.save(orderMaster);

        orderDTO.setOrderStatus(OrderStatusEnum.READY.getCode());
        //推送取餐通知
        pushMessageService.orderStatus(orderDTO);
        //发送websocket消息通知前端
        webSocket.sendMessage("order_ready:" + orderDTO.getOrderId());

        return orderDTO;
    }

    @Override
    @Transactional
    public OrderDTO finish(OrderDTO orderDTO) {
        // 行锁重读: 以锁内最新状态为准, 防止已完结/已取消/已退款订单被重复完结而覆盖终态
        OrderMaster orderMaster = orderMasterRepository.findByOrderIdForUpdate(orderDTO.getOrderId())
                .orElseThrow(() -> new SellException(ResultEnum.ORDER_NOT_EXIST));
        //待取餐 / 新订单 / 制作中 才能完结(兼容直接完结)
        Integer st = orderMaster.getOrderStatus();
        if (!OrderStatusEnum.READY.getCode().equals(st)
                && !OrderStatusEnum.NEW.getCode().equals(st)
                && !OrderStatusEnum.MAKING.getCode().equals(st)) {
            log.error("【完结订单】订单状态不正确, orderId={}, orderStatus={}", orderMaster.getOrderId(), st);
            throw new SellException(ResultEnum.ORDER_STATUS_ERROR);
        }
        orderMaster.setOrderStatus(OrderStatusEnum.FINISHED.getCode());
        orderMasterRepository.save(orderMaster);

        orderDTO.setOrderStatus(OrderStatusEnum.FINISHED.getCode());
        //推送微信模版消息
        pushMessageService.orderStatus(orderDTO);

        return orderDTO;
    }

    @Override
    @Transactional
    public OrderDTO paid(OrderDTO orderDTO) {
        // 行锁重读: 微信/支付宝可能在短时间内并发回调两次, 串行化避免重复处理. 以锁内最新状态为准.
        OrderMaster orderMaster = orderMasterRepository.findByOrderIdForUpdate(orderDTO.getOrderId())
                .orElseThrow(() -> new SellException(ResultEnum.ORDER_NOT_EXIST));

        //判断订单状态: 已取消/已退款的不能再标记支付; 其余(新单/制作中/待取/已完结)均可,
        //以兼容"先食后付"——顾客可能在订单已进入制作甚至完结后才付款.
        if (OrderStatusEnum.CANCEL.getCode().equals(orderMaster.getOrderStatus())
                || OrderStatusEnum.REFUNDED.getCode().equals(orderMaster.getOrderStatus())) {
            log.error("【订单支付完成】订单状态不正确, orderId={}, orderStatus={}", orderMaster.getOrderId(), orderMaster.getOrderStatus());
            throw new SellException(ResultEnum.ORDER_STATUS_ERROR);
        }

        //判断支付状态 (已是 SUCCESS 说明前一个回调已处理, 直接拦截, 保证幂等)
        if (!orderMaster.getPayStatus().equals(PayStatusEnum.WAIT.getCode())) {
            log.error("【订单支付完成】订单支付状态不正确, orderId={}, payStatus={}", orderMaster.getOrderId(), orderMaster.getPayStatus());
            throw new SellException(ResultEnum.ORDER_PAY_STATUS_ERROR);
        }

        //修改支付状态 (在锁定实体上改)
        orderMaster.setPayStatus(PayStatusEnum.SUCCESS.getCode());

        // 自动接单(先付后食): 付款成功后, 若订单仍是新单且开了自动接单, 自动转"制作中"
        if (OrderStatusEnum.NEW.getCode().equals(orderMaster.getOrderStatus())
                && isSwitchOn("switch.autoAcceptOrder", false)) {
            orderMaster.setOrderStatus(OrderStatusEnum.MAKING.getCode());
            orderDTO.setOrderStatus(OrderStatusEnum.MAKING.getCode());
        }

        OrderMaster updateResult = orderMasterRepository.save(orderMaster);
        if (updateResult == null) {
            log.error("【订单支付完成】更新失败, orderMaster={}", orderMaster);
            throw new SellException(ResultEnum.ORDER_UPDATE_FAIL);
        }
        orderDTO.setPayStatus(PayStatusEnum.SUCCESS.getCode());

        return orderDTO;
    }

    @Override
    public Page<OrderDTO> findList(Pageable pageable) {
        Page<OrderMaster> orderMasterPage = orderMasterRepository.findAll(pageable);

        List<OrderDTO> orderDTOList = OrderMaster2OrderDTOConverter.convert(orderMasterPage.getContent());

        return new PageImpl<>(orderDTOList, pageable, orderMasterPage.getTotalElements());
    }

    @Override
    @Transactional
    public OrderDTO refund(OrderDTO orderDTO) {
        // 行锁重读: 串行化, 防止并发重复退款 + 与 cancel 并发各返还一次库存. 以锁内最新状态为准.
        OrderMaster orderMaster = orderMasterRepository.findByOrderIdForUpdate(orderDTO.getOrderId())
                .orElseThrow(() -> new SellException(ResultEnum.ORDER_NOT_EXIST));

        // 已退款的不能重复退款 (订单状态或支付状态任一已标记退款)
        if (OrderStatusEnum.REFUNDED.getCode().equals(orderMaster.getOrderStatus())
                || PayStatusEnum.REFUND.getCode().equals(orderMaster.getPayStatus())) {
            log.error("【订单退款】订单已退款, 不能重复退款, orderId={}", orderMaster.getOrderId());
            throw new SellException(ResultEnum.ORDER_STATUS_ERROR);
        }
        // 退款必须基于已支付的订单
        if (!PayStatusEnum.SUCCESS.getCode().equals(orderMaster.getPayStatus())) {
            log.error("【订单退款】订单未支付, orderId={}", orderMaster.getOrderId());
            throw new SellException(ResultEnum.ORDER_NOT_PAID);
        }

        // 退款前是否已完结: 已完结=货已交付给顾客, 退款不应再返还库存(否则库存虚高).
        boolean wasFinished = OrderStatusEnum.FINISHED.getCode().equals(orderMaster.getOrderStatus());

        // 线下(现金/会员余额)收款 vs 线上(微信/支付宝)网关收款, 退款路径不同:
        // - 有收款流水 OrderPaymentRecord = 收银台线下收的款 → 退现金/退回余额是收银员人工动作,
        //   系统侧不调支付网关(否则没真实交易可退、必然报错, 退款按钮对现金单永远失败)
        // - 无收款流水 = 线上网关收的款 → 调网关原路退款, 失败必须抛出回滚(钱没退绝不能标记已退款)
        boolean offlinePaid = !orderPaymentRecordRepository
                .findByOrderIdOrderByPaymentIdAsc(orderMaster.getOrderId()).isEmpty();
        if (!offlinePaid) {
            try {
                payService.refund(orderDTO);
            } catch (Exception e) {
                log.error("【订单退款】支付通道退款失败, orderId={}, msg={}",
                        orderMaster.getOrderId(), e.getMessage());
                throw new SellException(ResultEnum.ORDER_REFUND_FAIL);
            }
        } else {
            log.info("【订单退款】线下收款订单, 系统侧标记退款(现金/余额由收银员人工退还), orderId={}",
                    orderMaster.getOrderId());
        }

        // 退款成功后置为已退款 (订单状态 + 支付状态双标记, 防止二次退款)
        orderMaster.setOrderStatus(OrderStatusEnum.REFUNDED.getCode());
        orderMaster.setPayStatus(PayStatusEnum.REFUND.getCode());
        OrderMaster updateResult = orderMasterRepository.save(orderMaster);
        if (updateResult == null) {
            throw new SellException(ResultEnum.ORDER_UPDATE_FAIL);
        }
        orderDTO.setOrderStatus(OrderStatusEnum.REFUNDED.getCode());
        orderDTO.setPayStatus(PayStatusEnum.REFUND.getCode());

        // 返还库存: 仅当退款前【未完结】(货还没交给顾客). 已完结订单退款不返库存, 避免库存虚高.
        if (!wasFinished && !CollectionUtils.isEmpty(orderDTO.getOrderDetailList())) {
            List<CartDTO> cartDTOList = orderDTO.getOrderDetailList().stream()
                    .map(e -> new CartDTO(e.getProductId(), e.getProductQuantity()))
                    .collect(Collectors.toList());
            productService.increaseStock(cartDTOList);
            restoreSkuStock(orderDTO);
            for (OrderDetail d : orderDTO.getOrderDetailList()) {
                writeStockLedger(1, orderDTO.getOrderId(), d, d.getProductQuantity() == null ? 0 : d.getProductQuantity());
            }
        }

        // WMS 退还原料 (按 BOM 反向入库, 失败不影响退款)
        try { triggerWmsReturn(orderDTO); } catch (Exception e) {
            log.warn("[WMS] return trigger failed for refunded order {}: {}", orderDTO.getOrderId(), e.getMessage());
        }
        return orderDTO;
    }

    @Override
    @Transactional
    public OrderDTO updateAmount(String orderId, BigDecimal newAmount) {
        if (newAmount == null || newAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new SellException(ResultEnum.NEW_AMOUNT_ERROR);
        }
        OrderDTO orderDTO = findOne(orderId);
        // 已支付不能改价 (避免对账异常)
        if (PayStatusEnum.SUCCESS.getCode().equals(orderDTO.getPayStatus())) {
            throw new SellException(ResultEnum.ORDER_PAY_STATUS_ERROR);
        }
        OrderMaster orderMaster = new OrderMaster();
        BeanUtils.copyProperties(orderDTO, orderMaster);
        orderMaster.setOrderAmount(newAmount);
        orderMasterRepository.save(orderMaster);
        orderDTO.setOrderAmount(newAmount);
        return orderDTO;
    }

    @Override
    @Transactional
    public OrderDTO updatePayType(String orderId, Integer payType) {
        OrderDTO orderDTO = findOne(orderId);
        if (payType == null || payType.equals(orderDTO.getPayType())) {
            return orderDTO;
        }
        // 已支付 / 已退款的订单保持原支付渠道, 避免与已发生的支付、退款渠道不一致
        if (PayStatusEnum.SUCCESS.getCode().equals(orderDTO.getPayStatus())
                || PayStatusEnum.REFUND.getCode().equals(orderDTO.getPayStatus())) {
            return orderDTO;
        }
        OrderMaster orderMaster = new OrderMaster();
        BeanUtils.copyProperties(orderDTO, orderMaster);
        orderMaster.setPayType(payType);
        orderMasterRepository.save(orderMaster);
        orderDTO.setPayType(payType);
        return orderDTO;
    }

    @Override
    @Transactional
    public OrderDTO applyDiscount(String orderId, int discountRate) {
        if (discountRate <= 0 || discountRate > 100) {
            throw new SellException(ResultEnum.DISCOUNT_RATE_ERROR);
        }
        OrderDTO orderDTO = findOne(orderId);
        BigDecimal discounted = orderDTO.getOrderAmount()
                .multiply(BigDecimal.valueOf(discountRate))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        return updateAmount(orderId, discounted);
    }

    @Override
    @Transactional
    public OrderDTO freeOrder(String orderId) {
        return updateAmount(orderId, BigDecimal.ZERO);
    }
}
