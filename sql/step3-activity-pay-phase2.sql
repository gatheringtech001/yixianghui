-- ============================================================
-- Activity pay phase 2: add payment columns to app_activity_order
--
-- SQLyog tips:
-- 1. Run ONE statement at a time (select line -> Execute)
-- 2. Skip any line that returns: Duplicate column name
-- 3. Run the UPDATE only after ALL 6 ALTER succeed
-- ============================================================

ALTER TABLE app_activity_order
    ADD COLUMN order_no VARCHAR(64) DEFAULT NULL;

ALTER TABLE app_activity_order
    ADD COLUMN money_payable DECIMAL(10,2) DEFAULT 0;

ALTER TABLE app_activity_order
    ADD COLUMN pay_status CHAR(1) DEFAULT '1';

ALTER TABLE app_activity_order
    ADD COLUMN pay_money DECIMAL(10,2) DEFAULT 0;

ALTER TABLE app_activity_order
    ADD COLUMN pay_type VARCHAR(32) DEFAULT NULL;

ALTER TABLE app_activity_order
    ADD COLUMN pay_time DATETIME DEFAULT NULL;

UPDATE app_activity_order
SET pay_status = '1', money_payable = 0, pay_money = 0
WHERE pay_status IS NULL;
