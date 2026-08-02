-- 商品分类(沿用）
`app_goods_category` (
  `category_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '分类id',
  `parent_id` bigint unsigned DEFAULT '0' COMMENT '所属上级',
  `parent_ids` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '祖级id集合',
  `category_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '分类名称',
  `category_icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '分类图标',
  `is_hot` smallint NOT NULL DEFAULT '0' COMMENT '是否热门',
  `link_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'goods' COMMENT '链接类型',
  `link_id` bigint DEFAULT '0' COMMENT '链接目标ID',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '备注',
  `order_num` int unsigned DEFAULT '1' COMMENT '排序顺序',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '分类状态',
  PRIMARY KEY (`category_id`) USING BTREE
) ENGINE=MyISAM AUTO_INCREMENT=54 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品分类';
-- 1. 商品表（修改）
CREATE TABLE `app_goods` (
                                  `goods_id` bigint(20) NOT NULL AUTO_INCREMENT,
                                  `goods_name` varchar(200) NOT NULL COMMENT '商品名称',
                                  `category_id` bigint(20) NOT NULL COMMENT '商品类型关联商品分类ID',
                                  `description` text COMMENT '商品描述',
                                  `goods_images` text COMMENT '商品图片(逗号分隔)',
                                  `tags` json DEFAULT NULL COMMENT '标签数组',
                                  `status` tinyint(1) DEFAULT '1' COMMENT '商品状态：0待上架，1已上架，2审核中，3拒绝上架，4特殊商品',
                                  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  PRIMARY KEY (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 2. 商品详情区块表（商品特色表，新增）
CREATE TABLE `app_goods_related` (
                                     `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                     `goods_id` bigint(20) NOT NULL COMMENT '商品ID',
                                     `section_id` varchar(100) NOT NULL COMMENT '区块ID',
                                     `section_name` varchar(100) NOT NULL COMMENT '区块名称',
                                     `content` text COMMENT '详情内容',
                                     `sort_order` int(11) DEFAULT '0' COMMENT '排序',
                                     `min_content_length` int(11) DEFAULT '500' COMMENT '展开阈值',
                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                     PRIMARY KEY (`id`),
                                     KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品详情区块表';

-- 3. 商品SKU表(商品套餐定义表，沿用)
CREATE TABLE `app_goods_sku` (
                                      `sku_id` bigint(20) NOT NULL AUTO_INCREMENT,
                                      `goods_id` bigint(20) NOT NULL COMMENT '商品ID',
                                      `sku_name` varchar(100) NOT NULL COMMENT 'SKU名称',
                                      --`days` int(11) NOT NULL COMMENT '入住天数',
                                      --`base_price` decimal(10,2) NOT NULL COMMENT '基础价格(不含餐)',
                                      `sort_order` int(11) DEFAULT '0' COMMENT '排序',
                                      `status` tinyint(1) DEFAULT '1' COMMENT '状态',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                      PRIMARY KEY (`sku_id`),
                                      KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU表';

-- 4. SKU餐饮组合表
CREATE TABLE `app_sku_meal_combo` (
                                      `combo_id` bigint(20) NOT NULL AUTO_INCREMENT,
                                      `sku_id` bigint(20) NOT NULL COMMENT 'SKU ID',
                                      `combo_type` tinyint(1) NOT NULL COMMENT '套餐类型 0:含早餐 1:一早一正 2:一日三餐',
                                      `combo_name` varchar(100) NOT NULL COMMENT '组合名称',
                                      `meal_price_per_day` decimal(10,2) NOT NULL COMMENT '每日餐费单价',
                                      `total_price` decimal(10,2) NOT NULL COMMENT '总价格',
                                      `average_price` decimal(10,2) NOT NULL COMMENT '人均价格',
                                      `include_breakfast` tinyint(1) DEFAULT '0' COMMENT '包含早餐',
                                      `include_lunch` tinyint(1) DEFAULT '0' COMMENT '包含午餐',
                                      `include_dinner` tinyint(1) DEFAULT '0' COMMENT '包含晚餐',
                                      `stock` int(11) DEFAULT '0' COMMENT '库存',
                                      `sort_order` int(11) DEFAULT '0' COMMENT '排序',
                                      `status` tinyint(1) DEFAULT '1' COMMENT '状态',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                      PRIMARY KEY (`combo_id`),
                                      UNIQUE KEY `uk_sku_combo` (`sku_id`, `combo_type`),
                                      KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU餐饮组合表';

-- 5. 旅居订单表（合并了详情表）
CREATE TABLE `app_sojourn_order` (
                                     `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                     `order_id` varchar(32) NOT NULL COMMENT '订单号',
                                     `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                     `goods_id` bigint(20) NOT NULL COMMENT '商品ID',
                                     `sku_id` bigint(20) NOT NULL COMMENT 'SKU ID',
                                     `combo_id` bigint(20) NOT NULL COMMENT '餐饮组合ID',

    -- 商品信息
                                     `goods_name` varchar(200) NOT NULL COMMENT '商品名称',
                                     `goods_image` varchar(500) DEFAULT '' COMMENT '商品主图',
                                     `sku_name` varchar(100) NOT NULL COMMENT 'SKU名称',
                                     `combo_name` varchar(100) NOT NULL COMMENT '套餐名称',

    -- 入住信息
                                     `check_in_date` date NOT NULL COMMENT '入住日期',
                                     `check_out_date` date NOT NULL COMMENT '退房日期',
                                     `days` int(11) NOT NULL COMMENT '入住天数',
                                     `room_number` int(11) NOT NULL COMMENT '房间数量',
                                     `people_number` int(11) NOT NULL COMMENT '用餐人数',

    -- 联系人信息
                                     `contact_name` varchar(50) NOT NULL COMMENT '联系人姓名',
                                     `contact_phone` varchar(20) NOT NULL COMMENT '联系人电话',

    -- 价格信息
                                     `room_unit_price` decimal(10,2) NOT NULL COMMENT '每人每晚房费单价',
                                     `room_total_price` decimal(10,2) NOT NULL COMMENT '房费总价',
                                     `meal_price_per_day` decimal(10,2) NOT NULL COMMENT '每日餐费单价',
                                     `meal_total_price` decimal(10,2) NOT NULL COMMENT '餐费总价',
                                     `total_amount` decimal(10,2) NOT NULL COMMENT '订单总金额',
                                     `pay_amount` decimal(10,2) NOT NULL COMMENT '实付金额',

    -- 餐饮份数
                                     `breakfast_count` int(11) NOT NULL COMMENT '早餐份数',
                                     `lunch_count` int(11) NOT NULL COMMENT '午餐份数',
                                     `dinner_count` int(11) NOT NULL COMMENT '晚餐份数',

    -- 订单状态
                                     `order_status` tinyint(1) DEFAULT '1' COMMENT '订单状态',
                                     `pay_status` tinyint(1) DEFAULT '0' COMMENT '支付状态',
                                     `pay_time` datetime DEFAULT NULL COMMENT '支付时间',

                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                     `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_order_id` (`order_id`),
                                     KEY `idx_user_id` (`user_id`),
                                     KEY `idx_goods_id` (`goods_id`),
                                     KEY `idx_order_status` (`order_status`),
                                     KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='旅居订单表';

-- 6. 订单操作日志表
CREATE TABLE `app_order_operation_log` (
                                           `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                           `order_id` varchar(32) NOT NULL COMMENT '订单号',
                                           `action` varchar(50) NOT NULL COMMENT '操作类型',
                                           `description` varchar(500) NOT NULL COMMENT '操作描述',
                                           `operator` varchar(100) NOT NULL COMMENT '操作人',
                                           `operator_type` varchar(20) NOT NULL COMMENT '操作人类型',
                                           `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                           PRIMARY KEY (`id`),
                                           KEY `idx_order_id` (`order_id`),
                                           KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单操作日志表';

-- 7. 简化后的支付订单表
CREATE TABLE `app_payment_order` (
                                     `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                     `order_id` varchar(32) NOT NULL COMMENT '业务订单号',
                                     `payment_no` varchar(32) NOT NULL COMMENT '支付单号',
                                     `pay_amount` decimal(10,2) NOT NULL COMMENT '实际支付金额',
                                     `payment_method` varchar(20) NOT NULL COMMENT '支付方式',
                                     `payment_status` tinyint(1) DEFAULT '0' COMMENT '支付状态',
                                     `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
                                     `payment_expire_time` datetime NOT NULL COMMENT '支付过期时间',
                                     `thirdparty_trade_no` varchar(64) DEFAULT '' COMMENT '第三方交易号',
                                     `payment_result` json DEFAULT NULL COMMENT '支付结果信息',
                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                     `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_payment_no` (`payment_no`),
                                     UNIQUE KEY `uk_order_id` (`order_id`),
                                     KEY `idx_payment_status` (`payment_status`),
                                     KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付订单表';

-- 8. 支付方式配置表
CREATE TABLE `app_payment_method` (
                                      `method_id` bigint(20) NOT NULL AUTO_INCREMENT,
                                      `method_code` varchar(20) NOT NULL COMMENT '支付方式代码',
                                      `method_name` varchar(50) NOT NULL COMMENT '支付方式名称',
                                      `icon` varchar(50) DEFAULT '' COMMENT '图标',
                                      `color` varchar(20) DEFAULT '' COMMENT '主题色',
                                      `status` tinyint(1) DEFAULT '1' COMMENT '状态',
                                      `sort_order` int(11) DEFAULT '0' COMMENT '排序',
                                      `description` varchar(200) DEFAULT '' COMMENT '描述',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                      PRIMARY KEY (`method_id`),
                                      UNIQUE KEY `uk_method_code` (`method_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付方式配置表';
