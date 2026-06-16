package com.sell.config;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.refund.RefundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信支付 APIv3 (wechatpay-java 0.2.17, 公钥模式) Bean 装配.
 * 仅当 wechat.mini.enabled=true 时生效——本地/CI 无凭据时整个 @Configuration 不加载,
 * 应用照常启动; 支付相关服务以 @Autowired(required=false) 注入, 未装配时为 null 由调用方兜底报错.
 */
@Configuration
@ConditionalOnProperty(name = "wechat.mini.enabled", havingValue = "true")
public class WechatPayApiV3Config {

    @Autowired
    private WechatMiniConfig cfg;

    @Bean
    public Config wechatPayConfig() {
        return new RSAPublicKeyConfig.Builder()
                .merchantId(cfg.getMchId())
                .privateKeyFromPath(cfg.getPrivateKeyPath())
                .publicKeyFromPath(cfg.getWechatPayPublicKeyPath())
                .publicKeyId(cfg.getWechatPayPublicKeyId())
                .merchantSerialNumber(cfg.getMerchantSerialNo())
                .apiV3Key(cfg.getApiV3Key())
                .build();
    }

    @Bean
    public JsapiServiceExtension jsapiServiceExtension(Config wechatPayConfig) {
        return new JsapiServiceExtension.Builder().config(wechatPayConfig).signType("RSA").build();
    }

    @Bean
    public RefundService refundService(Config wechatPayConfig) {
        return new RefundService.Builder().config(wechatPayConfig).build();
    }

    @Bean
    public NotificationParser notificationParser(Config wechatPayConfig) {
        return new NotificationParser((NotificationConfig) wechatPayConfig);
    }
}
