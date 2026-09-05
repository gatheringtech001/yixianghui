-- Production schema audit: 2026-09-05. MyISAM cannot roll back these writes.
-- Apply only in a maintenance window after a verified FULL backup, with app
-- writes and scheduled jobs paused. ALTER TABLE implicitly commits; this file
-- is NOT an atomic migration. Check disk capacity and run each statement once.
-- The remaining statements can be resumed after a failure by checking ENGINE.
ALTER TABLE app_goods ENGINE=InnoDB;
ALTER TABLE app_goods_order_detail ENGINE=InnoDB;
ALTER TABLE app_goods_order_after ENGINE=InnoDB;
ALTER TABLE app_activity ENGINE=InnoDB;
ALTER TABLE app_activity_order ENGINE=InnoDB;
ALTER TABLE app_pay_log ENGINE=InnoDB;
ALTER TABLE app_pay_refund_log ENGINE=InnoDB;
ALTER TABLE app_user_card ENGINE=InnoDB;
ALTER TABLE app_user_gold_log ENGINE=InnoDB;
-- HMAC status changes and their audit row must share transaction semantics.
ALTER TABLE app_article ENGINE=InnoDB;
ALTER TABLE app_ad_content ENGINE=InnoDB;

SELECT table_name, engine FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name IN (
    'app_goods', 'app_goods_order', 'app_goods_order_detail', 'app_goods_order_after',
    'app_goods_coupon', 'app_goods_coupon_got', 'app_activity', 'app_activity_order',
    'app_pay_log', 'app_pay_refund_log', 'app_user_card', 'app_user_gold_log',
    'app_user_info', 'app_article', 'app_ad_content', 'talent_center_admin_audit'
) ORDER BY table_name;
