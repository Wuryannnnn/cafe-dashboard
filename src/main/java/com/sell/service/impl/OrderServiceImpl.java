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

    @Override
    @Transactional
    public OrderDTO create(OrderDTO orderDTO) {

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

//        List<CartDTO> cartDTOList = new ArrayList<>();

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

        //4. 扣库存
        List<CartDTO> cartDTOList = orderDTO.getOrderDetailList().stream().map(e ->
                new CartDTO(e.getProductId(), e.getProductQuantity())
        ).collect(Collectors.toList());
        productService.decreaseStock(cartDTOList);

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
        OrderMaster orderMaster = new OrderMaster();

        //判断订单状态: 已完结或已取消的不能再取消
        if (orderDTO.getOrderStatus().equals(OrderStatusEnum.FINISHED.getCode())
                || orderDTO.getOrderStatus().equals(OrderStatusEnum.CANCEL.getCode())) {
            log.error("【取消订单】订单状态不正确, orderId={}, orderStatus={}", orderDTO.getOrderId(), orderDTO.getOrderStatus());
            throw new SellException(ResultEnum.ORDER_STATUS_ERROR);
        }

        //修改订单状态
        orderDTO.setOrderStatus(OrderStatusEnum.CANCEL.getCode());
        BeanUtils.copyProperties(orderDTO, orderMaster);
        OrderMaster updateResult = orderMasterRepository.save(orderMaster);
        if (updateResult == null) {
            log.error("【取消订单】更新失败, orderMaster={}", orderMaster);
            throw new SellException(ResultEnum.ORDER_UPDATE_FAIL);
        }

        //返回库存
        if (CollectionUtils.isEmpty(orderDTO.getOrderDetailList())) {
            log.error("【取消订单】订单中无商品详情, orderDTO={}", orderDTO);
            throw new SellException(ResultEnum.ORDER_DETAIL_EMPTY);
        }
        List<CartDTO> cartDTOList = orderDTO.getOrderDetailList().stream()
                .map(e -> new CartDTO(e.getProductId(), e.getProductQuantity()))
                .collect(Collectors.toList());
        productService.increaseStock(cartDTOList);

        //如果已支付, 需要退款
        if (orderDTO.getPayStatus().equals(PayStatusEnum.SUCCESS.getCode())) {
            payService.refund(orderDTO);
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
        //判断订单状态: 只有新订单才能开始制作
        if (!orderDTO.getOrderStatus().equals(OrderStatusEnum.NEW.getCode())) {
            log.error("【开始制作】订单状态不正确, orderId={}, orderStatus={}", orderDTO.getOrderId(), orderDTO.getOrderStatus());
            throw new SellException(ResultEnum.ORDER_STATUS_ERROR);
        }

        orderDTO.setOrderStatus(OrderStatusEnum.MAKING.getCode());
        OrderMaster orderMaster = new OrderMaster();
        BeanUtils.copyProperties(orderDTO, orderMaster);
        OrderMaster updateResult = orderMasterRepository.save(orderMaster);
        if (updateResult == null) {
            log.error("【开始制作】更新失败, orderMaster={}", orderMaster);
            throw new SellException(ResultEnum.ORDER_UPDATE_FAIL);
        }

        return orderDTO;
    }

    @Override
    @Transactional
    public OrderDTO ready(OrderDTO orderDTO) {
        //判断订单状态: 只有制作中才能变为待取餐
        if (!orderDTO.getOrderStatus().equals(OrderStatusEnum.MAKING.getCode())) {
            log.error("【待取餐】订单状态不正确, orderId={}, orderStatus={}", orderDTO.getOrderId(), orderDTO.getOrderStatus());
            throw new SellException(ResultEnum.ORDER_STATUS_ERROR);
        }

        orderDTO.setOrderStatus(OrderStatusEnum.READY.getCode());
        OrderMaster orderMaster = new OrderMaster();
        BeanUtils.copyProperties(orderDTO, orderMaster);
        OrderMaster updateResult = orderMasterRepository.save(orderMaster);
        if (updateResult == null) {
            log.error("【待取餐】更新失败, orderMaster={}", orderMaster);
            throw new SellException(ResultEnum.ORDER_UPDATE_FAIL);
        }

        //推送取餐通知
        pushMessageService.orderStatus(orderDTO);
        //发送websocket消息通知前端
        webSocket.sendMessage("order_ready:" + orderDTO.getOrderId());

        return orderDTO;
    }

    @Override
    @Transactional
    public OrderDTO finish(OrderDTO orderDTO) {
        //判断订单状态: 待取餐 或 新订单(兼容直接完结) 才能完结
        if (!orderDTO.getOrderStatus().equals(OrderStatusEnum.READY.getCode())
                && !orderDTO.getOrderStatus().equals(OrderStatusEnum.NEW.getCode())
                && !orderDTO.getOrderStatus().equals(OrderStatusEnum.MAKING.getCode())) {
            log.error("【完结订单】订单状态不正确, orderId={}, orderStatus={}", orderDTO.getOrderId(), orderDTO.getOrderStatus());
            throw new SellException(ResultEnum.ORDER_STATUS_ERROR);
        }

        //修改订单状态
        orderDTO.setOrderStatus(OrderStatusEnum.FINISHED.getCode());
        OrderMaster orderMaster = new OrderMaster();
        BeanUtils.copyProperties(orderDTO, orderMaster);
        OrderMaster updateResult = orderMasterRepository.save(orderMaster);
        if (updateResult == null) {
            log.error("【完结订单】更新失败, orderMaster={}", orderMaster);
            throw new SellException(ResultEnum.ORDER_UPDATE_FAIL);
        }

        //推送微信模版消息
        pushMessageService.orderStatus(orderDTO);

        return orderDTO;
    }

    @Override
    @Transactional
    public OrderDTO paid(OrderDTO orderDTO) {
        //判断订单状态
        if (!orderDTO.getOrderStatus().equals(OrderStatusEnum.NEW.getCode())) {
            log.error("【订单支付完成】订单状态不正确, orderId={}, orderStatus={}", orderDTO.getOrderId(), orderDTO.getOrderStatus());
            throw new SellException(ResultEnum.ORDER_STATUS_ERROR);
        }

        //判断支付状态
        if (!orderDTO.getPayStatus().equals(PayStatusEnum.WAIT.getCode())) {
            log.error("【订单支付完成】订单支付状态不正确, orderDTO={}", orderDTO);
            throw new SellException(ResultEnum.ORDER_PAY_STATUS_ERROR);
        }

        //修改支付状态
        orderDTO.setPayStatus(PayStatusEnum.SUCCESS.getCode());
        OrderMaster orderMaster = new OrderMaster();
        BeanUtils.copyProperties(orderDTO, orderMaster);
        OrderMaster updateResult = orderMasterRepository.save(orderMaster);
        if (updateResult == null) {
            log.error("【订单支付完成】更新失败, orderMaster={}", orderMaster);
            throw new SellException(ResultEnum.ORDER_UPDATE_FAIL);
        }

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
        // PRD 6.3: 退款必须基于已支付的订单
        if (!PayStatusEnum.SUCCESS.getCode().equals(orderDTO.getPayStatus())) {
            log.error("【订单退款】订单未支付, orderId={}", orderDTO.getOrderId());
            throw new SellException(ResultEnum.ORDER_NOT_PAID);
        }
        // 已退款的不能重复退款
        if (OrderStatusEnum.REFUNDED.getCode().equals(orderDTO.getOrderStatus())) {
            throw new SellException(ResultEnum.ORDER_STATUS_ERROR);
        }

        // 调用支付通道退款
        try {
            payService.refund(orderDTO);
        } catch (Exception e) {
            log.warn("【订单退款】支付通道退款失败 (将继续更新订单状态), orderId={}, msg={}",
                    orderDTO.getOrderId(), e.getMessage());
        }

        // 退款后状态置为已退款
        orderDTO.setOrderStatus(OrderStatusEnum.REFUNDED.getCode());
        OrderMaster orderMaster = new OrderMaster();
        BeanUtils.copyProperties(orderDTO, orderMaster);
        OrderMaster updateResult = orderMasterRepository.save(orderMaster);
        if (updateResult == null) {
            throw new SellException(ResultEnum.ORDER_UPDATE_FAIL);
        }

        // 退款时返还库存 (如果订单还未到完结状态)
        if (!CollectionUtils.isEmpty(orderDTO.getOrderDetailList())) {
            List<CartDTO> cartDTOList = orderDTO.getOrderDetailList().stream()
                    .map(e -> new CartDTO(e.getProductId(), e.getProductQuantity()))
                    .collect(Collectors.toList());
            productService.increaseStock(cartDTOList);
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
