package com.sell.config;

import com.lly835.bestpay.config.WxPayConfig;
import com.lly835.bestpay.service.impl.BestPayServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * 公众号 best-pay 微信支付 Bean (历史/存量 + 历史单退款). 支付宝已下线.
 * 小程序新支付走 APIv3 (WechatPayApiV3Config), 与此并存.
 */
@Component
public class WechatPayConfig {

    @Autowired
    private WechatAccountConfig accountConfig;

    @Bean
    public BestPayServiceImpl bestPayService() {
        BestPayServiceImpl bestPayService = new BestPayServiceImpl();
        bestPayService.setWxPayConfig(wxPayConfig());
        return bestPayService;
    }

    @Bean
    public WxPayConfig wxPayConfig() {
        WxPayConfig wxPayConfig = new WxPayConfig();
        wxPayConfig.setAppId(accountConfig.getMpAppId());
        wxPayConfig.setAppSecret(accountConfig.getMpAppSecret());
        wxPayConfig.setMchId(accountConfig.getMchId());
        wxPayConfig.setMchKey(accountConfig.getMchKey());
        wxPayConfig.setKeyPath(accountConfig.getKeyPath());
        wxPayConfig.setNotifyUrl(accountConfig.getNotifyUrl());
        return wxPayConfig;
    }
}
