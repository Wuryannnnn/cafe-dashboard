package com.sell.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sell.config.WechatMiniConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 调微信 jscode2session 换 openid/session_key 的封装 (独立组件, 便于 mock).
 * 失败(errcode!=0 / 异常 / 缺 openid)统一返回 null, 由 WxMiniLoginService 转可读错误.
 */
@Component
@Slf4j
public class Jscode2SessionClient {

    @Autowired
    private WechatMiniConfig cfg;

    private final RestTemplate rest = new RestTemplate();

    /** 登录态会话: openid + session_key (session_key 仅后端用). */
    public static class Session {
        public final String openid;
        public final String sessionKey;
        public Session(String openid, String sessionKey) {
            this.openid = openid;
            this.sessionKey = sessionKey;
        }
    }

    public Session exchange(String code) {
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + cfg.getAppId()
                + "&secret=" + cfg.getAppSecret()
                + "&js_code=" + code
                + "&grant_type=authorization_code";
        try {
            String body = rest.getForObject(url, String.class);
            if (body == null) return null;
            JsonObject o = JsonParser.parseString(body).getAsJsonObject();
            if (o.has("errcode") && !o.get("errcode").isJsonNull() && o.get("errcode").getAsInt() != 0) {
                log.warn("[小程序登录] jscode2session 失败: {}", body);
                return null;
            }
            if (!o.has("openid")) return null;
            String sk = o.has("session_key") ? o.get("session_key").getAsString() : null;
            return new Session(o.get("openid").getAsString(), sk);
        } catch (Exception e) {
            log.warn("[小程序登录] jscode2session 异常: {}", e.getMessage());
            return null;
        }
    }
}
