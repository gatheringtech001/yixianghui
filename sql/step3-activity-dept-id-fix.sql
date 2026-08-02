-- ============================================================
-- Fix: add dept_id to app_activity
-- Error without this column: Unknown column 'dept_id' in 'field list'
--
-- SQLyog tips:
-- 1. Run ONE statement at a time (select line -> Execute)
-- 2. Skip if you see: Duplicate column name 'dept_id'
-- ============================================================

ALTER TABLE app_activity
    ADD COLUMN dept_id BIGINT DEFAULT NULL;
