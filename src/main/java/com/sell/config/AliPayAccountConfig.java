package com.sell.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AliPayAccountConfig {

    /** 支付宝appId. */
    private String appId;

    /** 商户私钥. */
    private String privateKey;

    /** 支付宝公钥. */
    private String aliPayPublicKey;

    /** 支付宝异步通知地址. */
    private String notifyUrl;

    /** 支付宝同步跳转地址. */
    private String returnUrl;

    /**
     * 沙箱环境开关. true = 用 openapi.alipaydev.com, 不花真钱.
     * 沙箱页面虽然展示 openapi-sandbox.dl.alipaydev.com (新), 但旧地址仍可用并自动跳转.
     */
    private boolean sandbox = false;
}
