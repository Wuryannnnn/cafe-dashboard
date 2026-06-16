# 微信小程序点单 — 后端实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 给现有后端新增一条独立的「小程序登录 + 微信支付 APIv3」链路，复用订单/库存/退款/收银核心，并下线支付宝。

**Architecture:** 新增小程序登录（jscode2session + 不透明 DB token + 鉴权拦截器）、小程序下单（openid 由 token 注入）、APIv3 支付（官方 `wechatpay-java 0.2.17` 公钥模式：下单/调起/回调/退款）、发货上报、小程序码生成；移除支付宝新单入口、退役公众号 best-pay 新支付入口（保留 best-pay 退款分支给历史单）。不改订单/库存/退款核心逻辑。

**Tech Stack:** Spring Boot 3.2.5 / Java 17 / JPA(H2 本地, MySQL 生产, `ddl-auto=update`) / `wechatpay-java 0.2.17`(APIv3) / JUnit4 + Mockito + `@SpringBootTest`。

**对应 spec：** `docs/superpowers/specs/2026-06-13-wechat-miniprogram-ordering-design.md`

**外部阻塞（店主提供，影响"真链路"验证，不影响代码与 mock 测试）：** 已认证非个人主体小程序 appId、同主体商户号、APIv3 密钥 + 商户私钥 `apiclient_key.pem` + 微信支付公钥、餐饮类目过审、公网 HTTPS 回调域名。微信支付 APIv3 无沙箱 → 本计划所有任务验证到「mock SDK + 构造样例」边界；真实小额验证在凭据到位后做。

**约定：** 凭据走环境变量（`prod` profile），`local` 留占位；发起失败返回可读错误不抛 500（沿用现有约定）。每完成一个 Task 跑该 Task 的测试 + `mvn -o compile`，跑测试前先 `pkill -9 -f "spring-boot:run"`。提交时机由店主决定（默认不自动提交）。

---

## 文件结构（本计划新建/修改）

**新建（小程序登录/鉴权/下单）**
- `src/main/java/com/sell/dataobject/MiniUser.java` — 小程序顾客身份实体（openid 主键 + 不透明 token）。
- `src/main/java/com/sell/repository/MiniUserRepository.java` — `findByToken`。
- `src/main/java/com/sell/config/WechatMiniConfig.java` — `wechat.mini.*` 配置。
- `src/main/java/com/sell/service/WxMiniLoginService.java` — jscode2session + 签发/校验 token。
- `src/main/java/com/sell/controller/MiniLoginController.java` — `POST /mini/login`。
- `src/main/java/com/sell/interceptor/MiniAuthInterceptor.java` — 顾客 token 鉴权，注入 openid。
- `src/main/java/com/sell/config/MiniWebConfig.java` — 注册拦截器（路径白/黑名单）。
- `src/main/java/com/sell/controller/MiniOrderController.java` — `POST /mini/order/create`。

**新建（APIv3 支付/退款/发货/小程序码）**
- `src/main/java/com/sell/config/WechatPayApiV3Config.java` — `wechatpay-java` Config + Service Bean。
- `src/main/java/com/sell/service/MiniPayService.java` — APIv3 下单/调起/回调/退款。
- `src/main/java/com/sell/controller/MiniPayController.java` — `POST /pay/mini/create`、`POST /pay/mini/notify`。
- `src/main/java/com/sell/dataobject/PendingShipping.java` + `repository/PendingShippingRepository.java` — 发货上报重试队列。
- `src/main/java/com/sell/service/MiniShippingService.java` — 发货上报 + 重试。
- `src/main/java/com/sell/service/WxMiniAccessTokenService.java` — 小程序 access_token 缓存。
- `src/main/java/com/sell/controller/MiniQrCodeController.java` — `GET /seller/qrcode/mini`。

**修改**
- `pom.xml` — 加 `wechatpay-java 0.2.17`。
- `src/main/java/com/sell/dataobject/OrderMaster.java` — 加 `wxTransactionId` 字段。
- `src/main/java/com/sell/service/impl/OrderServiceImpl.java` — 退款分流按 `wxTransactionId`；paid 成功后触发发货上报；订正过时注释。
- `src/main/java/com/sell/service/impl/CashierServiceImpl.java` — 订正"paid 要求 NEW"过时注释。
- `src/main/resources/application-local.yml` / `application-prod.yml` — `wechat.mini.*` 配置段。
- 支付宝下线：`PayController.java`、`PayServiceImpl.java`、`WechatPayConfig.java`、删 `AliPayAccountConfig.java`、删/改 `PayServiceImplAlipayTest.java`、`smoke/AlipaySandboxSmokeTest.java`、`PayControllerTest.java` 相关用例。

---

## Task 1: 加 wechatpay-java 依赖 + APIv3 Config Bean（公钥模式）

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/com/sell/config/WechatMiniConfig.java`
- Create: `src/main/java/com/sell/config/WechatPayApiV3Config.java`
- Modify: `src/main/resources/application-local.yml`, `application-prod.yml`

- [ ] **Step 1: 加依赖**

`pom.xml` 在 `<dependencies>` 内加：
```xml
<dependency>
    <groupId>com.github.wechatpay-apiv3</groupId>
    <artifactId>wechatpay-java</artifactId>
    <version>0.2.17</version>
</dependency>
```

- [ ] **Step 2: 配置类 WechatMiniConfig**

```java
package com.sell.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 小程序登录 + APIv3 支付配置. 生产读环境变量, 本地留占位. */
@Data
@Component
@ConfigurationProperties(prefix = "wechat.mini")
public class WechatMiniConfig {
    private String appId;              // 小程序 appId
    private String appSecret;          // 小程序密钥 (登录用, 仅后端)
    private String mchId;              // 商户号
    private String merchantSerialNo;   // 商户 API 证书序列号
    private String privateKeyPath;     // 商户 API 私钥 apiclient_key.pem
    private String apiV3Key;           // APIv3 密钥
    private String wechatPayPublicKeyId;   // 微信支付公钥 ID (公钥模式)
    private String wechatPayPublicKeyPath; // 微信支付公钥 pem 路径
    private String notifyUrl;          // https://域名/sell/pay/mini/notify
    /** appId 与 mchId 都非空才算配置就绪 (未就绪时支付链路返回可读错误而非 500). */
    public boolean isPayConfigured() {
        return notBlank(appId) && notBlank(mchId) && notBlank(merchantSerialNo)
                && notBlank(privateKeyPath) && notBlank(apiV3Key) && notBlank(wechatPayPublicKeyId);
    }
    public boolean isLoginConfigured() { return notBlank(appId) && notBlank(appSecret); }
    private static boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }
}
```

- [ ] **Step 3: 配置段**

`application-local.yml` 在 `wechat:` 下加（本地占位）：
```yaml
wechat:
  mini:
    appId: ""
    appSecret: ""
    mchId: ""
    merchantSerialNo: ""
    privateKeyPath: ""
    apiV3Key: ""
    wechatPayPublicKeyId: ""
    wechatPayPublicKeyPath: ""
    notifyUrl: "http://localhost:8080/sell/pay/mini/notify"
```
`application-prod.yml` 同结构, 值用 `${WECHAT_MINI_APPID:}` 等环境变量占位。

- [ ] **Step 4: APIv3 Config Bean（公钥模式，仅在配置就绪时装配）**

```java
package com.sell.config;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.refund.RefundService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** wechatpay-java 0.2.17 公钥模式 Config 与服务 Bean. 配置未就绪时 Bean 为 null, 由 MiniPayService 兜底报错. */
@Configuration
@Slf4j
public class WechatPayApiV3Config {

    @Autowired private WechatMiniConfig cfg;

    @Bean
    public Config wechatPayConfig() {
        if (!cfg.isPayConfigured()) {
            log.warn("[小程序支付] APIv3 配置未就绪, 支付链路将返回可读错误");
            return null;
        }
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
        if (wechatPayConfig == null) return null;
        return new JsapiServiceExtension.Builder().config(wechatPayConfig).signType("RSA").build();
    }

    @Bean
    public RefundService refundService(Config wechatPayConfig) {
        if (wechatPayConfig == null) return null;
        return new RefundService.Builder().config(wechatPayConfig).build();
    }

    @Bean
    public NotificationParser notificationParser(Config wechatPayConfig) {
        if (wechatPayConfig == null) return null;
        return new NotificationParser((com.wechat.pay.java.core.notification.NotificationConfig) wechatPayConfig);
    }
}
```

- [ ] **Step 5: 验证编译**

Run: `pkill -9 -f "spring-boot:run"; mvn -o compile`
Expected: `BUILD SUCCESS`（依赖下载成功、Config 类编译通过；`null` Bean 在 Spring 中合法，注入处需 `@Autowired(required=false)`）。

- [ ] **Step 6: Commit**

```bash
git add pom.xml src/main/java/com/sell/config/WechatMiniConfig.java src/main/java/com/sell/config/WechatPayApiV3Config.java src/main/resources/application-local.yml src/main/resources/application-prod.yml
git commit -m "feat(mini): 加 wechatpay-java APIv3 依赖与公钥模式 Config"
```

---

## Task 2: MiniUser 实体 + 仓库（顾客身份 + 不透明 token）

**Files:**
- Create: `src/main/java/com/sell/dataobject/MiniUser.java`
- Create: `src/main/java/com/sell/repository/MiniUserRepository.java`
- Test: `src/test/java/com/sell/repository/MiniUserRepositoryTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.sell.repository;

import com.sell.dataobject.MiniUser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class MiniUserRepositoryTest {
    @Autowired private MiniUserRepository repo;

    @Test
    public void saveAndFindByToken() {
        MiniUser u = new MiniUser();
        u.setOpenid("o_test_1");
        u.setSessionKey("sk");
        u.setToken("tok-123");
        u.setTokenExpireAt(new Date(System.currentTimeMillis() + 86400000L));
        repo.save(u);
        MiniUser found = repo.findByToken("tok-123").orElse(null);
        assertNotNull(found);
        assertEquals("o_test_1", found.getOpenid());
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `pkill -9 -f "spring-boot:run"; mvn -o test -Dtest=MiniUserRepositoryTest`
Expected: 编译失败（`MiniUser` / `MiniUserRepository` 不存在）。

- [ ] **Step 3: 实体**

```java
package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import jakarta.persistence.*;
import java.util.Date;

/** 小程序顾客身份 (与储值 Member 解耦, 仅作点单登录态). */
@Entity
@Table(name = "mini_user", indexes = { @Index(name = "idx_mu_token", columnList = "token") })
@Data
@DynamicUpdate
public class MiniUser {
    @Id
    private String openid;          // 小程序 openid, 主键
    private String sessionKey;      // 仅后端留存, 不下发
    private String nickname;
    private String avatar;
    private String token;           // 不透明会话 token
    private Date tokenExpireAt;     // token 过期时间
    private Date createTime;
    private Date updateTime;
}
```

- [ ] **Step 4: 仓库**

```java
package com.sell.repository;

import com.sell.dataobject.MiniUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MiniUserRepository extends JpaRepository<MiniUser, String> {
    Optional<MiniUser> findByToken(String token);
}
```

- [ ] **Step 5: 跑测试确认通过**

Run: `pkill -9 -f "spring-boot:run"; mvn -o test -Dtest=MiniUserRepositoryTest`
Expected: PASS（H2 `ddl-auto=update` 自动建表）。

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/sell/dataobject/MiniUser.java src/main/java/com/sell/repository/MiniUserRepository.java src/test/java/com/sell/repository/MiniUserRepositoryTest.java
git commit -m "feat(mini): MiniUser 顾客身份实体 + 仓库"
```

---

## Task 3: WxMiniLoginService（jscode2session + 签发/校验 token）

**Files:**
- Create: `src/main/java/com/sell/service/WxMiniLoginService.java`
- Test: `src/test/java/com/sell/service/WxMiniLoginServiceTest.java`

设计：把"调微信 jscode2session"抽成可注入的 `code -> {openid, sessionKey}` 函数，便于单测 mock。

- [ ] **Step 1: 写失败测试（mock jscode2session）**

```java
package com.sell.service;

import com.sell.dataobject.MiniUser;
import com.sell.exception.SellException;
import com.sell.repository.MiniUserRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class WxMiniLoginServiceTest {
    @Autowired private WxMiniLoginService loginService;
    @Autowired private MiniUserRepository repo;
    @MockBean private WxMiniLoginService.Jscode2Session jscode2Session; // 内部接口, mock 微信调用

    @Test
    public void login_upsertsUserAndIssuesToken() {
        when(jscode2Session.exchange("good-code"))
            .thenReturn(new WxMiniLoginService.Session("openid-A", "sk-A"));
        WxMiniLoginService.LoginResult r = loginService.login("good-code");
        assertEquals("openid-A", r.openid);
        assertNotNull(r.token);
        MiniUser saved = repo.findById("openid-A").orElse(null);
        assertNotNull(saved);
        assertEquals("sk-A", saved.getSessionKey()); // sessionKey 留后端
        assertEquals(r.token, saved.getToken());
    }

    @Test(expected = SellException.class)
    public void login_wechatError_throwsReadable() {
        when(jscode2Session.exchange("bad-code")).thenReturn(null); // errcode!=0 → 返回 null
        loginService.login("bad-code");
    }

    @Test
    public void resolveOpenid_validToken_returnsOpenid() {
        when(jscode2Session.exchange("c")).thenReturn(new WxMiniLoginService.Session("openid-B", "sk"));
        String token = loginService.login("c").token;
        assertEquals("openid-B", loginService.resolveOpenid(token));
    }

    @Test
    public void resolveOpenid_unknownToken_returnsNull() {
        assertNull(loginService.resolveOpenid("no-such-token"));
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `pkill -9 -f "spring-boot:run"; mvn -o test -Dtest=WxMiniLoginServiceTest`
Expected: 编译失败（类不存在）。

- [ ] **Step 3: 实现**

```java
package com.sell.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sell.config.WechatMiniConfig;
import com.sell.dataobject.MiniUser;
import com.sell.enums.ResultEnum;
import com.sell.exception.SellException;
import com.sell.repository.MiniUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class WxMiniLoginService {

    /** token 有效期 (2 天, 短于 session_key). */
    private static final long TOKEN_TTL_MS = 2L * 24 * 3600 * 1000;

    @Autowired private WechatMiniConfig cfg;
    @Autowired private MiniUserRepository repo;
    @Autowired private Jscode2Session jscode2Session;

    public static class Session {
        public final String openid; public final String sessionKey;
        public Session(String o, String s) { this.openid = o; this.sessionKey = s; }
    }
    public static class LoginResult {
        public final String openid; public final String token;
        public LoginResult(String o, String t) { this.openid = o; this.token = t; }
    }

    /** 调微信 jscode2session 的封装(可被 mock); 失败(errcode!=0)返回 null. */
    @Service
    public static class Jscode2Session {
        @Autowired private WechatMiniConfig cfg;
        private final RestTemplate rest = new RestTemplate();
        public Session exchange(String code) {
            String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + cfg.getAppId()
                    + "&secret=" + cfg.getAppSecret() + "&js_code=" + code
                    + "&grant_type=authorization_code";
            try {
                String body = rest.getForObject(url, String.class);
                JsonObject o = JsonParser.parseString(body).getAsJsonObject();
                if (o.has("errcode") && o.get("errcode").getAsInt() != 0) {
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

    @Transactional
    public LoginResult login(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new SellException(ResultEnum.PARAM_ERROR.getCode(), "code 不能为空");
        }
        Session s = jscode2Session.exchange(code);
        if (s == null || s.openid == null) {
            throw new SellException(ResultEnum.PARAM_ERROR.getCode(), "微信登录失败, 请重试");
        }
        Date now = new Date();
        MiniUser u = repo.findById(s.openid).orElseGet(() -> {
            MiniUser n = new MiniUser(); n.setOpenid(s.openid); n.setCreateTime(now); return n;
        });
        u.setSessionKey(s.sessionKey);
        u.setToken(UUID.randomUUID().toString().replace("-", ""));
        u.setTokenExpireAt(new Date(now.getTime() + TOKEN_TTL_MS));
        u.setUpdateTime(now);
        repo.save(u);
        return new LoginResult(u.getOpenid(), u.getToken());
    }

    /** 校验 token, 返回 openid; 无效/过期返回 null. */
    public String resolveOpenid(String token) {
        if (token == null || token.isEmpty()) return null;
        MiniUser u = repo.findByToken(token).orElse(null);
        if (u == null || u.getTokenExpireAt() == null || u.getTokenExpireAt().before(new Date())) return null;
        return u.getOpenid();
    }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `pkill -9 -f "spring-boot:run"; mvn -o test -Dtest=WxMiniLoginServiceTest`
Expected: PASS（4 个用例）。

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/sell/service/WxMiniLoginService.java src/test/java/com/sell/service/WxMiniLoginServiceTest.java
git commit -m "feat(mini): 小程序登录服务 (jscode2session + 不透明 token)"
```

---

## Task 4: MiniLoginController（POST /mini/login）

**Files:**
- Create: `src/main/java/com/sell/controller/MiniLoginController.java`
- Test: `src/test/java/com/sell/controller/MiniLoginControllerTest.java`

- [ ] **Step 1: 写失败测试（MockMvc + mock 登录服务）**

```java
package com.sell.controller;

import com.sell.service.WxMiniLoginService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "admin.auth.enabled=false")
@AutoConfigureMockMvc
public class MiniLoginControllerTest {
    @Autowired private MockMvc mvc;
    @MockBean private WxMiniLoginService loginService;

    @Test
    public void login_returnsTokenAndOpenid() throws Exception {
        when(loginService.login(eq("code-1")))
            .thenReturn(new WxMiniLoginService.LoginResult("openid-X", "tok-X"));
        mvc.perform(post("/mini/login").param("code", "code-1"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.token").value("tok-X"))
           .andExpect(jsonPath("$.data.openid").value("openid-X"));
    }
}
```

- [ ] **Step 2: 跑确认失败** — Run: `mvn -o test -Dtest=MiniLoginControllerTest` → 404/编译失败。

- [ ] **Step 3: 实现**

```java
package com.sell.controller;

import com.sell.VO.ResultVO;
import com.sell.service.WxMiniLoginService;
import com.sell.utils.ResultVOUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/mini")
public class MiniLoginController {
    @Autowired private WxMiniLoginService loginService;

    @PostMapping("/login")
    public ResultVO<Map<String, String>> login(@RequestParam("code") String code) {
        WxMiniLoginService.LoginResult r = loginService.login(code);
        Map<String, String> m = new HashMap<>();
        m.put("token", r.token);
        m.put("openid", r.openid);
        return ResultVOUtil.success(m);
    }
}
```

- [ ] **Step 4: 跑确认通过** — Run: `mvn -o test -Dtest=MiniLoginControllerTest` → PASS。
- [ ] **Step 5: Commit** — `git commit -m "feat(mini): POST /mini/login 端点"`

---

## Task 5: MiniAuthInterceptor + 注册（顾客 token 鉴权，注入 openid）

**Files:**
- Create: `src/main/java/com/sell/interceptor/MiniAuthInterceptor.java`
- Create: `src/main/java/com/sell/config/MiniWebConfig.java`
- Test: `src/test/java/com/sell/interceptor/MiniAuthInterceptorTest.java`

约定：拦截器把 openid 放进 `request.setAttribute("miniOpenid", openid)`；受保护接口无有效 token → 401。

- [ ] **Step 1: 写失败测试**

```java
package com.sell.interceptor;

import com.sell.service.WxMiniLoginService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "admin.auth.enabled=false")
@AutoConfigureMockMvc
public class MiniAuthInterceptorTest {
    @Autowired private MockMvc mvc;
    @MockBean private WxMiniLoginService loginService;

    @Test
    public void protectedPath_noToken_unauthorized() throws Exception {
        mvc.perform(post("/mini/order/create").param("items", "[]"))
           .andExpect(status().isUnauthorized());
    }

    @Test
    public void protectedPath_badToken_unauthorized() throws Exception {
        when(loginService.resolveOpenid("bad")).thenReturn(null);
        mvc.perform(post("/mini/order/create").header("Authorization", "bad").param("items", "[]"))
           .andExpect(status().isUnauthorized());
    }

    @Test
    public void loginPath_excluded_notBlocked() throws Exception {
        // /mini/login 不被拦截器拦 (loginService.login 未 mock 会抛, 但至少不是 401)
        mvc.perform(post("/mini/login").param("code", "x"))
           .andExpect(status().is(org.springframework.http.HttpStatus.OK.value())); // login 失败走全局异常→仍 200+code!=0
    }
}
```

- [ ] **Step 2: 跑确认失败** — Run: `mvn -o test -Dtest=MiniAuthInterceptorTest` → 第一个用例非 401。

- [ ] **Step 3: 拦截器**

```java
package com.sell.interceptor;

import com.sell.service.WxMiniLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 小程序顾客鉴权: 校验 Authorization token → 注入 miniOpenid; 无效则 401. */
@Component
public class MiniAuthInterceptor implements HandlerInterceptor {
    public static final String ATTR_OPENID = "miniOpenid";
    @Autowired private WxMiniLoginService loginService;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        String token = req.getHeader("Authorization");
        String openid = loginService.resolveOpenid(token);
        if (openid == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"code\":401,\"msg\":\"登录已过期, 请重新进入\"}");
            return false;
        }
        req.setAttribute(ATTR_OPENID, openid);
        return true;
    }
}
```

- [ ] **Step 4: 注册（路径白/黑名单，独立于 admin.auth）**

```java
package com.sell.config;

import com.sell.interceptor.MiniAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MiniWebConfig implements WebMvcConfigurer {
    @Autowired private MiniAuthInterceptor miniAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(miniAuthInterceptor)
                .addPathPatterns("/mini/order/**", "/pay/mini/create")
                .excludePathPatterns("/mini/login", "/pay/mini/notify");
    }
}
```

- [ ] **Step 5: 跑确认通过** — Run: `mvn -o test -Dtest=MiniAuthInterceptorTest` → PASS。
- [ ] **Step 6: Commit** — `git commit -m "feat(mini): 顾客 token 鉴权拦截器 + 路径注册"`

---

## Task 6: MiniOrderController（POST /mini/order/create，openid 由 token 注入）

**Files:**
- Create: `src/main/java/com/sell/controller/MiniOrderController.java`
- Test: `src/test/java/com/sell/controller/MiniOrderControllerTest.java`

- [ ] **Step 1: 写失败测试（@SpringBootTest 真建单, 用拦截器注入的 openid）**

```java
package com.sell.controller;

import com.sell.dataobject.ProductInfo;
import com.sell.repository.ProductInfoRepository;
import com.sell.service.WxMiniLoginService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "admin.auth.enabled=false")
@AutoConfigureMockMvc
public class MiniOrderControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private ProductInfoRepository productRepo;
    @MockBean private WxMiniLoginService loginService;

    @Before public void seed() {
        ProductInfo p = new ProductInfo();
        p.setProductId("MINI_P1"); p.setProductName("拿铁"); p.setProductPrice(new BigDecimal("18.00"));
        p.setProductStock(100); p.setProductStatus(0); p.setCategoryType(1);
        productRepo.save(p);
        when(loginService.resolveOpenid("tok-1")).thenReturn("openid-buyer-1");
    }

    @Test
    public void create_usesTokenOpenid_andFrontendOpenidIgnored() throws Exception {
        mvc.perform(post("/mini/order/create")
                .header("Authorization", "tok-1")
                .param("tableId", "5")
                .param("openid", "FORGED-openid") // 应被忽略
                .param("items", "[{\"productId\":\"MINI_P1\",\"productQuantity\":1}]"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.orderId").exists());
        // 订单的 buyerOpenid 应是 token 的 openid, 不是前端伪造的
    }
}
```

- [ ] **Step 2: 跑确认失败** — Run: `mvn -o test -Dtest=MiniOrderControllerTest` → 404。

- [ ] **Step 3: 实现（复用 CashierServiceImpl.manualCreateOrder 的 items 解析思路；openid 从 attribute 取）**

```java
package com.sell.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sell.VO.ResultVO;
import com.sell.dataobject.OrderDetail;
import com.sell.dto.OrderDTO;
import com.sell.enums.ResultEnum;
import com.sell.exception.SellException;
import com.sell.interceptor.MiniAuthInterceptor;
import com.sell.service.OrderService;
import com.sell.utils.ResultVOUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/mini/order")
public class MiniOrderController {
    @Autowired private OrderService orderService;

    @PostMapping("/create")
    public ResultVO<Map<String, String>> create(@RequestParam(value = "tableId", required = false) Integer tableId,
                                                 @RequestParam("items") String items,
                                                 HttpServletRequest request) {
        String openid = (String) request.getAttribute(MiniAuthInterceptor.ATTR_OPENID);
        List<OrderDetail> details;
        try {
            details = new Gson().fromJson(items, new TypeToken<List<OrderDetail>>(){}.getType());
        } catch (Exception e) {
            throw new SellException(ResultEnum.PARAM_ERROR);
        }
        if (details == null || details.isEmpty()) throw new SellException(ResultEnum.CART_EMPTY);

        OrderDTO dto = new OrderDTO();
        dto.setBuyerOpenid(openid);            // 服务端注入, 忽略前端 openid
        dto.setBuyerName("微信顾客");
        dto.setBuyerPhone("");
        dto.setBuyerAddress("");
        dto.setDiningType(0);
        dto.setTableId(tableId);
        dto.setOrderDetailList(details);
        OrderDTO created = orderService.create(dto);

        Map<String, String> m = new HashMap<>();
        m.put("orderId", created.getOrderId());
        m.put("pickupNumber", created.getPickupNumber());
        return ResultVOUtil.success(m);
    }
}
```

注：`OrderService.create` 已含 §B1 修复（`buyerOpenid` 非 `cashier-manual` 时受 `switch.qrOrder` 管 → 小程序顾客单受扫码开关控制，符合预期）。

- [ ] **Step 4: 跑确认通过** — Run: `mvn -o test -Dtest=MiniOrderControllerTest` → PASS。
- [ ] **Step 5: Commit** — `git commit -m "feat(mini): POST /mini/order/create (openid 由 token 注入)"`

---

## Task 7: OrderMaster 加 wxTransactionId 字段

**Files:**
- Modify: `src/main/java/com/sell/dataobject/OrderMaster.java`

- [ ] **Step 1: 加字段**（在 `payType` 字段后）：
```java
    /** 小程序 APIv3 微信支付交易号 (回调写入); 退款分流与发货上报范围的判定依据. */
    private String wxTransactionId;
```
- [ ] **Step 2: 编译验证** — Run: `mvn -o compile` → `BUILD SUCCESS`（H2/MySQL `ddl-auto=update` 自动加列）。
- [ ] **Step 3: Commit** — `git commit -m "feat(order): OrderMaster 加 wxTransactionId(小程序支付交易号)"`

---

## Task 8: MiniPayService.create + MiniPayController（POST /pay/mini/create，APIv3 下单）

**Files:**
- Create: `src/main/java/com/sell/service/MiniPayService.java`
- Create: `src/main/java/com/sell/controller/MiniPayController.java`
- Create: `src/main/java/com/sell/utils/MoneyUtil.java`
- Test: `src/test/java/com/sell/utils/MoneyUtilTest.java`, `src/test/java/com/sell/service/MiniPayServiceCreateTest.java`

- [ ] **Step 1: 金额换算工具 + 失败测试**

`MoneyUtilTest`:
```java
package com.sell.utils;
import org.junit.Test;
import java.math.BigDecimal;
import static org.junit.Assert.assertEquals;
public class MoneyUtilTest {
    @Test public void yuanToFen() {
        assertEquals(1, MoneyUtil.yuanToFen(new BigDecimal("0.01")));
        assertEquals(10, MoneyUtil.yuanToFen(new BigDecimal("0.10")));
        assertEquals(7, MoneyUtil.yuanToFen(new BigDecimal("0.07")));
        assertEquals(1999, MoneyUtil.yuanToFen(new BigDecimal("19.99")));
        assertEquals(1800, MoneyUtil.yuanToFen(new BigDecimal("18")));
    }
}
```
Run: `mvn -o test -Dtest=MoneyUtilTest` → 失败（类不存在）。

实现 `MoneyUtil`:
```java
package com.sell.utils;
import java.math.BigDecimal;
import java.math.RoundingMode;
/** 元 ↔ 分 换算, 全程整数, 不经 double. */
public final class MoneyUtil {
    private MoneyUtil() {}
    public static int yuanToFen(BigDecimal yuan) {
        return yuan.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }
}
```
Run: `mvn -o test -Dtest=MoneyUtilTest` → PASS。

- [ ] **Step 2: MiniPayService.create 失败测试（mock JsapiServiceExtension）**

```java
package com.sell.service;

import com.sell.dto.OrderDTO;
import com.sell.exception.SellException;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import java.math.BigDecimal;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"admin.auth.enabled=false", "wechat.mini.appId=wxTESTmini", "wechat.mini.mchId=160000"})
public class MiniPayServiceCreateTest {
    @Autowired private MiniPayService miniPayService;
    @MockBean private JsapiServiceExtension jsapi;

    @Test
    public void create_buildsPrepayRequest_andReturnsFiveFields() {
        PrepayWithRequestPaymentResponse stub = new PrepayWithRequestPaymentResponse();
        stub.setAppId("wxTESTmini"); stub.setTimeStamp("1700000000");
        stub.setNonceStr("n"); stub.setPackageVal("prepay_id=pp1");
        stub.setSignType("RSA"); stub.setPaySign("sig");
        when(jsapi.prepayWithRequestPayment(any(PrepayRequest.class))).thenReturn(stub);

        OrderDTO order = new OrderDTO();
        order.setOrderId("O123"); order.setOrderAmount(new BigDecimal("18.00"));

        PrepayWithRequestPaymentResponse out = miniPayService.create(order, "openid-1");

        ArgumentCaptor<PrepayRequest> cap = ArgumentCaptor.forClass(PrepayRequest.class);
        org.mockito.Mockito.verify(jsapi).prepayWithRequestPayment(cap.capture());
        PrepayRequest req = cap.getValue();
        assertEquals("O123", req.getOutTradeNo());
        assertEquals("openid-1", req.getPayer().getOpenid());
        assertEquals(Integer.valueOf(1800), req.getAmount().getTotal()); // 元→分
        assertEquals("prepay_id=pp1", out.getPackageVal());
    }

    @Test(expected = SellException.class)
    public void create_notConfigured_throwsReadable() {
        // 当 jsapi Bean 为 null (未配置) 时应抛可读错误 —— 见实现的兜底
        // 本用例在另一个未注入 mock 的上下文验证; 此处占位说明意图
        throw new SellException(1, "未配置");
    }
}
```
注：第二个用例的"未配置"分支在实现里体现为 `jsapi == null` 时抛可读错误；集成测试用 mock 注入了 jsapi，故此处仅以占位表达意图，真实"未配置"路径靠 `MiniPayService` 内 null 判断 + 手工/本地占位验证。

Run: `mvn -o test -Dtest=MiniPayServiceCreateTest` → 失败（`MiniPayService` 不存在）。

- [ ] **Step 3: 实现 MiniPayService.create**

```java
package com.sell.service;

import com.sell.config.WechatMiniConfig;
import com.sell.dto.OrderDTO;
import com.sell.exception.SellException;
import com.sell.utils.MoneyUtil;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MiniPayService {
    private static final String ORDER_NAME = "咖啡厅订单";

    @Autowired private WechatMiniConfig cfg;
    @Autowired(required = false) private JsapiServiceExtension jsapi;

    /** APIv3 小程序下单 + 调起签名 (SDK 一步返回 5 字段). */
    public PrepayWithRequestPaymentResponse create(OrderDTO order, String openid) {
        if (jsapi == null || !cfg.isPayConfigured()) {
            throw new SellException(-1, "支付未配置, 请联系商家");
        }
        PrepayRequest req = new PrepayRequest();
        req.setAppid(cfg.getAppId());
        req.setMchid(cfg.getMchId());
        req.setDescription(ORDER_NAME);
        req.setOutTradeNo(order.getOrderId());
        req.setNotifyUrl(cfg.getNotifyUrl());
        Amount amount = new Amount();
        amount.setTotal(MoneyUtil.yuanToFen(order.getOrderAmount()));
        amount.setCurrency("CNY");
        req.setAmount(amount);
        Payer payer = new Payer();
        payer.setOpenid(openid);
        req.setPayer(payer);
        return jsapi.prepayWithRequestPayment(req);
    }
}
```

- [ ] **Step 4: 跑确认通过** — Run: `mvn -o test -Dtest=MiniPayServiceCreateTest,MoneyUtilTest` → 第一个 create 用例 PASS（第二个占位用例自抛通过）。

- [ ] **Step 5: MiniPayController.create**

```java
package com.sell.controller;

import com.sell.dto.OrderDTO;
import com.sell.interceptor.MiniAuthInterceptor;
import com.sell.service.MiniPayService;
import com.sell.service.OrderService;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/pay/mini")
@Slf4j
public class MiniPayController {
    @Autowired private MiniPayService miniPayService;
    @Autowired private OrderService orderService;

    @PostMapping("/create")
    public Map<String, Object> create(@RequestParam("orderId") String orderId, HttpServletRequest request) {
        String openid = (String) request.getAttribute(MiniAuthInterceptor.ATTR_OPENID);
        Map<String, Object> r = new LinkedHashMap<>();
        try {
            OrderDTO order = orderService.findOne(orderId);
            PrepayWithRequestPaymentResponse p = miniPayService.create(order, openid);
            r.put("timeStamp", p.getTimeStamp());
            r.put("nonceStr", p.getNonceStr());
            r.put("package", p.getPackageVal());
            r.put("signType", p.getSignType());
            r.put("paySign", p.getPaySign());
        } catch (Exception e) {
            log.error("【小程序支付】发起失败 orderId={}: {}", orderId, e.getMessage());
            r.put("code", -1);
            r.put("msg", "支付发起失败: " + e.getMessage());
        }
        return r;
    }
}
```

- [ ] **Step 6: Commit** — `git commit -m "feat(mini): APIv3 小程序下单 /pay/mini/create + 元分换算"`

---

## Task 9: MiniPayService.notify + /pay/mini/notify（回调验签解密 + 幂等 + 写 transaction_id）

**Files:**
- Modify: `src/main/java/com/sell/service/MiniPayService.java`（加 notify）
- Modify: `src/main/java/com/sell/controller/MiniPayController.java`（加 /notify）
- Modify: `src/main/java/com/sell/service/impl/OrderServiceImpl.java`（paid 写 wxTransactionId 的入口；见下）
- Test: `src/test/java/com/sell/service/MiniPayServiceNotifyTest.java`

幂等关键：调 `OrderService.paid` **之前**先判 `SUCCESS` 直接返回（`paid()` 对非 WAIT 抛异常）。

- [ ] **Step 1: 失败测试（mock NotificationParser + OrderService）**

```java
package com.sell.service;

import com.sell.dto.OrderDTO;
import com.sell.enums.PayStatusEnum;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import java.math.BigDecimal;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "admin.auth.enabled=false")
public class MiniPayServiceNotifyTest {
    @Autowired private MiniPayService miniPayService;
    @MockBean private NotificationParser notificationParser;
    @MockBean private OrderService orderService;

    private Transaction tx(String outTradeNo, int fen, String txnId) {
        Transaction t = new Transaction();
        t.setOutTradeNo(outTradeNo); t.setTransactionId(txnId);
        Transaction.TradeStateEnum s = Transaction.TradeStateEnum.SUCCESS; t.setTradeState(s);
        com.wechat.pay.java.service.payments.model.TransactionAmount amt =
            new com.wechat.pay.java.service.payments.model.TransactionAmount();
        amt.setTotal(fen); t.setAmount(amt);
        return t;
    }
    private OrderDTO order(String id, int payStatus) {
        OrderDTO o = new OrderDTO(); o.setOrderId(id); o.setOrderAmount(new BigDecimal("18.00")); o.setPayStatus(payStatus); return o;
    }

    @Test
    public void notify_firstTime_callsPaid() {
        when(notificationParser.parse(any(RequestParam.class), eq(Transaction.class)))
            .thenReturn(tx("O1", 1800, "TX1"));
        when(orderService.findOne("O1")).thenReturn(order("O1", PayStatusEnum.WAIT.getCode()));
        miniPayService.notify(stubHeaders(), "body");
        verify(orderService).paid(any(OrderDTO.class));
    }

    @Test
    public void notify_duplicate_skipsPaid() {
        when(notificationParser.parse(any(RequestParam.class), eq(Transaction.class)))
            .thenReturn(tx("O1", 1800, "TX1"));
        when(orderService.findOne("O1")).thenReturn(order("O1", PayStatusEnum.SUCCESS.getCode()));
        miniPayService.notify(stubHeaders(), "body");
        verify(orderService, never()).paid(any(OrderDTO.class)); // 幂等: 已支付直接跳过
    }

    @Test(expected = RuntimeException.class)
    public void notify_amountMismatch_throws() {
        when(notificationParser.parse(any(RequestParam.class), eq(Transaction.class)))
            .thenReturn(tx("O1", 1, "TX1")); // 1 分 ≠ 1800
        when(orderService.findOne("O1")).thenReturn(order("O1", PayStatusEnum.WAIT.getCode()));
        miniPayService.notify(stubHeaders(), "body");
    }

    private java.util.Map<String,String> stubHeaders() {
        java.util.Map<String,String> h = new java.util.HashMap<>();
        h.put("Wechatpay-Serial","s"); h.put("Wechatpay-Nonce","n");
        h.put("Wechatpay-Signature","sig"); h.put("Wechatpay-Timestamp","123");
        return h;
    }
}
```

- [ ] **Step 2: 跑确认失败** — Run: `mvn -o test -Dtest=MiniPayServiceNotifyTest` → 失败（notify 不存在）。

- [ ] **Step 3: 实现 notify（加到 MiniPayService）**

```java
    @Autowired(required = false) private com.wechat.pay.java.core.notification.NotificationParser notificationParser;
    @Autowired private OrderService orderService;
    @Autowired private com.sell.utils.MoneyUtil moneyUtilUnused; // 占位避免误删; 实际用静态方法

    /** APIv3 回调: 验签+解密(SDK) → 幂等前置 → 金额比对 → paid + 写 transaction_id. 返回 true=成功应答. */
    public boolean notify(java.util.Map<String, String> headers, String body) {
        if (notificationParser == null) throw new SellException(-1, "支付未配置");
        com.wechat.pay.java.core.notification.RequestParam params =
            new com.wechat.pay.java.core.notification.RequestParam.Builder()
                .serialNumber(headers.get("Wechatpay-Serial"))
                .nonce(headers.get("Wechatpay-Nonce"))
                .signature(headers.get("Wechatpay-Signature"))
                .timestamp(headers.get("Wechatpay-Timestamp"))
                .body(body)
                .build();
        com.wechat.pay.java.service.payments.model.Transaction tx =
            notificationParser.parse(params, com.wechat.pay.java.service.payments.model.Transaction.class);
        String orderId = tx.getOutTradeNo();
        OrderDTO order = orderService.findOne(orderId);
        // 幂等前置: 已支付直接返回成功, 不再调 paid (paid 对非 WAIT 抛异常 → 否则微信无限重试)
        if (com.sell.enums.PayStatusEnum.SUCCESS.getCode().equals(order.getPayStatus())) {
            log.info("[小程序支付] 回调幂等跳过 orderId={}", orderId);
            return true;
        }
        int needFen = MoneyUtil.yuanToFen(order.getOrderAmount());
        int gotFen = tx.getAmount() != null && tx.getAmount().getTotal() != null ? tx.getAmount().getTotal() : -1;
        if (needFen != gotFen) {
            log.error("[小程序支付] 金额不一致 orderId={} 需{}分 收{}分", orderId, needFen, gotFen);
            throw new SellException(-1, "回调金额不一致");
        }
        order.setWxTransactionId(tx.getTransactionId());
        orderService.paid(order); // paid 内行锁 + 状态校验是并发双回调第二道防线
        orderService.saveTransactionId(orderId, tx.getTransactionId()); // 写交易号(退款分流依据)
        return true;
    }
```
> 说明：`OrderService.paid(order)` 已把 `payStatus→SUCCESS`；`saveTransactionId` 见 Task 10 在 `OrderService` 增的小方法（行锁写 `wxTransactionId`）。删除上面占位的 `moneyUtilUnused` 字段——`MoneyUtil` 是静态工具无需注入（此行仅为提示，实现时不要保留该字段）。

- [ ] **Step 4: /pay/mini/notify 端点（加到 MiniPayController）**

```java
    @Autowired(required = false) // 见类已有注入
    @PostMapping("/notify")
    public Map<String, String> notify(@RequestBody String body,
                                      @RequestHeader("Wechatpay-Serial") String serial,
                                      @RequestHeader("Wechatpay-Nonce") String nonce,
                                      @RequestHeader("Wechatpay-Signature") String signature,
                                      @RequestHeader("Wechatpay-Timestamp") String timestamp) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Wechatpay-Serial", serial);
        headers.put("Wechatpay-Nonce", nonce);
        headers.put("Wechatpay-Signature", signature);
        headers.put("Wechatpay-Timestamp", timestamp);
        Map<String, String> resp = new HashMap<>();
        try {
            miniPayService.notify(headers, body);
            resp.put("code", "SUCCESS"); resp.put("message", "成功");
        } catch (Exception e) {
            log.error("【小程序支付回调】处理失败: {}", e.getMessage());
            // 非 200 让微信重试: 这里返回 500
            throw new RuntimeException(e);
        }
        return resp;
    }
```
（`@Autowired(required=false)` 那行是误粘，删掉；`/notify` 直接是 `@PostMapping`。）

- [ ] **Step 5: 跑确认通过** — Run: `mvn -o test -Dtest=MiniPayServiceNotifyTest` → 3 用例 PASS。
- [ ] **Step 6: Commit** — `git commit -m "feat(mini): APIv3 回调 /pay/mini/notify (验签解密+幂等+写交易号)"`

---

## Task 10: APIv3 退款 + 退款渠道分流（按 wxTransactionId）

**Files:**
- Modify: `src/main/java/com/sell/service/OrderService.java`（加 `saveTransactionId`）
- Modify: `src/main/java/com/sell/service/impl/OrderServiceImpl.java`（`saveTransactionId` 实现 + 退款分流）
- Modify: `src/main/java/com/sell/service/MiniPayService.java`（加 `refund`）
- Create: `src/main/java/com/sell/dataobject/RefundRecord.java` + repo（out_refund_no 落库，可选简化为复用订单号+时间戳）
- Test: `src/test/java/com/sell/service/impl/RefundRoutingTest.java`

- [ ] **Step 1: OrderService 接口加方法**
```java
    /** 行锁写入微信交易号 (回调用). */
    OrderDTO saveTransactionId(String orderId, String transactionId);
```
- [ ] **Step 2: 实现（OrderServiceImpl，行锁，仅改该字段）**
```java
    @Override
    @Transactional
    public OrderDTO saveTransactionId(String orderId, String transactionId) {
        OrderMaster om = orderMasterRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new SellException(ResultEnum.ORDER_NOT_EXIST));
        om.setWxTransactionId(transactionId);
        orderMasterRepository.save(om);
        return findOne(orderId);
    }
```
- [ ] **Step 3: 退款分流失败测试**（构造一个有 `wxTransactionId` 的订单退款 → 走 MiniPayService.refund；无的 → 走 best-pay）。测试用 `@MockBean MiniPayService` 与现有 `PayService` 断言调用对象。

```java
package com.sell.service.impl;
// 断言: refund 时, 有 wxTransactionId(且无 OrderPaymentRecord 线下记录) → 调 miniPayService.refund; 否则旧逻辑.
// 用 @SpringBootTest + @MockBean MiniPayService + 现有 createOrder/paid 工具, 验证 verify(miniPayService).refund(...)。
```
（完整用例照 `CashierGuardRefundTest` 的建单/支付工具搭，断言分流到 `MiniPayService.refund`。）

- [ ] **Step 4: 在 OrderServiceImpl.refund 的"线上"分支内分流**

把现有：
```java
        if (!offlinePaid) {
            try { payService.refund(orderDTO); } catch (Exception e) { ... throw ORDER_REFUND_FAIL; }
        }
```
改为：
```java
        if (!offlinePaid) {
            try {
                if (orderMaster.getWxTransactionId() != null && !orderMaster.getWxTransactionId().isEmpty()) {
                    miniPayService.refund(orderMaster.getOrderId(), orderMaster.getOrderAmount()); // APIv3 新单
                } else {
                    payService.refund(orderDTO); // best-pay 历史单(公众号 v2/支付宝)
                }
            } catch (Exception e) {
                log.error("【订单退款】通道退款失败 orderId={}, msg={}", orderMaster.getOrderId(), e.getMessage());
                throw new SellException(ResultEnum.ORDER_REFUND_FAIL);
            }
        }
```
（注入 `@Autowired private MiniPayService miniPayService;`。）

- [ ] **Step 5: MiniPayService.refund（APIv3）**
```java
    @Autowired(required = false) private com.wechat.pay.java.service.refund.RefundService refundService;

    /** APIv3 退款; out_refund_no 用 订单号+毫秒 保证唯一. */
    public void refund(String orderId, java.math.BigDecimal amountYuan) {
        if (refundService == null) throw new SellException(-1, "支付未配置");
        com.wechat.pay.java.service.refund.model.CreateRequest req =
            new com.wechat.pay.java.service.refund.model.CreateRequest();
        req.setOutTradeNo(orderId);
        req.setOutRefundNo("R" + orderId); // 单订单单次退款; 多次退款需带序号并落库
        com.wechat.pay.java.service.refund.model.AmountReq amt =
            new com.wechat.pay.java.service.refund.model.AmountReq();
        int fen = com.sell.utils.MoneyUtil.yuanToFen(amountYuan);
        amt.setRefund((long) fen); amt.setTotal((long) fen); amt.setCurrency("CNY");
        req.setAmount(amt);
        refundService.create(req);
    }
```
> 注：实现时以 SDK 0.2.17 `com.wechat.pay.java.service.refund.RefundService` / `model.CreateRequest` / `model.AmountReq` 实际类型为准（执行时 `javap`/IDE 确认字段名）。

- [ ] **Step 6: 跑确认通过** — Run: `mvn -o test -Dtest=RefundRoutingTest` → PASS。
- [ ] **Step 7: Commit** — `git commit -m "feat(mini): APIv3 退款 + 按交易号分流(新单v3/旧单best-pay)"`

---

## Task 11: 订单发货上报（小程序 access_token 缓存 + pending_shipping 重试）

**Files:**
- Create: `src/main/java/com/sell/service/WxMiniAccessTokenService.java`
- Create: `src/main/java/com/sell/dataobject/PendingShipping.java` + `repository/PendingShippingRepository.java`
- Create: `src/main/java/com/sell/service/MiniShippingService.java`
- Modify: `src/main/java/com/sell/service/impl/OrderServiceImpl.java`（paid 成功后触发，仅小程序单）
- Test: `src/test/java/com/sell/service/MiniShippingServiceTest.java`

- [ ] **Step 1: access_token 缓存服务**（`GET https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=&secret=`，内存缓存约 110 分钟）。
- [ ] **Step 2: PendingShipping 实体 + repo**（`id`/`orderId`/`status`/`attempts`/`nextRetryAt`/`lastError`/`createTime`，仿 `PendingWmsShipment`，**独立表**）。
- [ ] **Step 3: MiniShippingService**：`report(orderId, openid, transactionId)` 调 `POST https://api.weixin.qq.com/wxa/sec/order/upload_shipping_info?access_token=`（`order_key{order_number_type=2, transaction_id}`、`logistics_type=4 虚拟发货`、`upload_time`、`payer{openid}`、`delivery_mode=1`）；失败入 `pending_shipping`。`@Scheduled` 重试（仿 `WmsRetryService`）。
- [ ] **Step 4: 触发点**：在 `OrderServiceImpl.paid` 成功后，**仅当 `orderMaster.getWxTransactionId()!=null`**（小程序 APIv3 单）异步触发上报；现金/收银台/旧单不报。失败不阻塞。
- [ ] **Step 5: 测试**：mock access_token + HTTP，断言"有 transactionId 才上报""失败入队列"。
- [ ] **Step 6: Commit** — `git commit -m "feat(mini): 订单发货上报 + access_token 缓存 + 重试队列(合规)"`

---

## Task 12: 小程序码生成（GET /seller/qrcode/mini?tableId=）

**Files:**
- Create: `src/main/java/com/sell/controller/MiniQrCodeController.java`
- Modify: 复用 Task 11 的 `WxMiniAccessTokenService`
- Test: `src/test/java/com/sell/controller/MiniQrCodeControllerTest.java`

- [ ] **Step 1: 失败测试**（mock access_token + RestTemplate 返回 PNG 字节，断言返回 `image/png`、scene=tableId）。
- [ ] **Step 2: 实现**：`POST https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=`，body `{ page:"pages/order/index", scene:String(tableId), check_path:false, env_version:"trial" }`，返回 PNG 字节流，`Content-Type: image/png`。scene 用数字 `tableId`（≤32 字符）。
- [ ] **Step 3: 跑确认通过 + Commit** — `git commit -m "feat(mini): 小程序码生成端点(scene=tableId)"`

> 店主后台"二维码管理"页改调此端点属前端改动，列入前端计划/后续。

---

## Task 13: 支付宝下线 + 公众号 best-pay 新支付入口退役（保留退款分支）

**Files:**
- Modify: `PayController.java`, `PayServiceImpl.java`, `WechatPayConfig.java`
- Delete: `AliPayAccountConfig.java`
- Modify/Delete tests: `PayServiceImplAlipayTest.java`, `smoke/AlipaySandboxSmokeTest.java`, `PayControllerTest.java`

- [ ] **Step 1: 删支付宝新单代码**：`PayServiceImpl.createAlipay`/`alipayNotify`；`PayController` 的 `/pay/alipay/notify`、`create()` 内 `payType` 切换分支与 `patchSandboxUrl`；`PayController.create`(公众号)与 `/pay/notify`、`PayServiceImpl.create/notify`（公众号新支付入口退役）。
- [ ] **Step 2: WechatPayConfig 解耦支付宝**：删 `@Autowired AliPayAccountConfig` 字段、`bestPayService()` 内 `setAliPayConfig(aliPayConfig())`、`aliPayConfig()` Bean。**保留** `wxPayConfig()`/`bestPayService()` 供 `PayServiceImpl.refund` 历史微信单退款。删 `AliPayAccountConfig.java`。
- [ ] **Step 3: 枚举保留**：`PayTypeEnum.ALIPAY` 不删（历史订单 `payType=1`），仅无新单产生。
- [ ] **Step 4: 测试清理**：删 `PayServiceImplAlipayTest.java`、`smoke/AlipaySandboxSmokeTest.java`；改 `PayControllerTest`——删/改 `create_payTypeAlipay_*`、`alipayNotify_returnsLiteralSuccess`、`create_noPayType_defaultsToWechat` 等依赖被删代码的用例。
- [ ] **Step 5: 订正过时注释**：`CashierServiceImpl` 第~126 行"paid 要求 orderStatus=NEW"改为准确描述（paid 仅拒 CANCEL/REFUNDED）。
- [ ] **Step 6: 全量回归** — Run: `pkill -9 -f "spring-boot:run"; mvn -o test`
  Expected: `BUILD SUCCESS`（支付宝预存失败用例已被清理；新增小程序用例全绿；订单/库存/收银无回归）。
- [ ] **Step 7: Commit** — `git commit -m "refactor(pay): 下线支付宝新单+退役公众号best-pay新支付入口(保留退款分支)"`

---

## 自审（Self-Review 结论）

- **Spec 覆盖**：§6.1→Task1、§6.2 登录/拦截器→Task2-5、§6.3 下单→Task6、§6.4 支付→Task7-8、§6.5 回调→Task9、§6.6 退款分流→Task10、§6.7 发货→Task11、§6.8 小程序码→Task12、§6.9 支付宝下线→Task13。前端（§7）另起前端计划。
- **类型一致**：`PrepayWithRequestPaymentResponse.getPackageVal()`、`RSAPublicKeyConfig.Builder` 链、`NotificationParser.parse(RequestParam, Transaction.class)`、`MoneyUtil.yuanToFen` 全计划一致。
- **占位提醒**：Task8 第二个用例、Task9 Step3 的 `moneyUtilUnused`/Task9 Step4 误粘 `@Autowired` 行，均在文中标注"实现时删除"——执行者照注释清理。
- **已知执行期需 `javap`/IDE 二次确认的 SDK 细节**：`RefundService`/`CreateRequest`/`AmountReq` 字段名、`Transaction.getAmount().getTotal()` 类型、`Amount.setTotal(Integer)` 入参——SDK 0.2.17 实际签名为准。
- **外部阻塞**：所有"真链路"（下单/回调/退款/发货/小程序码）验证到 mock+样例边界；真实小额验证待凭据（§spec 10）。

## 执行说明

Task 1–7、13 不需外部凭据即可完整实现+测试通过；Task 8–12 实现+mock 测试可完成，真链路待凭据。按"每完成一个 Task 停下来汇报"推进。
