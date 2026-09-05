-- Production: verified full backup + maintenance window BEFORE these ALTERs.
-- Existing order/coupon tables must already have distribution-coupon-channel.sql.
-- DDL is not transactional. The two new tables can be created repeatedly.
ALTER TABLE app_goods ENGINE=InnoDB;
ALTER TABLE app_goods_cart ENGINE=InnoDB;
ALTER TABLE app_goods_order_detail ENGINE=InnoDB;
ALTER TABLE app_goods_order_after ENGINE=InnoDB;
ALTER TABLE app_pay_log ENGINE=InnoDB;
ALTER TABLE app_pay_refund_log ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS app_supplier_goods (
  goods_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
  supplier_id BIGINT UNSIGNED NOT NULL,
  KEY idx_supplier (supplier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS app_supplier_order (
  fulfillment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT UNSIGNED NULL,
  user_id BIGINT NOT NULL,
  supplier_id BIGINT UNSIGNED NOT NULL,
  checkout_key VARCHAR(64) NOT NULL,
  request_hash VARCHAR(64) NOT NULL,
  address_snapshot TEXT NOT NULL,
  lines_snapshot MEDIUMTEXT NOT NULL,
  notice_status VARCHAR(16) NOT NULL DEFAULT 'pending',
  attempts INT NOT NULL DEFAULT 0,
  next_attempt DATETIME NULL,
  last_error VARCHAR(255) NULL,
  notified_at DATETIME NULL,
  confirmed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_checkout (user_id,checkout_key),
  UNIQUE KEY uq_order (order_id),
  KEY idx_dispatch (notice_status,next_attempt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO app_supplier(supplier_name,supplier_code,status)
SELECT '云野集','YUNYE','1' WHERE NOT EXISTS (SELECT 1 FROM app_supplier WHERE supplier_code='YUNYE' OR supplier_name='云野集');
INSERT INTO app_supplier_goods(goods_id,supplier_id)
SELECT g.goods_id,s.supplier_id FROM app_goods g
JOIN (SELECT MIN(supplier_id) supplier_id FROM app_supplier WHERE supplier_code='YUNYE' OR supplier_name='云野集') s
WHERE g.specifications LIKE 'YUNYE:%' AND s.supplier_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM app_supplier_goods b WHERE b.goods_id=g.goods_id);
-- Reuse the existing Quartz management UI. Notifications remain PAUSED until configured.
-- Server environment: SUPPLIER_NOTICE_ENABLED defaults to false.
-- Configure SUPPLIER_NOTICE_OPERATIONS_WEBHOOK for our INTERNAL operations group outside Git.
-- Operations manually forwards verified shipping lists to the supplier's ordinary WeChat group.
-- Enable the server switch and Quartz job only after the recipient group is verified.
INSERT INTO sys_job(job_name,job_group,invoke_target,cron_expression,misfire_policy,concurrent,status,create_by,create_time,remark)
SELECT '内部运营待发货通知','DEFAULT','supplierFulfillmentService.dispatchPending','0 0/30 * * * ?','3','1','1','admin',NOW(),'通知内部企业微信群，运营转发给供应商；启用前配置通知地址；默认暂停'
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target='supplierFulfillmentService.dispatchPending');
