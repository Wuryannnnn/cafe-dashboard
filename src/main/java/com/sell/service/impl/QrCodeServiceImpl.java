package com.sell.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.sell.dataobject.ShopConfig;
import com.sell.repository.ShopConfigRepository;
import com.sell.service.QrCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class QrCodeServiceImpl implements QrCodeService {

    private static final String CONFIG_KEY_BASE_URL = "shop.base.url";

    @Autowired
    private ShopConfigRepository shopConfigRepository;

    @Override
    public byte[] generatePng(String content, int sizePx) {
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("二维码内容不能为空");
        }
        int size = (sizePx > 0) ? sizePx : 300;
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new RuntimeException("二维码生成失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String buildTableQrUrl(String tableCode) {
        String base = readConfiguredBaseUrl();
        if (base == null || base.isEmpty()) {
            base = inferRequestBaseUrl();
        }
        if (base != null && base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/order?table=" + (tableCode == null ? "" : tableCode);
    }

    private String readConfiguredBaseUrl() {
        ShopConfig cfg = shopConfigRepository.findById(CONFIG_KEY_BASE_URL).orElse(null);
        return cfg == null ? null : cfg.getConfigValue();
    }

    private String inferRequestBaseUrl() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "http://localhost:8080";
            HttpServletRequest req = attrs.getRequest();
            String scheme = req.getScheme();
            String host = req.getServerName();
            int port = req.getServerPort();
            boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
            return scheme + "://" + host + (defaultPort ? "" : ":" + port);
        } catch (Exception e) {
            return "http://localhost:8080";
        }
    }
}
