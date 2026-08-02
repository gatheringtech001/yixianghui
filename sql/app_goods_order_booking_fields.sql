-- 商品订单表补充旅居/预定相关字段（与 AppGoodsOrderMapper 保持一致）
ALTER TABLE `app_goods_order`
    ADD COLUMN `check_in_date` date DEFAULT NULL COMMENT '预定开始日期' AFTER `status`,
    ADD COLUMN `check_out_date` date DEFAULT NULL COMMENT '预定结束日期' AFTER `check_in_date`,
    ADD COLUMN `contact_name` varchar(50) DEFAULT NULL COMMENT '联系人姓名' AFTER `check_out_date`,
    ADD COLUMN `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系人电话' AFTER `contact_name`;
