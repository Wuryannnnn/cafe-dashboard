package com.sell.service.impl;

import com.sell.dataobject.OrderDetail;
import com.sell.dataobject.PrinterConfig;
import com.sell.dataobject.ProductCategory;
import com.sell.dataobject.ProductInfo;
import com.sell.dto.OrderDTO;
import com.sell.dto.PrintTicketDTO;
import com.sell.enums.DiningTypeEnum;
import com.sell.enums.PrintStationEnum;
import com.sell.repository.PrinterConfigRepository;
import com.sell.service.CategoryService;
import com.sell.service.OrderService;
import com.sell.service.PrinterService;
import com.sell.service.ProductService;
import com.sell.utils.EnumUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PrinterServiceImpl implements PrinterService {

    @Autowired
    private PrinterConfigRepository printerConfigRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Override
    @Async
    public void printOrder(OrderDTO orderDTO) {
        // 1. 按工位拆分订单明细
        Map<Integer, List<OrderDetail>> stationItems = splitByStation(orderDTO.getOrderDetailList());

        // 2. 对每个工位生成小票并发送到对应打印机
        for (Map.Entry<Integer, List<OrderDetail>> entry : stationItems.entrySet()) {
            Integer station = entry.getKey();
            List<OrderDetail> items = entry.getValue();

            PrintStationEnum stationEnum = EnumUtil.getByCode(station, PrintStationEnum.class);
            String stationName = stationEnum != null ? stationEnum.getMessage() : "未知工位";

            // 组装小票内容
            PrintTicketDTO ticket = new PrintTicketDTO();
            ticket.setStationName(stationName);
            ticket.setOrderId(orderDTO.getOrderId());
            ticket.setCreateTime(orderDTO.getCreateTime() != null ? orderDTO.getCreateTime() : new Date());
            ticket.setItems(items);

            DiningTypeEnum diningEnum = EnumUtil.getByCode(
                    orderDTO.getDiningType() != null ? orderDTO.getDiningType() : 0,
                    DiningTypeEnum.class);
            ticket.setDiningType(diningEnum != null ? diningEnum.getMessage() : "堂食");
            ticket.setTableNumber(orderDTO.getTableNumber());
            ticket.setPickupNumber(orderDTO.getPickupNumber());
            ticket.setRemark(orderDTO.getOrderRemark());

            // 查找该工位的打印机
            List<PrinterConfig> printers = printerConfigRepository.findByStationAndEnabledTrue(station);
            if (printers.isEmpty()) {
                log.warn("【小票打印】工位[{}]没有配置打印机, 跳过", stationName);
                continue;
            }

            // 生成ESC/POS指令
            byte[] ticketData = buildEscPosTicket(ticket);

            // 发送到每台打印机(同一工位可能有多台)
            for (PrinterConfig printer : printers) {
                sendToPrinter(printer, ticketData);
            }
        }
    }

    @Override
    @Async
    public void printCustomerReceipt(OrderDTO orderDTO) {
        // 顾客小票打到吧台打印机(前台)
        List<PrinterConfig> printers = printerConfigRepository.findByStationAndEnabledTrue(PrintStationEnum.BAR.getCode());
        if (printers.isEmpty()) {
            log.warn("【顾客小票】没有配置吧台打印机, 跳过");
            return;
        }

        byte[] data = buildCustomerReceipt(orderDTO);
        for (PrinterConfig printer : printers) {
            sendToPrinter(printer, data);
        }
    }

    @Override
    public void reprintOrder(String orderId) {
        OrderDTO orderDTO = orderService.findOne(orderId);
        printOrder(orderDTO);
    }

    /**
     * 生成顾客消费小票(带金额明细)
     */
    private byte[] buildCustomerReceipt(OrderDTO orderDTO) {
        StringBuilder sb = new StringBuilder();

        sb.append("\u001B\u0040"); // 初始化

        // 标题
        sb.append("\u001B\u0045\u0001");
        sb.append("\u001D\u0021\u0011");
        sb.append(centerText("咖啡厅", 16)).append("\n");
        sb.append("\u001D\u0021\u0000");
        sb.append("\u001B\u0045\u0000");

        sb.append("================================\n");

        // 取餐号
        if (orderDTO.getPickupNumber() != null) {
            sb.append("\u001D\u0021\u0011");
            sb.append("取餐号: #").append(orderDTO.getPickupNumber()).append("\n");
            sb.append("\u001D\u0021\u0000");
        }

        DiningTypeEnum diningEnum = EnumUtil.getByCode(
                orderDTO.getDiningType() != null ? orderDTO.getDiningType() : 0,
                DiningTypeEnum.class);
        sb.append(diningEnum != null ? diningEnum.getMessage() : "堂食");
        if (orderDTO.getTableNumber() != null && !orderDTO.getTableNumber().isEmpty()) {
            sb.append("  桌号: ").append(orderDTO.getTableNumber());
        }
        sb.append("\n");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        sb.append("时间: ").append(sdf.format(orderDTO.getCreateTime() != null ? orderDTO.getCreateTime() : new Date())).append("\n");
        sb.append("--------------------------------\n");

        // 商品明细
        BigDecimal total = BigDecimal.ZERO;
        for (OrderDetail item : orderDTO.getOrderDetailList()) {
            sb.append(item.getProductName());
            if (item.getSkuName() != null && !item.getSkuName().isEmpty()) {
                sb.append("(").append(item.getSkuName()).append(")");
            }
            sb.append("\n");

            int qty = item.getProductQuantity() != null ? item.getProductQuantity() : 0;
            BigDecimal unit = item.getProductPrice() != null ? item.getProductPrice() : BigDecimal.ZERO;
            BigDecimal linePrice = unit.multiply(new BigDecimal(qty));
            BigDecimal addonFee = item.getAddonFee() != null ?
                    item.getAddonFee().multiply(new BigDecimal(qty)) : BigDecimal.ZERO;

            sb.append("  x").append(qty);
            sb.append("              ")
              .append(linePrice.add(addonFee).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
              .append("元\n");

            if (item.getAddons() != null && !item.getAddons().isEmpty() && !item.getAddons().equals("[]")) {
                String addonNames = parseAddonNames(item.getAddons());
                if (!addonNames.isEmpty()) {
                    sb.append("  +").append(addonNames).append("\n");
                }
            }

            total = total.add(linePrice).add(addonFee);
        }

        sb.append("--------------------------------\n");
        sb.append("\u001B\u0045\u0001");
        sb.append("合计: ")
          .append((orderDTO.getOrderAmount() != null ? orderDTO.getOrderAmount() : total)
                  .setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
          .append("元\n");
        sb.append("\u001B\u0045\u0000");

        if (orderDTO.getOrderRemark() != null && !orderDTO.getOrderRemark().isEmpty()) {
            sb.append("备注: ").append(orderDTO.getOrderRemark()).append("\n");
        }

        sb.append("================================\n");
        sb.append(centerText("谢谢光临", 32)).append("\n");
        sb.append("\n\n\n");
        sb.append("\u001D\u0056\u0042\u0000"); // 切纸

        try {
            return sb.toString().getBytes("GBK");
        } catch (Exception e) {
            return sb.toString().getBytes();
        }
    }

    /**
     * 按工位拆分订单明细
     * 根据商品所属类目的printStation字段, 将明细分到不同工位
     */
    private Map<Integer, List<OrderDetail>> splitByStation(List<OrderDetail> details) {
        // 收集所有商品ID, 批量查商品信息拿到categoryType
        Map<String, ProductInfo> productMap = new HashMap<>();
        for (OrderDetail detail : details) {
            if (!productMap.containsKey(detail.getProductId())) {
                ProductInfo product = productService.findOne(detail.getProductId());
                if (product != null) {
                    productMap.put(detail.getProductId(), product);
                }
            }
        }

        // 收集所有categoryType, 批量查类目拿到printStation
        Set<Integer> categoryTypes = productMap.values().stream()
                .map(ProductInfo::getCategoryType)
                .collect(Collectors.toSet());
        List<ProductCategory> categories = categoryService.findByCategoryTypeIn(new ArrayList<>(categoryTypes));
        Map<Integer, Integer> categoryStationMap = categories.stream()
                .collect(Collectors.toMap(
                        ProductCategory::getCategoryType,
                        c -> c.getPrintStation() != null ? c.getPrintStation() : PrintStationEnum.BAR.getCode()
                ));

        // 按工位分组
        Map<Integer, List<OrderDetail>> result = new HashMap<>();
        for (OrderDetail detail : details) {
            ProductInfo product = productMap.get(detail.getProductId());
            int station = PrintStationEnum.BAR.getCode(); // 默认吧台
            if (product != null) {
                Integer categoryStation = categoryStationMap.get(product.getCategoryType());
                if (categoryStation != null) {
                    station = categoryStation;
                }
            }
            result.computeIfAbsent(station, k -> new ArrayList<>()).add(detail);
        }

        return result;
    }

    /**
     * 生成ESC/POS热敏打印机指令
     * 58mm小票纸, 一行最多32个英文字符(16个中文)
     */
    private byte[] buildEscPosTicket(PrintTicketDTO ticket) {
        StringBuilder sb = new StringBuilder();

        // ESC/POS 初始化
        sb.append("\u001B\u0040"); // ESC @ 初始化打印机

        // === 标题: 加粗+放大 ===
        sb.append("\u001B\u0045\u0001"); // ESC E 1 加粗
        sb.append("\u001D\u0021\u0011"); // GS ! 0x11 宽高各放大2倍
        sb.append(centerText(ticket.getStationName() + "制作单", 16));
        sb.append("\n");
        sb.append("\u001D\u0021\u0000"); // 恢复正常大小
        sb.append("\u001B\u0045\u0000"); // 取消加粗

        // === 分隔线 ===
        sb.append("================================\n");

        // === 取餐号(超大字) ===
        if (ticket.getPickupNumber() != null && !ticket.getPickupNumber().isEmpty()) {
            sb.append("\u001D\u0021\u0011"); // 放大2倍
            sb.append("取餐号: #").append(ticket.getPickupNumber());
            sb.append("\n");
            sb.append("\u001D\u0021\u0000"); // 恢复正常
        }

        // === 就餐方式 + 桌号(大字) ===
        sb.append("\u001D\u0021\u0011"); // 放大2倍
        sb.append(ticket.getDiningType());
        if (ticket.getTableNumber() != null && !ticket.getTableNumber().isEmpty()) {
            sb.append("  桌号: ").append(ticket.getTableNumber());
        }
        sb.append("\n");
        sb.append("\u001D\u0021\u0000"); // 恢复正常

        // === 订单信息 ===
        sb.append("订单号: ").append(ticket.getOrderId().substring(0, Math.min(16, ticket.getOrderId().length()))).append("\n");
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
        sb.append("时间: ").append(sdf.format(ticket.getCreateTime())).append("\n");
        sb.append("--------------------------------\n");

        // === 商品明细 ===
        int itemNo = 1;
        for (OrderDetail item : ticket.getItems()) {
            // 商品名 x数量
            sb.append(itemNo).append(". ").append(item.getProductName());
            sb.append(" x").append(item.getProductQuantity()).append("\n");

            // 规格 + 加料 (缩进显示)
            StringBuilder specLine = new StringBuilder("   ");
            boolean hasSpec = false;
            if (item.getSkuName() != null && !item.getSkuName().isEmpty()) {
                specLine.append(item.getSkuName());
                hasSpec = true;
            }
            if (item.getAddons() != null && !item.getAddons().isEmpty() && !item.getAddons().equals("[]")) {
                // 解析JSON加料, 提取名称
                String addonNames = parseAddonNames(item.getAddons());
                if (!addonNames.isEmpty()) {
                    if (hasSpec) specLine.append(" / ");
                    specLine.append(addonNames);
                    hasSpec = true;
                }
            }
            if (hasSpec) {
                sb.append(specLine).append("\n");
            }

            itemNo++;
        }

        // === 备注 ===
        if (ticket.getRemark() != null && !ticket.getRemark().isEmpty()) {
            sb.append("--------------------------------\n");
            sb.append("\u001B\u0045\u0001"); // 加粗
            sb.append("备注: ").append(ticket.getRemark()).append("\n");
            sb.append("\u001B\u0045\u0000"); // 取消加粗
        }

        sb.append("================================\n");
        sb.append("\n\n\n"); // 走纸

        // ESC/POS 切纸
        sb.append("\u001D\u0056\u0042\u0000"); // GS V 66 0 切纸

        try {
            return sb.toString().getBytes("GBK"); // 热敏打印机通常用GBK编码
        } catch (Exception e) {
            log.error("【小票打印】编码转换失败", e);
            return sb.toString().getBytes();
        }
    }

    /**
     * 从JSON加料信息中提取名称
     * 输入: [{"name":"加浓","price":3},{"name":"燕麦奶","price":5}]
     * 输出: 加浓+燕麦奶
     */
    private String parseAddonNames(String addonsJson) {
        try {
            // 简单解析, 不引入额外JSON库
            List<String> names = new ArrayList<>();
            String[] parts = addonsJson.split("\"name\"\\s*:\\s*\"");
            for (int i = 1; i < parts.length; i++) {
                int end = parts[i].indexOf("\"");
                if (end > 0) {
                    names.add(parts[i].substring(0, end));
                }
            }
            return String.join("+", names);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 居中文本(按热敏打印机一行字符数)
     */
    private String centerText(String text, int lineWidth) {
        int textWidth = 0;
        for (char c : text.toCharArray()) {
            textWidth += (c > 127) ? 2 : 1; // 中文占2字符宽
        }
        if (textWidth >= lineWidth) return text;
        int padding = (lineWidth - textWidth) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < padding; i++) sb.append(' ');
        sb.append(text);
        return sb.toString();
    }

    /**
     * 通过TCP Socket发送ESC/POS指令到热敏打印机
     */
    private void sendToPrinter(PrinterConfig printer, byte[] data) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(printer.getIpAddress(), printer.getPort()), 3000);
            socket.setSoTimeout(3000);
            OutputStream out = socket.getOutputStream();
            out.write(data);
            out.flush();
            log.info("【小票打印】发送成功, 打印机={}, IP={}:{}", printer.getPrinterName(), printer.getIpAddress(), printer.getPort());
        } catch (IOException e) {
            log.error("【小票打印】发送失败, 打印机={}, IP={}:{}, 错误={}",
                    printer.getPrinterName(), printer.getIpAddress(), printer.getPort(), e.getMessage());
        }
    }
}
