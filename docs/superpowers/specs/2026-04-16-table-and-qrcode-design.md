# 桌台管理 + 二维码管理 实现规格

> 批次范围：PRD `doc/PRD-咖啡店管理系统.md` 第五章（桌台管理）
> 包含：5.1 桌台区域、5.2 桌台、5.3 二维码管理
> 不包含：订单退款（PRD 6.3）、收银台增强（PRD 7.3）—— 留给下一批
> 实现日期：2026-04-16

---

## 一、本批次目标

完整实现 PRD 第五章描述的全部桌台管理功能，并打通"扫桌台二维码 → H5 点餐 → 订单绑定到桌"的闭环。完成后 P0 中"桌台管理"和"二维码管理"两项达到可上线状态。

---

## 二、PRD 复述（直接对照实现）

### 5.1 桌台区域 — 后台店主操作
| 操作 | 说明 |
|------|------|
| 新增区域 | 创建区域（如"吧台区"、"大厅A区"、"户外区"、"VIP区"） |
| 编辑区域 | 修改区域名称 |
| 删除区域 | 删除空区域（区域下还挂有桌台时禁止删除） |

### 5.2 桌台
**页面结构**：左侧区域列表（显示桌台数量） + 右侧桌台列表表格

| 操作 | 说明 |
|------|------|
| 新增桌台 | 设置桌号（如A1）、所属区域、座位数 |
| 编辑桌台 | 修改桌台信息 |
| 删除桌台 | 删除桌台 |
| 桌台排序 | 调整桌台展示顺序 |
| 批量新增 | 按规则批量创建桌台 |

### 5.3 二维码管理
| 操作 | 说明 |
|------|------|
| 生成二维码 | 为桌台生成扫码点餐二维码 |
| 批量生成 | 按区域批量生成 |
| 下载打印 | 下载二维码图片用于制作桌贴 |
| 自定义样式 | 设置二维码外观（本批次只做基础样式：尺寸可调） |

---

## 三、PRD 未覆盖的实现层决策

### 3.1 数据模型

新增两张表，并给 `order_master` 加一个外键字段。

```sql
-- 桌台区域
create table `restaurant_area` (
    `area_id` int not null auto_increment,
    `area_name` varchar(64) not null comment '区域名',
    `sort_order` int not null default 0,
    `create_time` timestamp not null default current_timestamp,
    `update_time` timestamp not null default current_timestamp on update current_timestamp,
    primary key (`area_id`)
) comment '桌台区域';

-- 桌台
create table `restaurant_table` (
    `table_id` int not null auto_increment,
    `area_id` int not null,
    `table_code` varchar(32) not null comment '桌号(对外展示与扫码识别), 如A1',
    `seat_count` int not null default 4 comment '座位数',
    `sort_order` int not null default 0,
    `enabled` tinyint(1) not null default 1,
    `create_time` timestamp not null default current_timestamp,
    `update_time` timestamp not null default current_timestamp on update current_timestamp,
    primary key (`table_id`),
    unique key `uk_table_code` (`table_code`),
    key `idx_area_id` (`area_id`)
) comment '桌台';

-- 订单关联桌台
alter table `order_master`
    add column `table_id` int default null comment '关联桌台id',
    add key `idx_table_id` (`table_id`);
```

**决策依据**：
- `table_code` 全店唯一：扫码 URL 沿用现有 `/h5/index.html?table=A1` 设计，无须改动 H5 已上线代码。
- `OrderMaster.table_number`（已存在的 varchar）保留并继续写入 `table_code` 文本，让小票打印（`PrintTicketDTO.tableNumber`）和历史订单展示不受影响。
- 不存桌台占用状态字段 — 实时算（见 3.4），避免状态漂移。

### 3.2 桌台占用判定

桌台是否"使用中" = 是否存在 `table_id = ?` 且 `order_status IN (新订单, 制作中, 待取餐)` 的订单。

```sql
SELECT DISTINCT table_id FROM order_master
WHERE table_id IS NOT NULL
  AND order_status IN (0, 1, 2);
```

桌台列表页 SSR 时一次性取该集合，列表渲染时给桌台打"使用中"标。

### 3.3 二维码生成

- 引入依赖：`com.google.zxing:core:3.5.3` + `com.google.zxing:javase:3.5.3`
- 服务接口：`QrCodeService.generatePng(String content, int sizePx) → byte[]`
- 二维码内容：`{shopBaseUrl}/h5/index.html?table={tableCode}`
  - `shopBaseUrl` 从 `ShopConfig`（KV 配置表）读取，配置 key = `shop.base.url`
  - 若未配置则回退到当前请求的 `scheme://host[:port]`
- 单桌下载：返回 `Content-Type: image/png` 流，文件名 `qrcode-{tableCode}.png`
- 批量下载（按区域）：返回 ZIP 流，每张 PNG 以 `{tableCode}.png` 命名

### 3.4 与现有代码的对接

| 文件 | 改动 |
|------|------|
| `dataobject/OrderMaster.java` | 加 `private Integer tableId;` |
| `form/OrderForm.java` | 加 `private Integer tableId;` |
| `converter/OrderForm2OrderDTOConverter.java` | 复制 tableId 到 DTO |
| `dto/OrderDTO.java` | 加 `tableId` 字段 |
| `service/impl/OrderServiceImpl.create()` | 若 `tableId` 非空：从 `RestaurantTableService` 取 `table_code` 回填 `tableNumber`；若 `tableId` 空但 `tableNumber` 非空（兼容老 H5）：反查 table_id 回填 |
| `controller/BuyerOrderController.create()` | 不需改，OrderForm 字段加完自动绑定 |
| `static/h5/index.html` | 取 URL 参数时同时尝试 `?tableId=` 和 `?table=`；提交订单时优先传 `tableId`。**新生成的二维码 URL 改为 `?tableId={id}&table={code}` 双参数**，老二维码 `?table=A1` 仍兼容 |
| `templates/common/nav.ftl` | 加菜单项"桌台管理" → `/seller/area/list` 和 "桌台" → `/seller/table/list` |

### 3.5 后台 API（Controller 路由）

延续现有 Seller* 命名风格。

**区域** — `SellerAreaController`：
- `GET  /seller/area/list` — 区域列表页（FreeMarker SSR，含每区域桌台数）
- `POST /seller/area/save` — 新增/编辑（areaId 为空 = 新增）
- `POST /seller/area/delete` — 删除（区域下有桌台则抛 SellException）

**桌台** — `SellerTableController`：
- `GET  /seller/table/list` — 桌台列表页（左侧区域 + 右侧桌台表）
- `POST /seller/table/save` — 单个新增/编辑
- `POST /seller/table/batchCreate` — 批量新增（参数：areaId、prefix、startNum、endNum、digitWidth、seatCount）
- `POST /seller/table/delete` — 删除（占用中的桌不可删）
- `POST /seller/table/sort` — 排序（接收 tableId 顺序数组，更新 sort_order）

**二维码** — `SellerQrCodeController`：
- `GET /seller/qrcode/single?tableId=&size=300` — 单桌 PNG 流
- `GET /seller/qrcode/batch?areaId=&size=300` — 按区域 ZIP 下载（areaId 为空 = 全部桌）
- `GET /seller/qrcode/preview?tableId=` — 在后台页面里展示预览（同 single，前端用于弹窗预览）

### 3.6 页面（FreeMarker）

延续 `templates/dashboard/index.ftl`、`templates/product/list.ftl` 的视觉风格。

**`templates/area/list.ftl`**
- 表格列：序号、区域名、桌台数、操作（编辑/删除）
- 顶部按钮：新增区域（弹窗输入名称）

**`templates/table/list.ftl`**
- 左：区域列表（点击筛选）+ "全部"项
- 右：桌台表格列：序号、桌号、所属区域、座位数、状态（空闲/使用中）、操作（编辑/二维码/删除）
- 顶部按钮：新增桌台、批量新增（弹窗：前缀+起止号+座位数+所属区域）、批量下载二维码（按当前筛选区域）
- 二维码弹窗：展示预览图 + "下载"按钮

### 3.7 验证规则

- `area_name` 非空、长度 1-64
- `table_code` 非空、长度 1-32、全店唯一（DB 约束 + service 层友好报错）
- `seat_count` ≥ 1
- 删除区域：检查 `restaurant_table` 下是否有该 area_id
- 删除桌台：检查是否被未完结订单占用
- 批量新增：`endNum >= startNum`，生成时跳过冲突 `table_code` 并在响应里提示

### 3.8 测试

- Repository（H2 + JPA）：CRUD 基础
- `RestaurantTableServiceTest`：占用计算、批量新增规则、唯一约束冲突处理
- `QrCodeServiceTest`：生成的 PNG 字节非空、用 ZXing 反向解码内容正确
- Controller MockMvc：区域/桌台 CRUD、批量新增、单桌 PNG 流响应
- `OrderServiceImplTest`：扩展现有用例，覆盖 tableId 双向回填

### 3.9 不在本批次

- 操作日志（PRD 12.5，P2）
- 二维码自定义样式（颜色、Logo 嵌入）—— 仅做基础尺寸调节
- 桌台扫码后顾客的会员识别（依赖未做的会员体系）
- 订单退款 / 收银台改造（下一批）

---

## 四、上线验收清单

- [ ] DB schema 迁移在 H2（dev）和 MySQL（prod）下都能执行
- [ ] 后台可完成区域 CRUD、桌台 CRUD、批量新增、排序
- [ ] 单桌二维码可下载，扫码后跳转到 H5 并自动带桌信息
- [ ] 按区域 ZIP 批量下载，每张 PNG 以桌号命名
- [ ] 新订单创建后 `order_master.table_id` 与 `table_number` 同步写入
- [ ] 桌台列表正确显示"使用中"标
- [ ] 占用中桌不可删，区域有桌时不可删，均给出友好提示
- [ ] 既有 H5 旧二维码 `?table=A1` 仍可用
- [ ] 全部新增/修改后的单元测试通过

---

## 五、依赖与风险

**依赖**：
- 新增 Maven 依赖 `com.google.zxing:core` + `javase`（共约 200KB，纯 Java，无 native）
- 不引入新外部服务

**风险**：
- 旧订单 `table_id` 为 NULL：可接受，桌台报表只统计 table_id 非空的订单
- `shop.base.url` 未配置：回退到请求 host，需在管理后台首次进入桌台页时给出提示横幅（本批次提示文案即可，不强制配置）
