package com.sell.service;

public interface QrCodeService {

    /** 生成二维码 PNG 字节数组. */
    byte[] generatePng(String content, int sizePx);

    /** 给桌台生成扫码点餐二维码 URL. */
    String buildTableQrUrl(String tableCode);
}
