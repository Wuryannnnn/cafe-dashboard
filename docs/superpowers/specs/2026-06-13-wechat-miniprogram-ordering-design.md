# 微信小程序点单改造 — 设计规格

- 日期：2026-06-13
- 范围：把顾客点单端从 H5 改造为**原生微信小程序（TypeScript）**，后端新增小程序登录 + 微信支付 APIv3 链路，下线支付宝（及随之退役的公众号 best-pay 新支付入口）；店主后台（React 网页）不动。
- 状态：设计已与店主确认，并经对抗式自审收敛（26 项修正已并入），待写实现计划。

---

## 1. 背景与现状

- 后端：Spring Boot 3.2.5 / Java 17；订单、库存（含 WMS 整合）、退款、收银台均已落地并经审计加固。
- 现有支付：`best-pay-sdk 1.3.7`，跑微信公众号 JSAPI（`WXPAY_MP`，微信支付 **APIv2 / MD5**）+ 支付宝 WAP。
- 现有 openid：来自公众号网页授权（`WechatController`，`snsapi_base`）。
- 现有顾客身份：**没有独立顾客表**。openid 仅作为 `OrderMaster.buyerOpenid` 字段存在；`Member` 实体是已搁置的储值会员体系，不复用作点单身份。
- 顾客端：React 单体内的 `frontend/src/features/h5/` 三屏（`order-page` / `pay-page` / `status-page`）。
- 现有桌码：`QrCodeServiceImpl.buildTableQrUrl` 产出 H5 URL `base + "/order?table=" + tableCode`，`SellerQrCodeController` 编码成普通 PNG 二维码——**是 H5 链接二维码，不是小程序码**。
- 店主后台：同一 React 仓库（TanStack Router/Query + Tailwind4 + Radix + Clerk），PC 端仪表盘。

## 2. 目标

1. 顾客在微信内扫桌上的**小程序码**直接进入对应桌的点单页，下单、在线微信支付、看制作/叫号状态。
2. 后端复用现有订单/库存/退款/收银核心，**仅新增**小程序登录与小程序支付链路。
3. 下线支付宝（顾客端只留微信）；公众号 best-pay 新支付入口随之退役（但保留 best-pay 退款分支供历史单退款，见 §6.5）。
4. 满足微信交易类小程序的合规要求（餐饮类目、订单发货上报）。

## 3. 关键技术结论（来自调研，决定设计）

- **小程序支付是微信支付 APIv3（RSA 签名 + AES-256-GCM 回调解密）**，与现有 best-pay 的公众号 v2/MD5 链路是两套机制；`best-pay-sdk 1.3.7` 在小程序 JSAPI + APIv3 上不够用 → **采用官方 `wechatpay-java`（APIv3）新建小程序支付链路**（方案 A，已确认）。
- **小程序支付的 openid 必须是「小程序 appId 对应的 openid」**，不能复用公众号网页授权拿到的 openid → 必须新建小程序登录链路。
- `wx.requestPayment` 需要后端返回 5 个字段：`timeStamp / nonceStr / package(=prepay_id=xxx) / signType(=RSA) / paySign`。
  - `paySign = SHA256withRSA(appId\n timeStamp\n nonceStr\n package\n)`，用商户 API 私钥签。
  - **`timeStamp` 必须是 Unix「秒」级 10 位字符串**（`System.currentTimeMillis()/1000`）；后端签名所用值与返回前端的值必须是同一个秒级字符串，否则调起失败。
- **金额换算（元↔分）全程走整数，不经 double**：`分 = new BigDecimal(元).movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact()`。下单与回调金额比对都用分（int）。避免 `0.1/0.07/19.99` 等浮点误差。
- 回调验签是 APIv3 机制：**4 个响应头** `Wechatpay-Timestamp / Wechatpay-Nonce / Wechatpay-Signature / Wechatpay-Serial`，验签明文 = `timestamp\n nonce\n body\n`（`Wechatpay-Serial` 仅用于选取对应平台公钥/证书）；body 用 `apiV3Key` 做 AES-256-GCM 解密。**实现用官方 `wechatpay-java` 的 `NotificationParser` 处理，不手写验签/解密**。
- 验签采用**「微信支付公钥」模式**（新主体多为公钥模式），据此固定 SDK `Config` 初始化与凭据集合。
- 交易类小程序须接入「订单发货管理」上报，否则微信限制支付权限。
- **微信支付 APIv3 无公开沙箱**（v2 沙箱已停）；后端在拿到真实凭据前只能 mock 到 SDK 调用层 + 构造验签/解密样例 + 金额换算单测，真链路必须等 §10 外部凭据到位。
- 前端：现有 React/Radix/Tailwind 是 DOM 组件，**不能编译进小程序**；顾客端用**独立的原生小程序 + TS 工程**（已确认）。
- 主体资质：必须是**企业/个体工商户**主体的已认证小程序 + 同主体商户号（店主已具备）。

## 4. 非目标（第一版有意不做 / YAGNI）

- 手机号授权（`getPhoneNumber`，每次 0.03 元、非刚需）；本版 `session_key` 不参与任何 `encryptedData` 解密。
- 头像昵称个性化（默认 openid 匿名下单）。
- 会员体系与小程序打通（此前已约定搁置）。
- unionid / 多端账号互通（只有单一小程序，无需）。
- 店主后台搬进小程序。

## 5. 总体架构（维持"多套独立系统"取向）

```
顾客微信 → 扫小程序码(scene=tableId)
   → [小程序前端 miniapp/]  ──HTTPS──>  [后端 Spring Boot]
        点单 / 支付 / 状态                 ├─ 新增: POST /mini/login        (code2session + 自建token)
                                          ├─ 新增: POST /mini/order/create (openid 从 token 注入)
                                          ├─ 新增: POST /pay/mini/create   (APIv3 JSAPI下单 + RSA paySign)
                                          ├─ 新增: POST /pay/mini/notify   (NotificationParser 验签解密 → paid)
                                          ├─ 新增: APIv3 退款 / 发货上报 / 小程序码生成
                                          ├─ 复用: 订单/库存/退款/收银/WMS 核心
                                          └─ 移除: 支付宝新单入口; 退役公众号 best-pay 新支付入口

店主后台(React 网页)         ── 不动 ──>  同一后端 (+ 二维码管理页改调小程序码端点)
```

## 6. 后端设计（新增为主，不动订单/库存/退款核心）

### 6.1 依赖与配置

- `pom.xml` 新增 `com.github.wechatpay-apiv3:wechatpay-java`（APIv3 官方 SDK）。
- 新增配置类 `WechatMiniConfig`（`@ConfigurationProperties(prefix="wechat.mini")`）：
  - `appId` / `appSecret` —— 小程序 appId 与密钥（登录用，**secret 仅后端**）。
  - `mchId` —— 商户号（可与现有 `wechat.mchId` 同号；独立配置便于解耦）。
  - `merchantSerialNo` —— 商户 API 证书序列号。
  - `privateKeyPath` —— 商户 API 私钥 `apiclient_key.pem` 路径。
  - `apiV3Key` —— APIv3 密钥（回调 GCM 解密用）。
  - `wechatPayPublicKeyId` / `wechatPayPublicKeyPath` —— 微信支付公钥（**公钥模式**验签）。
  - `notifyUrl` —— `https://域名/sell/pay/mini/notify`。
- 用 `wechatpay-java` 的 `RSAPublicKeyConfig`（公钥模式）初始化全局 `Config`，派生 `JsapiServiceExtension`（下单/调起签名）、`RefundService`（退款）、`NotificationParser`（回调）Bean。
- `prod` profile 全部读环境变量（沿用现有约定）；`local` 留占位，发起时返回可读错误而非 500。

### 6.2 小程序登录单元

- **职责**：把 `wx.login` 的临时 `code` 换成稳定顾客身份 + 自建会话 token。
- **接口**：`POST /mini/login`，body `{ code }` → 返回 `{ token, openid }`。
- **实现** `WxMiniLoginService.login(code)`：
  1. `GET https://api.weixin.qq.com/sns/jscode2session?appid=&secret=&js_code=&grant_type=authorization_code`。
  2. **先判 `errcode`**：响应是 HTTP 200 + JSON，失败时含 `errcode!=0`（如 40029 code 失效、45011 频限、40226 高风险）；`errcode!=0` 即返回可读错误（沿用现有约定），不可把空 openid 当成功。
  3. 成功则 upsert `mini_user`（见 §8），`session_key` 只留后端、绝不下发。
  4. 签发自建 token（JWT，subject=openid，有效期 1–2 天，短于 session_key）。
- **鉴权拦截器**（新增，独立于店主后台 `AdminSecurityConfig`，各拦各的前缀）：
  - `addPathPatterns`：`/mini/order/**`、`/pay/mini/create`、`/buyer/order/status`、`/buyer/order/detail`（顾客只读也要 token）。
  - `excludePathPatterns`：`/mini/login`、`/pay/mini/notify`（微信服务器回调，无 token）。
  - 解析 token → openid 注入请求上下文；**忽略/拒绝前端传入的 openid**（防伪造他人 openid）。

### 6.3 小程序下单单元

- **职责**：顾客端下单，openid 由服务端身份注入，不由前端传。
- **接口**：`POST /mini/order/create`，body `{ tableId, items }`（openid 从 token 取）。
- **实现**：组装 `OrderDTO`（`buyerOpenid=token.openid`、`tableId`、`buyerName/buyerPhone` 服务端给默认值，避免现有 `OrderForm` 的 `@NotEmpty name/phone/openid` 校验失败），复用现有 `OrderService.create`（含 §B1 修复后的"收银台/扫码"开关逻辑、库存扣减、WMS 出库）。
- **不复用** `/buyer/order/create`（其 `OrderForm` 强校验 name/phone/openid，且 openid 来自前端）。该 H5 端点随 H5 下线后清理。

### 6.4 小程序支付单元（APIv3）

- **接口**：`POST /pay/mini/create`，body `{ orderId }`（openid 从 token 取）→ 返回 `{ timeStamp, nonceStr, package, signType:"RSA", paySign }`。
- **实现** `MiniPayService.create(order, openid)`：
  1. 校验订单存在、未支付、金额>0。
  2. APIv3 `POST /v3/pay/transactions/jsapi`：`appid=小程序appId`、`mchid`、`description`、`out_trade_no=orderId`、`notify_url`、`amount.total=元转分(整数)`、`payer.openid=小程序openid` → 拿 `prepay_id`。
  3. 生成调起签名（`timeStamp` 秒级 10 位）→ 返回 5 字段。
- **错误处理**：网关失败捕获后返回 `{code:-1,msg}`，不抛 500。

### 6.5 支付回调单元（APIv3）— 幂等是重点

- **接口**：`POST /pay/mini/notify`。
- **实现** `MiniPayService.notify(headers, body)`：
  1. 用 `NotificationParser` 完成 4 头验签 + GCM 解密 → 拿 `out_trade_no / amount / transaction_id`。
  2. **幂等前置判重（关键）**：先 `findOne(out_trade_no)`，若 `payStatus==SUCCESS` 直接返回 `{code:"SUCCESS"}`，**不再调 `paid()`**。照现有 `PayServiceImpl.notify` 的写法——因为 `OrderService.paid` 对非 WAIT 状态是**抛 `ORDER_PAY_STATUS_ERROR` 异常而非跳过**；若直接调 `paid()` 靠它幂等，微信重复回调会让它抛错、回非 200、触发微信无限重试。
  3. 金额比对（分）；一致则调现有 `OrderService.paid(order)`（其行锁 + 状态校验是并发双回调的第二道防线）。
  4. **存渠道标记**：回调成功时把 `transaction_id` 写入订单（新增字段，见 §8），作为退款分流依据（§6.6）。
  5. 返回 APIv3 应答格式（成功 `{code:"SUCCESS"}`，失败非 200 让微信重试）。
- **注**：`OrderService.paid` 实际仅拒绝 CANCEL/REFUNDED，允许 NEW/MAKING/READY/FINISHED（支持先食后付）——`CashierServiceImpl` 第 126 行"paid 要求 orderStatus=NEW"的注释已过时，实现时顺手订正。

### 6.6 退款单元（APIv3）+ 渠道分流

- 新增 APIv3 退款：`POST /v3/refund/domestic/refunds`，字段 `out_trade_no`、`out_refund_no`（**后端生成并落库**，用 `订单号+序号` 保证唯一与可查）、`amount{refund,total,currency:"CNY"}`、可选退款结果 `notify_url`（与支付回调同机制验签解密）。
- **退款渠道分流（解决新旧并存）**：现有 `OrderServiceImpl.refund` 只按"有无 `OrderPaymentRecord`"分线下/线上，无法区分 APIv3 新单与 best-pay 旧单（两者 `PayType` 都是 WECHAT=0）。改为：
  - 有 `transaction_id`（§6.5 回调写入）→ **APIv3 退款**。
  - 无 `transaction_id` 但线上微信旧单 → **保留 best-pay 退款分支**（历史 v2 已付单仍可退），直到旧单退款窗口关闭。
  - 线下/现金单（有 `OrderPaymentRecord`）→ 不调网关（现有逻辑不动）。
- 即 best-pay 退款分支**暂留**（不随支付宝一起删），仅退役其新支付入口。

### 6.7 订单发货上报单元（合规必做）

- **职责**：交易类小程序保支付权限的硬要求。
- **触发点（唯一确定）**：在 `OrderService.paid` 支付成功后**异步**上报，类型为虚拟发货/服务完成，`out_trade_no=orderId`。先食后付/已完结后付款等时序也一律以 **paid 成功**为准（删除"或完结"的二义）。
- **范围限定**：**仅对经小程序 APIv3 JSAPI 支付成功的订单上报**（用是否有 `transaction_id` 判定），**排除现金/收银台手动单/非小程序微信单**，避免错报。
- **实现**：调小程序「发货信息录入」`uploadShippingInfo`（需小程序 access_token，见 §6.8），字段 `out_trade_no / logistics_type=4(虚拟发货) / upload_time / payer.openid`。
- **重试**：新建独立 `pending_shipping` 重试表（**不与 WMS 的 `pending_wms_shipment` 混用**），参考 `WmsRetryService` 的补偿队列思路，失败重试，不阻塞主流程。

### 6.8 小程序码生成单元（桌码）

- **现状缺口**：后端无小程序码生成、无小程序 access_token 管理（现有 access_token 仅公众号 OAuth）。
- **新增**：后端端点 `GET /seller/qrcode/mini?tableId=` → 调微信 `wxacode.getUnlimited`（`page=pages/order/index`、`scene=tableId`）生成小程序码 PNG。
- **access_token**：新增小程序 access_token 获取 + **内存缓存（约 2 小时，提前刷新）**，与登录的 `jscode2session` 不同接口。
- **scene**：用**数字 `tableId`**（短、≤32 字符、字符集安全）；小程序 `onLoad` 解析 `scene` → `tableId` → 走现有 `tableId→tableNumber` 回填路径。
- **店主后台**：现有"二维码管理"页改调此端点（产出小程序码替代 H5 链接二维码）。

### 6.9 支付宝下线 + 公众号新支付入口退役（移除）

- **删除（支付宝新单）**：`PayServiceImpl.createAlipay` / `alipayNotify`；`PayController` 的 `/pay/alipay/notify`、`create()` 里 `payType` 微信↔支付宝切换分支与沙箱地址 patch；`AliPayAccountConfig`。
- **删除（WechatPayConfig 的支付宝耦合，需一并删否则装配失败）**：`@Autowired AliPayAccountConfig` 字段、`bestPayService()` 内 `setAliPayConfig(aliPayConfig())` 调用、`aliPayConfig()` Bean。
- **退役（公众号 best-pay 新支付入口）**：`PayController.create`（公众号 JSAPI）+ `/pay/notify`、`PayServiceImpl.create/notify`、`WechatPayConfig` 的 `wxPayConfig()`/`bestPayService()` —— 这些随 H5/公众号顾客端下线而退役。**但 `PayServiceImpl.refund` 的微信 best-pay 分支保留**（§6.6 历史单退款），故 best-pay 依赖与必要的退款 Bean 暂留，待历史单退款窗口关闭再清理。
- **`PayTypeEnum.ALIPAY` 枚举保留**（历史订单 `payType=1` 反序列化/展示需要），**仅停止产生新支付宝单**——不删枚举（删了会编译失败/历史单炸裂）。历史支付宝已付订单退款：走保留的 best-pay 支付宝退款分支或人工对账退款。
- **测试清理**：删除/改写 `PayServiceImplAlipayTest.java`、`smoke/AlipaySandboxSmokeTest.java`；处理 `PayControllerTest` 内 `create_payTypeAlipay_*`、`alipayNotify_returnsLiteralSuccess`、`create_noPayType_defaultsToWechat` 等依赖被删支付宝代码的用例。

## 7. 前端设计（新建独立原生小程序 + TS 工程）

- **位置**：仓库内新目录 `miniapp/`（独立 `project.config.json` / `tsconfig`），不在现有 React 仓库内改造。
- **页面（迁移自现有 H5 三屏）**：
  - `pages/order/`（点单）← `order-page`：菜单分类、规格/加料、购物车 → 调 `/mini/order/create`。
  - `pages/pay/`（支付）← `pay-page`：调 `/pay/mini/create` → `wx.requestPayment`（替换 `WeixinJSBridge.getBrandWCPayRequest`）。
  - `pages/status/`（状态）← `status-page`：轮询订单状态看制作中/待取餐/叫号。
- **登录态** `utils/auth.ts`：启动 `wx.checkSession` → 通过复用本地 token；失败 `wx.login` 拿 code 立即 `POST /mini/login` 换 token（code 5 分钟一次性，拿到即传）；请求统一带 `Authorization`，401 静默重登。
- **请求封装** `utils/request.ts`：`wx.request` 封装，注入 token、统一错误提示。
- **桌码入口**：`onLoad` 读 `scene`（=`tableId`）→ 进对应桌点单页。
- **去支付宝**：不迁移"微信↔支付宝切换""一码两扫"逻辑，只留微信。
- **合法域名**：小程序后台配置后端 HTTPS 域名为 request 合法域名。

## 8. 数据模型

- **新增 `mini_user` 表**（顾客身份，独立于储值 `Member`，不污染其语义）：
  - `openid`（主键）、`session_key`、`nickname`（可空）、`avatar`（可空）、`create_time`、`update_time`。
  - `login` 的 upsert 与 token 解析注入都走这张表。
- **订单表新增字段**：`wx_transaction_id`（小程序 APIv3 支付的微信交易号，回调时写入）——作为退款分流（§6.6）与发货上报范围（§6.7）的判定依据。
- **新增 `pending_shipping` 表**：发货上报重试队列（独立于 WMS 队列）。
- openid 仍对接现有 `OrderMaster.buyerOpenid`。金额仍以元（BigDecimal）存，支付边界按分换算。

## 9. 安全

- `appSecret` / `session_key` / 商户私钥 / `apiV3Key` **只留后端**，绝不下发前端或写进小程序包。
- 自建 token 有效期短于 session_key；过期静默重登。
- 下单/支付的 openid 一律由 token 注入，**拒绝前端传入的 openid**（防伪造）。
- 回调强制验签 + 解密 + 金额比对，任一不过即拒绝并让微信重试。
- 凭据走环境变量（沿用现有 prod 约定）。

## 10. 上线前置（外部依赖，店主提供 / 阻塞项）

1. 企业/个体工商户主体、已微信认证的小程序 appId。
2. 同主体微信支付商户号，且与小程序 appId 完成 JSAPI 支付绑定授权。
3. APIv3 密钥 + 商户 API 证书 `apiclient_key.pem` + 微信支付公钥（公钥模式）。
4. 餐饮「餐饮服务场所」类目过审（《食品经营许可证》；不要选点餐/外卖平台类目）。
5. 后端公网 HTTPS 域名（回调可达）+ 小程序后台配置 request/回调合法域名。

## 11. 测试策略

- 后端单元/集成：`/mini/login`（jscode2session mock，含 `errcode!=0` 失败分支）、`/mini/order/create`（openid 由 token 注入、拒绝前端 openid）、`/pay/mini/create`（APIv3 下单 mock，断言 5 字段 + 秒级 timeStamp + RSA 签名串）、`/pay/mini/notify`（构造验签+GCM 样例，断言**重复回调幂等**走"先判 SUCCESS 直接返回"、金额比对、写 `transaction_id`）、APIv3 退款（`out_refund_no` 唯一性）、退款分流（有/无 `transaction_id`）、元↔分换算（覆盖 `0.1/0.07/19.99`）。
- 复用现有订单/库存/收银测试（不应回归）。
- **无 APIv3 沙箱**：后端只能 mock 到 SDK 边界；真链路（下单/回调/退款/发货/小程序码）须等 §10 凭据，用体验版 0.01 元真验。
- 沿用验证纪律：跑测试前 `pkill spring-boot:run`；凭据占位时发起返回可读错误。

## 12. 落地顺序（外部依赖最长的先动）

1. **店主办主体/餐饮类目/商户号 + 领 APIv3 证书/密钥/公钥**（阻塞，外部，最先启动）。
2. 后端：`mini_user` 表 + 小程序登录 + 鉴权拦截器 + `/mini/order/create`（凭据占位即可开发，mock 到 SDK 边界）。
3. 后端：APIv3 支付 `/pay/mini/create` + 回调 `/pay/mini/notify`（幂等）+ 订单 `wx_transaction_id` 字段 + APIv3 退款 + 退款分流。
4. 后端：订单发货上报（`pending_shipping` + 小程序 access_token 缓存）。
5. 后端：小程序码端点 `/seller/qrcode/mini` + 店主后台二维码页改调。
6. 前端：新建小程序三屏 + 登录态 + 支付调起 + scene 解析。
7. 下线：支付宝新单入口 + 公众号 best-pay 新支付入口 + 测试清理（保留 best-pay 退款分支）。
8. 真机体验版 0.01 元全链路真验。
9. （可选，后续）手机号 / 头像昵称。

## 13. 风险与对策

- **商户号未开通/未绑定小程序** → 支付无法落地；最高优先级前置项。
- **APIv3 凭据齐全度（私钥/序列号/APIv3 密钥/公钥）** → 任一缺失即验签失败；上线前体验版小额真验。
- **回调幂等漏做** → 微信重复回调让 `paid()` 抛错、回非 200、无限重试；§6.5 强制"先判 SUCCESS 直接返回"。
- **退款分流漏做** → APIv3 新单退款误发 best-pay 网关；§6.6 用 `transaction_id` 分流。
- **发货上报漏接/错报** → 微信限权或对非小程序单错报；§6.7 限定范围 + 唯一触发点。
- **金额换算浮点误差** → §3 全程整数（分）+ 单测覆盖易错值。
- **token 与 session_key 生命周期错配** → token 短于 session_key + checkSession + 静默重登。
- **历史 v2/支付宝已付单退款** → 保留 best-pay 退款分支 + 枚举不删（§6.6/§6.9）。
