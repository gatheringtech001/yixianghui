-- ============================================================
-- Activity pay phase 1: add fee columns to app_activity
--
-- SQLyog tips:
-- 1. Run ONE statement at a time (select line -> Execute)
-- 2. Skip any line that returns: Duplicate column name
-- 3. Run UPDATE only after the 3 ALTER succeed
-- ============================================================

ALTER TABLE app_activity
    ADD COLUMN is_free TINYINT NOT NULL DEFAULT 1;

ALTER TABLE app_activity
    ADD COLUMN price DECIMAL(10,2) DEFAULT 0;

ALTER TABLE app_activity
    ADD COLUMN vip_price DECIMAL(10,2) DEFAULT 0;

UPDATE app_activity SET is_free = 1 WHERE is_free IS NULL;
