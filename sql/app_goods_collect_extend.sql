-- Extend app_goods_collect for goods/activity collect (idempotent)
-- Safe to re-run: skips existing columns

SET @db := DATABASE();

-- collect_type
SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'app_goods_collect' AND COLUMN_NAME = 'collect_type'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE `app_goods_collect` ADD COLUMN `collect_type` VARCHAR(20) NOT NULL DEFAULT ''goods'' COMMENT ''collect type: goods/activity'' AFTER `user_id`',
  'SELECT ''skip collect_type'' AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- activity_id
SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'app_goods_collect' AND COLUMN_NAME = 'activity_id'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE `app_goods_collect` ADD COLUMN `activity_id` BIGINT(20) NULL DEFAULT NULL COMMENT ''activity id'' AFTER `goods_id`',
  'SELECT ''skip activity_id'' AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- goods_id nullable
ALTER TABLE `app_goods_collect`
  MODIFY COLUMN `goods_id` BIGINT(20) UNSIGNED NULL DEFAULT NULL COMMENT 'goods id';

UPDATE `app_goods_collect`
SET `collect_type` = 'goods'
WHERE `collect_type` IS NULL OR `collect_type` = '';

UPDATE `app_goods_collect`
SET `goods_id` = NULL
WHERE `collect_type` = 'activity';
