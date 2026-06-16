package com.sell.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 小程序登录 + 微信支付 APIv3 配置.
 * 生产读环境变量, 本地留占位; enabled=false 时不装配 APIv3 Bean, 应用照常启动.
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat.mini")
public class WechatMiniConfig {
    /** 是否启用 APIv3 支付 Bean 装配 (凭据齐全时设 true). 本地/CI 默认 false, 避免无凭据时启动失败. */
    private boolean enabled = false;
    private String appId;                  // 小程序 appId
    private String appSecret;              // 小程序密钥 (登录用, 仅后端)
    private String mchId;                  // 商户号
    private String merchantSerialNo;       // 商户 API 证书序列号
    private String privateKeyPath;         // 商户 API 私钥 apiclient_key.pem
    private String apiV3Key;               // APIv3 密钥
    private String wechatPayPublicKeyId;   // 微信支付公钥 ID (公钥模式)
    private String wechatPayPublicKeyPath; // 微信支付公钥 pem 路径
    private String notifyUrl;              // https://域名/sell/pay/mini/notify

    public boolean isLoginConfigured() {
        return notBlank(appId) && notBlank(appSecret);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
