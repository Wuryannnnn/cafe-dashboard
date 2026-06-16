package com.sell.controller;

import com.google.gson.JsonObject;
import com.sell.service.WxMiniAccessTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

/**
 * 桌台小程序码生成 (店主后台二维码页用): 调 wxacodeunlimit, scene=tableId.
 * 顾客扫码直达 pages/order/index 对应桌. 受店主后台 admin.auth 保护 (/seller/**).
 */
@RestController
@RequestMapping("/seller/qrcode")
@Slf4j
public class MiniQrCodeController {

    @Autowired private WxMiniAccessTokenService accessTokenService;

    private final RestTemplate rest = new RestTemplate();

    @GetMapping("/mini")
    public ResponseEntity<byte[]> mini(@RequestParam("tableId") Integer tableId) {
        String token = accessTokenService.getAccessToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("未配置小程序凭据".getBytes(StandardCharsets.UTF_8));
        }
        JsonObject body = new JsonObject();
        body.addProperty("page", "pages/order/index");
        body.addProperty("scene", String.valueOf(tableId)); // 数字 tableId, ≤32 字符且字符集安全
        body.addProperty("check_path", false);
        body.addProperty("env_version", "release");

        String url = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + token;
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        byte[] resp = rest.postForObject(url, new HttpEntity<>(body.toString(), h), byte[].class);

        // 失败时微信返回 JSON 错误体(以 '{' 开头)而非 PNG
        if (resp != null && resp.length > 0 && resp[0] == '{') {
            log.warn("[小程序码] 生成失败 tableId={}: {}", tableId, new String(resp, StandardCharsets.UTF_8));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        }
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(resp);
    }
}
