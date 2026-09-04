-- 渠道优惠券与订单归因。执行前必须先备份生产库。
-- 三张现有表为 MyISAM；领券、下单和核销必须具备真实事务与行锁。
ALTER TABLE app_goods_coupon ENGINE=InnoDB;
ALTER TABLE app_goods_coupon_got ENGINE=InnoDB;
ALTER TABLE app_goods_order ENGINE=InnoDB;

ALTER TABLE app_goods_coupon
    ADD COLUMN channel_code varchar(64) NULL COMMENT '渠道专属码' AFTER get_method,
    ADD COLUMN source_app_id varchar(64) NULL COMMENT '来源小程序AppID' AFTER channel_code,
    ADD COLUMN popup_title varchar(100) NULL COMMENT '进场弹窗标题' AFTER source_app_id,
    ADD UNIQUE KEY uk_coupon_channel_code (channel_code);

ALTER TABLE app_goods_coupon_got
    ADD COLUMN channel_code varchar(64) NULL COMMENT '领取渠道' AFTER get_method,
    ADD UNIQUE KEY uk_coupon_user_channel (coupon_id, user_id, channel_code);

ALTER TABLE app_goods_order
    ADD COLUMN distribution_channel_code varchar(64) NULL COMMENT '分销渠道码' AFTER coupon_got_ids,
    ADD KEY idx_order_distribution_channel (distribution_channel_code);

CREATE TABLE app_distribution_visit (
    visit_id bigint unsigned NOT NULL AUTO_INCREMENT,
    channel_code varchar(64) NOT NULL,
    user_id bigint NOT NULL,
    source_app_id varchar(64) NULL,
    launch_scene varchar(32) NULL,
    create_time datetime NOT NULL,
    PRIMARY KEY (visit_id),
    KEY idx_distribution_visit_channel_time (channel_code, create_time),
    KEY idx_distribution_visit_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='渠道访问记录';
