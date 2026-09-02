ALTER TABLE `app_goods_order`
  ADD COLUMN `order_origin` varchar(20) NOT NULL DEFAULT 'mini_program' COMMENT '订单来源 mini_program/feishu_history' AFTER `order_no`,
  ADD COLUMN `feishu_record_id` varchar(64) DEFAULT NULL COMMENT '飞书记录ID' AFTER `order_origin`,
  ADD COLUMN `feishu_order_no` varchar(50) DEFAULT NULL COMMENT '飞书订单编号' AFTER `feishu_record_id`,
  ADD COLUMN `channel` varchar(50) DEFAULT NULL COMMENT '订单渠道' AFTER `feishu_order_no`,
  ADD COLUMN `travel_customer_record_id` varchar(64) DEFAULT NULL COMMENT '飞书客户记录ID' AFTER `channel`,
  ADD COLUMN `travel_base_record_id` varchar(64) DEFAULT NULL COMMENT '飞书基地记录ID' AFTER `travel_customer_record_id`,
  ADD COLUMN `travel_base_name` varchar(128) DEFAULT NULL COMMENT '旅居基地名称' AFTER `travel_base_record_id`,
  ADD COLUMN `room_type` varchar(255) DEFAULT NULL COMMENT '房型' AFTER `travel_base_name`,
  ADD COLUMN `room_count` int unsigned DEFAULT NULL COMMENT '房间数' AFTER `room_type`,
  ADD COLUMN `traveler_count` int unsigned DEFAULT NULL COMMENT '同行人数' AFTER `room_count`,
  ADD COLUMN `service_owner` varchar(100) DEFAULT NULL COMMENT '客服负责人' AFTER `traveler_count`,
  ADD COLUMN `service_remark` varchar(500) DEFAULT NULL COMMENT '服务备注' AFTER `service_owner`,
  ADD COLUMN `source_fields_json` json DEFAULT NULL COMMENT '飞书订单源字段快照' AFTER `service_remark`,
  ADD UNIQUE KEY `uk_app_goods_order_feishu_record` (`feishu_record_id`),
  ADD KEY `idx_app_goods_order_origin` (`order_origin`),
  ADD KEY `idx_app_goods_order_channel` (`channel`);

UPDATE `app_goods_order`
SET `order_origin` = 'mini_program'
WHERE `order_origin` IS NULL;
