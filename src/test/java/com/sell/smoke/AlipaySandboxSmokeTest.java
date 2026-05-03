package com.sell.smoke;

import com.lly835.bestpay.config.AliPayConfig;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.lly835.bestpay.model.PayRequest;
import com.lly835.bestpay.model.PayResponse;
import com.lly835.bestpay.service.impl.BestPayServiceImpl;
import org.junit.Assume;
import org.junit.Test;

import java.util.UUID;

/**
 * 直连支付宝沙箱网关的烟测, 验证签名/网络/响应解析整条链路.
 * 不在默认 mvn test 跑, 用:
 *   source .env.alipay && mvn test -Dtest=AlipaySandboxSmokeTest
 */
public class AlipaySandboxSmokeTest {

    @Test
    public void buildSandboxPayUrl() {
        String appId = System.getenv("ALIPAY_APP_ID");
        String privateKey = System.getenv("ALIPAY_PRIVATE_KEY");
        String publicKey = System.getenv("ALIPAY_PUBLIC_KEY");
        // 缺凭证时跳过, 不让 mvn test 失败 (这是手工冒烟测试, 需要 source .env.alipay)
        Assume.assumeTrue(
                "缺 ALIPAY_APP_ID/PRIVATE_KEY/PUBLIC_KEY, 跳过. 手工跑: source .env.alipay && mvn test -Dtest=AlipaySandboxSmokeTest",
                appId != null && privateKey != null && publicKey != null);

        AliPayConfig cfg = new AliPayConfig();
        cfg.setAppId(appId);
        cfg.setPrivateKey(privateKey);
        cfg.setAliPayPublicKey(publicKey);
        cfg.setNotifyUrl(env("ALIPAY_NOTIFY_URL", "http://localhost:8080/sell/pay/alipay/notify"));
        cfg.setReturnUrl(env("ALIPAY_RETURN_URL", "http://localhost:5173/order/status"));
        cfg.setSandbox(true);

        BestPayServiceImpl svc = new BestPayServiceImpl();
        svc.setAliPayConfig(cfg);

        PayRequest req = new PayRequest();
        req.setPayTypeEnum(BestPayTypeEnum.ALIPAY_WAP);
        req.setOrderId("SMOKE_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        req.setOrderName("沙箱烟测订单");
        req.setOrderAmount(0.01d);

        PayResponse resp = svc.pay(req);
        String body = patchSandboxUrl(resp.getBody());

        try {
            String html = "<!doctype html><meta charset=utf-8><title>Alipay sandbox</title>" + body;
            java.nio.file.Path out = java.nio.file.Paths.get("target/alipay_pay.html");
            java.nio.file.Files.createDirectories(out.getParent());
            java.nio.file.Files.writeString(out, html);
            System.out.println("WROTE: " + out.toAbsolutePath());
        } catch (Exception e) {
            System.out.println("write fail: " + e);
        }

        System.out.println();
        System.out.println("========= ALIPAY SANDBOX PAY URL =========");
        System.out.println("orderId : " + req.getOrderId());
        System.out.println("amount  : ¥" + req.getOrderAmount());
        System.out.println("body    :");
        System.out.println(body);
        System.out.println("==========================================");
    }

    private static String patchSandboxUrl(String s) {
        if (s == null) return null;
        return s
                .replace("openapi.alipaydev.com//", "openapi-sandbox.dl.alipaydev.com/")
                .replace("openapi.alipaydev.com/", "openapi-sandbox.dl.alipaydev.com/")
                .replace("openapi.alipaydev.com", "openapi-sandbox.dl.alipaydev.com");
    }

    private static String env(String k, String fallback) {
        String v = System.getenv(k);
        return v == null || v.isEmpty() ? fallback : v;
    }
}
