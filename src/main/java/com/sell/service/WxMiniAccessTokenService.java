package com.sell.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sell.config.WechatMiniConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 小程序全局 access_token (cgi-bin/token) 内存缓存.
 * 与登录用的 jscode2session 不是一回事; 发货上报、小程序码生成都用它.
 * 提前 5 分钟过期刷新; 未配置 appId/secret 时返回 null (调用方降级).
 */
@Service
@Slf4j
public class WxMiniAccessTokenService {

    @Autowired
    private WechatMiniConfig cfg;

    private final RestTemplate rest = new RestTemplate();

    private volatile String cachedToken;
    private volatile long expireAtMs;

    public synchronized String getAccessToken() {
        if (cachedToken != null && System.currentTimeMillis() < expireAtMs) {
            return cachedToken;
        }
        if (!cfg.isLoginConfigured()) {
            return null;
        }
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential"
                + "&appid=" + cfg.getAppId() + "&secret=" + cfg.getAppSecret();
        try {
            String body = rest.getForObject(url, String.class);
            if (body == null) return null;
            JsonObject o = JsonParser.parseString(body).getAsJsonObject();
            if (o.has("access_token")) {
                cachedToken = o.get("access_token").getAsString();
                int expiresIn = o.has("expires_in") ? o.get("expires_in").getAsInt() : 7200;
                expireAtMs = System.currentTimeMillis() + (expiresIn - 300) * 1000L;
                return cachedToken;
            }
            log.warn("[小程序] 获取 access_token 失败: {}", body);
        } catch (Exception e) {
            log.warn("[小程序] 获取 access_token 异常: {}", e.getMessage());
        }
        return null;
    }
}
