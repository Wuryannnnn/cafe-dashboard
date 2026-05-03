-- 类目和商品在 seed_meituan_products.sql (autogen). data-local.sql 只剩公共 seed.

-- 加料
INSERT INTO product_addon (addon_id, addon_name, addon_price, category_type) VALUES ('addon001', '加浓', 3.00, null);
INSERT INTO product_addon (addon_id, addon_name, addon_price, category_type) VALUES ('addon002', '换燕麦奶', 5.00, null);
INSERT INTO product_addon (addon_id, addon_name, addon_price, category_type) VALUES ('addon003', '加香草糖浆', 3.00, null);
INSERT INTO product_addon (addon_id, addon_name, addon_price, category_type) VALUES ('addon004', '加焦糖糖浆', 3.00, null);
INSERT INTO product_addon (addon_id, addon_name, addon_price, category_type) VALUES ('addon005', '少糖', 0.00, null);

-- 卖家
INSERT INTO seller_info (seller_id, username, password, openid) VALUES ('seller01', 'admin', 'admin', 'test_openid');

-- 员工
INSERT INTO staff (staff_id, username, password, name, phone, role, enabled) VALUES (1, 'boss', 'boss123', '张老板', '13800000001', 0, true);
INSERT INTO staff (staff_id, username, password, name, phone, role, enabled) VALUES (2, 'manager', 'mgr123', '王店长', '13800000002', 1, true);
INSERT INTO staff (staff_id, username, password, name, phone, role, enabled) VALUES (3, 'cashier1', 'cs123', '李收银', '13800000003', 2, true);
INSERT INTO staff (staff_id, username, password, name, phone, role, enabled) VALUES (4, 'maker1', 'mk123', '小制作', '13800000004', 3, true);

-- 店铺配置
INSERT INTO shop_config (config_key, config_value) VALUES ('shopName', 'The Infinite Cafe');
INSERT INTO shop_config (config_key, config_value) VALUES ('shopLogo', '/sell/images/logo.jpg');
INSERT INTO shop_config (config_key, config_value) VALUES ('announcement', '');
INSERT INTO shop_config (config_key, config_value) VALUES ('themeColor', '#6b4226');
INSERT INTO shop_config (config_key, config_value) VALUES ('banners', '[]');
INSERT INTO shop_config (config_key, config_value) VALUES ('shop.base.url', 'https://insulin-centres-genetics-elite.trycloudflare.com');

-- WMS 整合
INSERT INTO shop_config (config_key, config_value) VALUES ('wms.baseUrl', 'http://localhost:8081');
INSERT INTO shop_config (config_key, config_value) VALUES ('wms.username', 'admin');
INSERT INTO shop_config (config_key, config_value) VALUES ('wms.password', 'admin123');
INSERT INTO shop_config (config_key, config_value) VALUES ('wms.token', '');
INSERT INTO shop_config (config_key, config_value) VALUES ('wms.defaultWarehouseId', '');
INSERT INTO shop_config (config_key, config_value) VALUES ('wms.preCheckStock', 'false');

-- 门店信息
INSERT INTO shop_config (config_key, config_value) VALUES ('store.name', 'The Infinite Cafe');
INSERT INTO shop_config (config_key, config_value) VALUES ('store.address', '示例地址 1 号');
INSERT INTO shop_config (config_key, config_value) VALUES ('store.phone', '400-000-0000');
INSERT INTO shop_config (config_key, config_value) VALUES ('store.openHours', '周一至周日 08:00 - 22:00');
INSERT INTO shop_config (config_key, config_value) VALUES ('store.logo', '/sell/images/logo.jpg');

-- 业务开关
INSERT INTO shop_config (config_key, config_value) VALUES ('switch.qrOrder', 'true');
INSERT INTO shop_config (config_key, config_value) VALUES ('switch.autoAcceptOrder', 'true');
INSERT INTO shop_config (config_key, config_value) VALUES ('switch.memberRegister', 'true');
INSERT INTO shop_config (config_key, config_value) VALUES ('switch.payBeforeServe', 'true');

-- 常用备注
INSERT INTO common_remark (remark_id, text, sort_order) VALUES (1, '少冰', 0);
INSERT INTO common_remark (remark_id, text, sort_order) VALUES (2, '多冰', 1);
INSERT INTO common_remark (remark_id, text, sort_order) VALUES (3, '去冰', 2);
INSERT INTO common_remark (remark_id, text, sort_order) VALUES (4, '加浓', 3);
INSERT INTO common_remark (remark_id, text, sort_order) VALUES (5, '打包', 4);

-- 桌台区域
INSERT INTO restaurant_area (area_id, area_name, sort_order) VALUES (1, '吧台区', 0);
INSERT INTO restaurant_area (area_id, area_name, sort_order) VALUES (2, '大厅A区', 1);
INSERT INTO restaurant_area (area_id, area_name, sort_order) VALUES (3, '户外区', 2);
INSERT INTO restaurant_area (area_id, area_name, sort_order) VALUES (4, 'VIP区', 3);

-- 会员等级
INSERT INTO member_level (level_id, level_name, upgrade_amount, upgrade_count, discount_rate, sort_order) VALUES (1, '普通会员', 0, 0, 100, 0);
INSERT INTO member_level (level_id, level_name, upgrade_amount, upgrade_count, discount_rate, sort_order) VALUES (2, '银卡', 500, NULL, 95, 1);
INSERT INTO member_level (level_id, level_name, upgrade_amount, upgrade_count, discount_rate, sort_order) VALUES (3, '金卡', 2000, NULL, 90, 2);
INSERT INTO member_level (level_id, level_name, upgrade_amount, upgrade_count, discount_rate, sort_order) VALUES (4, '黑金', 5000, NULL, 85, 3);

-- 充值方案
INSERT INTO recharge_plan (plan_id, plan_name, pay_amount, give_amount, enabled, sort_order) VALUES (1, '充200送30', 200, 30, true, 0);
INSERT INTO recharge_plan (plan_id, plan_name, pay_amount, give_amount, enabled, sort_order) VALUES (2, '充500送100', 500, 100, true, 1);
INSERT INTO recharge_plan (plan_id, plan_name, pay_amount, give_amount, enabled, sort_order) VALUES (3, '充1000送300', 1000, 300, true, 2);

-- 结账方式
INSERT INTO payment_method (method_id, method_name, method_code, enabled, is_default, sort_order) VALUES (1, '微信', 'wechat', true, true, 0);
INSERT INTO payment_method (method_id, method_name, method_code, enabled, is_default, sort_order) VALUES (2, '支付宝', 'alipay', true, false, 1);
INSERT INTO payment_method (method_id, method_name, method_code, enabled, is_default, sort_order) VALUES (3, '现金', 'cash', true, false, 2);
INSERT INTO payment_method (method_id, method_name, method_code, enabled, is_default, sort_order) VALUES (4, '会员余额', 'balance', true, false, 3);

-- 桌台
INSERT INTO restaurant_table (table_id, area_id, table_code, seat_count, sort_order, enabled) VALUES (1, 1, 'B1', 2, 0, true);
INSERT INTO restaurant_table (table_id, area_id, table_code, seat_count, sort_order, enabled) VALUES (2, 1, 'B2', 2, 1, true);
INSERT INTO restaurant_table (table_id, area_id, table_code, seat_count, sort_order, enabled) VALUES (3, 2, 'A1', 4, 0, true);
INSERT INTO restaurant_table (table_id, area_id, table_code, seat_count, sort_order, enabled) VALUES (4, 2, 'A2', 4, 1, true);
INSERT INTO restaurant_table (table_id, area_id, table_code, seat_count, sort_order, enabled) VALUES (5, 2, 'A3', 4, 2, true);
INSERT INTO restaurant_table (table_id, area_id, table_code, seat_count, sort_order, enabled) VALUES (6, 3, 'O1', 6, 0, true);
INSERT INTO restaurant_table (table_id, area_id, table_code, seat_count, sort_order, enabled) VALUES (7, 4, 'V1', 8, 0, true);

-- 同步 H2 sequence 到种子数据之后（避免主键冲突）
ALTER SEQUENCE IF EXISTS common_remark_seq RESTART WITH 100;
ALTER SEQUENCE IF EXISTS member_level_seq RESTART WITH 100;
ALTER SEQUENCE IF EXISTS recharge_plan_seq RESTART WITH 100;
ALTER SEQUENCE IF EXISTS payment_method_seq RESTART WITH 100;
ALTER SEQUENCE IF EXISTS restaurant_area_seq RESTART WITH 100;
ALTER SEQUENCE IF EXISTS restaurant_table_seq RESTART WITH 100;
ALTER SEQUENCE IF EXISTS staff_seq RESTART WITH 100;
ALTER SEQUENCE IF EXISTS product_category_seq RESTART WITH 100;
ALTER SEQUENCE IF EXISTS expense_category_seq RESTART WITH 100;

-- ========== E2E 测试: 美式 BOM + 默认仓库 ==========
-- 美式 (mt004) 的 BOM 配方
INSERT INTO product_recipe (recipe_id, product_id, sku_id, wms_item_id, wms_item_name, wms_sku_id, quantity, unit) VALUES (1, 'mt004', NULL, 2045282416043921410, '咖啡豆-阿拉比卡', 2045282416052310018, 18.00, 'g');
INSERT INTO product_recipe (recipe_id, product_id, sku_id, wms_item_id, wms_item_name, wms_sku_id, quantity, unit) VALUES (2, 'mt004', NULL, 2045282417121857537, '纸杯-中杯', 2045282417126051841, 1.00, '个');
INSERT INTO product_recipe (recipe_id, product_id, sku_id, wms_item_id, wms_item_name, wms_sku_id, quantity, unit) VALUES (3, 'mt004', NULL, 2045282417226715137, '杯盖', 2045282417230909442, 1.00, '个');
ALTER SEQUENCE IF EXISTS product_recipe_seq RESTART WITH 100;

-- 配置默认仓库
UPDATE shop_config SET config_value='2045279165273772033' WHERE config_key='wms.defaultWarehouseId';
