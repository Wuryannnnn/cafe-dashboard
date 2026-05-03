# 咖啡厅点餐系统数据库


```sql
-- 类目
create table `product_category` (
    `category_id` int not null auto_increment,
    `category_name` varchar(64) not null comment '类目名字',
    `category_type` int not null comment '类目编号',
    `print_station` tinyint(3) not null default '0' comment '出单工位: 0吧台 1后厨',
    `create_time` timestamp not null default current_timestamp comment '创建时间',
    `update_time` timestamp not null default current_timestamp on update current_timestamp comment '修改时间',
    primary key (`category_id`),
    UNIQUE KEY `uqe_category_type` (`category_type`)
);

-- 商品
create table `product_info` (
    `product_id` varchar(32) not null,
    `product_name` varchar(64) not null comment '商品名称',
    `product_price` decimal(8,2) not null comment '基础单价',
    `product_stock` int not null comment '库存',
    `product_description` varchar(64) comment '描述',
    `product_icon` varchar(512) comment '小图',
    `product_status` tinyint(3) DEFAULT '0' COMMENT '商品状态,0正常1下架',
    `category_type` int not null comment '类目编号',
    `create_time` timestamp not null default current_timestamp comment '创建时间',
    `update_time` timestamp not null default current_timestamp on update current_timestamp comment '修改时间',
    primary key (`product_id`)
);

-- 商品规格(大杯/中杯/小杯、冰/热等)
create table `product_sku` (
    `sku_id` varchar(32) not null,
    `product_id` varchar(32) not null comment '所属商品id',
    `sku_name` varchar(64) not null comment '规格名称, 如大杯/冰',
    `sku_price` decimal(8,2) not null comment '该规格价格',
    `sku_stock` int not null default 999 comment '库存',
    `create_time` timestamp not null default current_timestamp comment '创建时间',
    `update_time` timestamp not null default current_timestamp on update current_timestamp comment '修改时间',
    primary key (`sku_id`),
    key `idx_product_id` (`product_id`)
);

-- 加料选项(加浓、燕麦奶、糖浆等)
create table `product_addon` (
    `addon_id` varchar(32) not null,
    `addon_name` varchar(64) not null comment '加料名称',
    `addon_price` decimal(8,2) not null comment '加料价格',
    `category_type` int default null comment '适用类目编号, null表示所有类目',
    `create_time` timestamp not null default current_timestamp comment '创建时间',
    `update_time` timestamp not null default current_timestamp on update current_timestamp comment '修改时间',
    primary key (`addon_id`),
    key `idx_category_type` (`category_type`)
);

-- 订单
create table `order_master` (
    `order_id` varchar(32) not null,
    `buyer_name` varchar(32) not null comment '买家名字',
    `buyer_phone` varchar(32) not null comment '买家电话',
    `buyer_address` varchar(128) not null default '' comment '买家地址',
    `buyer_openid` varchar(64) not null comment '买家微信openid',
    `order_amount` decimal(8,2) not null comment '订单总金额',
    `order_status` tinyint(3) not null default '0' comment '订单状态: 0新订单 1制作中 2待取餐 3完结 4已取消',
    `pay_status` tinyint(3) not null default '0' comment '支付状态: 0未支付 1已支付',
    `pay_type` tinyint(3) not null default '0' comment '支付方式: 0微信 1支付宝',
    `dining_type` tinyint(3) not null default '0' comment '就餐方式: 0堂食 1外带',
    `table_number` varchar(16) default null comment '桌号(堂食时使用)',
    `pickup_number` varchar(16) default null comment '取餐号(每日流水号)',
    `order_remark` varchar(256) default null comment '订单备注',
    `create_time` timestamp not null default current_timestamp comment '创建时间',
    `update_time` timestamp not null default current_timestamp on update current_timestamp comment '修改时间',
    primary key (`order_id`),
    key `idx_buyer_openid` (`buyer_openid`)
);

-- 订单商品
create table `order_detail` (
    `detail_id` varchar(32) not null,
    `order_id` varchar(32) not null,
    `product_id` varchar(32) not null,
    `product_name` varchar(64) not null comment '商品名称',
    `product_price` decimal(8,2) not null comment '当前价格',
    `product_quantity` int not null comment '数量',
    `product_icon` varchar(512) comment '小图',
    `sku_id` varchar(32) default null comment '规格id',
    `sku_name` varchar(64) default null comment '规格名称',
    `addons` varchar(512) default null comment '加料信息JSON',
    `addon_fee` decimal(8,2) default null comment '加料总费用',
    `create_time` timestamp not null default current_timestamp comment '创建时间',
    `update_time` timestamp not null default current_timestamp on update current_timestamp comment '修改时间',
    primary key (`detail_id`),
    key `idx_order_id` (`order_id`)
);

-- 卖家信息
create table `seller_info` (
    `seller_id` varchar(32) not null,
    `username` varchar(32) not null,
    `password` varchar(32) not null,
    `openid` varchar(64) not null comment '微信openid',
    `create_time` timestamp not null default current_timestamp comment '创建时间',
    `update_time` timestamp not null default current_timestamp on update current_timestamp comment '修改时间',
    primary key (`seller_id`)
) comment '卖家信息表';

-- 打印机配置
create table `printer_config` (
    `printer_id` varchar(32) not null,
    `printer_name` varchar(64) not null comment '打印机名称',
    `station` tinyint(3) not null default '0' comment '工位: 0吧台 1后厨',
    `ip_address` varchar(64) not null comment '打印机IP地址',
    `port` int not null default 9100 comment '端口',
    `enabled` tinyint(1) not null default '1' comment '是否启用',
    `create_time` timestamp not null default current_timestamp comment '创建时间',
    `update_time` timestamp not null default current_timestamp on update current_timestamp comment '修改时间',
    primary key (`printer_id`)
) comment '打印机配置表';

```

#### 咖啡厅示例数据

```sql
-- 类目(print_station: 0=吧台, 1=后厨)
INSERT INTO `product_category` (`category_name`, `category_type`, `print_station`) VALUES ('经典咖啡', 1, 0);
INSERT INTO `product_category` (`category_name`, `category_type`, `print_station`) VALUES ('特调饮品', 2, 0);
INSERT INTO `product_category` (`category_name`, `category_type`, `print_station`) VALUES ('茶饮', 3, 0);
INSERT INTO `product_category` (`category_name`, `category_type`, `print_station`) VALUES ('轻食', 4, 1);

-- 商品(基础价格为中杯价格)
INSERT INTO `product_info` VALUES ('prod001', '美式咖啡', 22.00, 999, '经典美式, 浓缩咖啡+水', '/images/americano.jpg', 0, 1, now(), now());
INSERT INTO `product_info` VALUES ('prod002', '拿铁', 28.00, 999, '浓缩咖啡+牛奶', '/images/latte.jpg', 0, 1, now(), now());
INSERT INTO `product_info` VALUES ('prod003', '卡布奇诺', 28.00, 999, '浓缩咖啡+奶泡+牛奶', '/images/cappuccino.jpg', 0, 1, now(), now());
INSERT INTO `product_info` VALUES ('prod004', '摩卡', 32.00, 999, '浓缩咖啡+巧克力+牛奶', '/images/mocha.jpg', 0, 1, now(), now());

-- 规格
INSERT INTO `product_sku` VALUES ('sku001', 'prod001', '小杯/热', 18.00, 999, now(), now());
INSERT INTO `product_sku` VALUES ('sku002', 'prod001', '中杯/热', 22.00, 999, now(), now());
INSERT INTO `product_sku` VALUES ('sku003', 'prod001', '大杯/热', 26.00, 999, now(), now());
INSERT INTO `product_sku` VALUES ('sku004', 'prod001', '小杯/冰', 18.00, 999, now(), now());
INSERT INTO `product_sku` VALUES ('sku005', 'prod001', '中杯/冰', 22.00, 999, now(), now());
INSERT INTO `product_sku` VALUES ('sku006', 'prod001', '大杯/冰', 26.00, 999, now(), now());
INSERT INTO `product_sku` VALUES ('sku007', 'prod002', '小杯/热', 24.00, 999, now(), now());
INSERT INTO `product_sku` VALUES ('sku008', 'prod002', '中杯/热', 28.00, 999, now(), now());
INSERT INTO `product_sku` VALUES ('sku009', 'prod002', '大杯/热', 32.00, 999, now(), now());
INSERT INTO `product_sku` VALUES ('sku010', 'prod002', '小杯/冰', 24.00, 999, now(), now());
INSERT INTO `product_sku` VALUES ('sku011', 'prod002', '中杯/冰', 28.00, 999, now(), now());
INSERT INTO `product_sku` VALUES ('sku012', 'prod002', '大杯/冰', 32.00, 999, now(), now());

-- 加料(categoryType为null表示适用所有咖啡类目)
INSERT INTO `product_addon` VALUES ('addon001', '加浓', 3.00, null, now(), now());
INSERT INTO `product_addon` VALUES ('addon002', '换燕麦奶', 5.00, null, now(), now());
INSERT INTO `product_addon` VALUES ('addon003', '加香草糖浆', 3.00, null, now(), now());
INSERT INTO `product_addon` VALUES ('addon004', '加焦糖糖浆', 3.00, null, now(), now());
INSERT INTO `product_addon` VALUES ('addon005', '少糖', 0.00, null, now(), now());
INSERT INTO `product_addon` VALUES ('addon006', '加奶油顶', 4.00, 1, now(), now());

-- 轻食商品
INSERT INTO `product_info` VALUES ('prod005', '牛油果吐司', 32.00, 999, '新鲜牛油果+全麦吐司+溏心蛋', '/images/avocado-toast.jpg', 0, 4, now(), now());
INSERT INTO `product_info` VALUES ('prod006', '凯撒沙拉', 28.00, 999, '罗马生菜+培根+帕玛森芝士', '/images/caesar-salad.jpg', 0, 4, now(), now());
INSERT INTO `product_info` VALUES ('prod007', '三明治', 26.00, 999, '火腿芝士三明治', '/images/sandwich.jpg', 0, 4, now(), now());

-- 打印机(示例)
INSERT INTO `printer_config` VALUES ('printer001', '吧台打印机', 0, '192.168.1.100', 9100, 1, now(), now());
INSERT INTO `printer_config` VALUES ('printer002', '后厨打印机', 1, '192.168.1.101', 9100, 1, now(), now());
```
