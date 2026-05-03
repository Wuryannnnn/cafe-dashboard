# 前端视觉焕新 — 设计规格

**日期**: 2026-04-18
**范围**: 商家后台 (FreeMarker, ~50 页) + H5 顾客点单 (`static/h5/`) + 收银台 (`cashier/desk.ftl`) + 支付页 (`pay/create.ftl`)
**目标**: 三端统一到「温暖咖啡馆」视觉风格 (Manner / Blue Bottle 取向)

## 1. 架构

不逐页改动 50+ FTL，而是建立共享设计系统：

- **新建** `src/main/resources/static/css/cafe-theme.css` — 设计 tokens + 通用组件类
- **修改** `templates/common/header.ftl` — 引入 `cafe-theme.css`，全局生效
- **修改** `templates/common/nav.ftl` — 后台导航焕新（高曝光区）
- **精修 4 个页面**:
  - `templates/dashboard/index.ftl` — 后台首页门面
  - `templates/cashier/desk.ftl` — 员工高频使用
  - `static/h5/index.html` (及关联 css/js) — 顾客扫码入口
  - `templates/pay/create.ftl` — 顾客付款页
- **其他 FTL 页面不单独改** — 通用组件类自动继承

工作量从 50+ 页下降到 1 套 CSS + 5 个文件精修。

## 2. 设计 Tokens

### 配色 (CSS variables)

```css
--cafe-bg:         #FAF7F2;  /* 奶油白背景 */
--cafe-card:       #FFFFFF;  /* 卡片底 */
--cafe-primary:    #6B4226;  /* 深咖啡棕，主按钮/链接 */
--cafe-primary-d:  #4A2D18;  /* hover/按下 */
--cafe-accent:     #C9A074;  /* 焦糖金，标签/点缀 */
--cafe-text:       #2B2018;  /* 正文 */
--cafe-text-2:     #8B7B6E;  /* 辅助文字 */
--cafe-line:       #EDE5DA;  /* 分割线/边框 */
--cafe-success:    #4A7C59;  /* 墨绿 */
--cafe-warn:       #C97B3F;  /* 焦橙 */
--cafe-danger:     #B8453A;  /* 砖红 */
```

主色 `#6B4226` 与现有 `shop_config.themeColor` 默认值一致，无需迁移。

### 字体

```css
--cafe-font-serif: "Noto Serif SC", "Source Han Serif SC", "Songti SC", serif;
--cafe-font-sans:  "Inter", -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif;
```

- 标题 (h1/h2/h3 + `.page-title`) 用衬线
- 正文/按钮/表单用无衬线
- 中文衬线字体走系统兜底，不在线加载（避免国内访问慢）

### 间距 / 圆角 / 阴影

```css
--cafe-r-sm: 6px;
--cafe-r-md: 8px;   /* 按钮 */
--cafe-r-lg: 12px;  /* 卡片 */
--cafe-r-xl: 16px;  /* Modal */

--cafe-shadow-1: 0 2px 8px rgba(43,32,24,.06);   /* 静态卡片 */
--cafe-shadow-2: 0 8px 24px rgba(43,32,24,.12);  /* 悬浮/Modal */
```

间距尺度: 4 / 8 / 12 / 16 / 24 / 32 / 48 px。

## 3. 通用组件类

写在 `cafe-theme.css`，所有 FTL 页面自动可用，无需逐页修改：

| 类名 | 替换/对应 | 用途 |
|---|---|---|
| `.btn-cafe` | `btn btn-primary` | 实心咖啡棕按钮 |
| `.btn-cafe-ghost` | `btn btn-default` | 描边按钮 |
| `.btn-cafe-danger` | `btn btn-danger` | 砖红按钮 |
| `.card-cafe` | 自定义 panel | 白底 12px 圆角 + shadow-1 |
| `.table-cafe` | `table` | 去粗边、行高 48px、表头浅色底 |
| `.form-cafe` 内 input/select | `form-control` | 1px 浅边，focus 变主色 |
| `.badge-cafe / -success / -warn / -danger` | `label label-*` | 状态标签 |
| `.page-title` | h1/h2 | 衬线 + 焦糖金 24px 短横线 |

**Bootstrap 兼容策略**: 现有 Bootstrap 类不删除。`cafe-theme.css` 通过更具体的选择器（如 `.btn.btn-primary`、`.table > thead > tr > th`、`.panel.panel-default`）覆盖核心元素的视觉。`cafe-theme.css` 在 `header.ftl` 中**最后引入**，自然胜出 Bootstrap。这样所有未精修页面也能自动焕新。

## 4. 各页面应用

### `nav.ftl`
- 侧栏背景: `--cafe-bg` 奶油底
- Logo 位用衬线大字
- 分组标题深棕 + 字距加宽
- 选中项: 焦糖金 3px 左边框 + 浅色背景
- 折叠展开样式保留现有逻辑

### `dashboard/index.ftl`
- 顶部欢迎条: `「{店名}」`衬线大标题 + 今日日期/星期
- 数据 4 大卡片: 营业额 / 订单数 / 客单价 / 在售商品 (`card-cafe` + 大数字衬线 + 焦糖金图标)
- WMS widget 三块 (alerts / overview / shipments) 套用 `card-cafe`

### `cashier/desk.ftl`
- 三栏: 左 `分类` (竖直 tab) | 中 `商品网格` (卡片栅格) | 右 `购物车` (固定面板)
- 商品卡: 大图 + 价格 + 加号按钮
- 购物车: 顶部桌台号 + 商品列表 + 底部金额 + 大「结账」按钮
- 整体 `--cafe-bg` 底，三栏白底圆角

### `h5/index.html`
- 顶部门店头图 (3:1 banner) + 店名衬线
- 分类横滚 tab (sticky)
- 商品双列卡片大图
- 底部购物车浮条 (FAB 风) + 数量徽章
- 全宽贴边设计，移动端友好
- 触发改 `static/h5/css/index.css` (核心)、可能微调 `static/h5/index.html` 模板结构

### `pay/create.ftl`
- 居中卡片 (max-width 480, mx-auto)
- 顶部店名 + 订单号
- 大金额数字 (衬线 60px + `--cafe-primary`)
- 商品摘要列表
- 底部「微信支付」「支付宝」大按钮 (各自品牌色但保持圆角风格)

## 5. 实施顺序

每步完成后停下等用户反馈（per-feature 暂停约定）:

1. **基础层**: `cafe-theme.css` + `header.ftl` 引入 + `nav.ftl` 焕新
2. **dashboard 精修**
3. **cashier 精修**
4. **H5 精修**
5. **pay 精修**

每步完成后跑一次 dev server 自检，截图或描述效果给用户确认。

## 6. 范围外（明确不做）

- **不动**业务逻辑、controller、JPA entity
- **不删**现有 Bootstrap 依赖（兼容存量页面）
- **不改**字体在线加载（用系统字体兜底，避免国内访问问题）
- **不做**深色模式（保留为后续可选）
- **不改** WMS RuoYi 端样式（两个独立系统）
- **不在 UI 出现**「PRD/规格/spec」字样（per 用户偏好）

## 7. 验收

- 三端打开页面应有明显「咖啡馆质感」视觉差异
- 未精修的存量页面（如 product/list、member/list）应自动有新按钮、表格、表单样式
- 移动端 H5 在 iPhone Safari 上滚动顺畅、按钮可点
- 现有功能 0 回归（不改业务代码理论上保证）
