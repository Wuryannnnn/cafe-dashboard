# 咖啡厅点单 · 微信小程序（原生 TypeScript）

顾客端三屏：**点单 → 支付 → 取餐状态**。后端复用主项目（Spring Boot），只通过 HTTPS 调用，不在本工程内。

## 目录

```
miniapp/
  app.ts / app.json / app.wxss     小程序入口与全局配置
  project.config.json              开发者工具工程配置（改 appid）
  tsconfig.json / typings/         原生 TS 编译配置与轻量声明
  utils/
    config.ts    后端 BASE_URL（部署后改）
    request.ts   wx.request 封装（表单编码 + token 注入 + 401 重登）
    auth.ts      ensureLogin：wx.login → /mini/login 换 token
  pages/
    order/   点单（左侧分类导航 + 菜单 + 规格/加料弹层 + 购物车明细 → /mini/order/create）
    pay/     支付（/pay/mini/create → wx.requestPayment）
    status/  状态（轮询 /buyer/order/status，叫号）
```

## 点单页能力

- **堂食 / 外带**：顶部切换。外带不绑桌台；后端按 `DiningTypeEnum`（0 堂食 / 1 外带）落库，小票、报表、后台订单列表都按此显示。
- **分类导航**：左侧类目栏，点击跳转右侧菜单，手动滚动时高亮联动。
- **规格 / 加料**：商品带 `skus`/`addons` 时点「选规格」弹层，单选规格、多选加料，价格随选实时变化；无规格商品直接 ±。
- **购物车**：同一商品的不同规格/加料各算一行；底部🛒打开明细可逐行增减、清空。金额全程按「分」整数运算，避免浮点误差。
- 下单时每行带 `skuId` + `addonFee`（加料总价，字符串）+ `addons`（加料明细 JSON，入库做小票）。

## 跑起来（微信开发者工具）

1. 用微信开发者工具「导入项目」，目录选 `miniapp/`。
2. 把 `project.config.json` 的 `appid` 改成**你的小程序 appId**（与商户号同主体、已认证）。
3. 改 `utils/config.ts` 的 `BASE_URL` 为你的后端公网 HTTPS 地址（含 `/sell`）。
4. 调试期：工具「详情 → 本地设置 → 不校验合法域名」打开即可连隧道/本地后端；
   上线前在「小程序后台 → 开发管理 → 服务器域名 → request 合法域名」加上该域名。

## 后端依赖（已实现）

| 屏 | 调用 | 鉴权 |
|---|---|---|
| 点单 | `GET /buyer/product/list`、`POST /mini/order/create` | 下单需 token |
| 支付 | `POST /pay/mini/create` → `wx.requestPayment` | 需 token |
| 状态 | `GET /buyer/order/status?orderId=` | 否 |
| 登录 | `POST /mini/login`（code → token） | 否 |

## 桌台

桌码用**小程序码**（后端 `GET /seller/qrcode/mini?tableId=` 生成，`scene=tableId`）。
顾客扫码进入 `pages/order/index`，`onLoad` 读 `query.scene` 解析桌号并随单提交。

## 在线支付前置（外部，店主提供）

`wx.requestPayment` 真正扣款需要后端配好并 `WECHAT_MINI_ENABLED=true`：
商户号绑定小程序、APIv3 密钥、商户私钥 `apiclient_key.pem`、微信支付公钥、餐饮类目过审、公网回调域名。
未配齐时 `/pay/mini/create` 返回可读错误，支付页提示「支付未配置」。

## 说明

- token 是后端自建的不透明会话标识（非 session_key），2 天有效，401 自动重登。
- 第一版按 openid 匿名下单，不取手机号/头像昵称（YAGNI）。
- 订单备注暂未做：后端 `/mini/order/create` 暂不收备注字段，要加得先动后端。
