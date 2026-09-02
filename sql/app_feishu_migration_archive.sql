-- 飞书一次性迁移落库层。保留原始表、字段、记录和稳定 record_id，避免覆盖正式业务表。
CREATE TABLE IF NOT EXISTS `app_feishu_migration_table` (
  `migration_table_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `base_key` varchar(32) NOT NULL,
  `source_table_id` varchar(64) NOT NULL,
  `source_table_name` varchar(128) NOT NULL,
  `source_revision` bigint DEFAULT NULL,
  `field_count` int unsigned NOT NULL DEFAULT 0,
  `record_count` int unsigned NOT NULL DEFAULT 0,
  `imported_at` datetime NOT NULL,
  PRIMARY KEY (`migration_table_id`),
  UNIQUE KEY `uk_feishu_migration_table_source` (`source_table_id`),
  KEY `idx_feishu_migration_table_base` (`base_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='飞书迁移表目录';

CREATE TABLE IF NOT EXISTS `app_feishu_migration_field` (
  `migration_field_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `source_table_id` varchar(64) NOT NULL,
  `source_field_id` varchar(64) NOT NULL,
  `source_field_name` varchar(255) NOT NULL,
  `source_field_type` int NOT NULL,
  `source_ui_type` varchar(64) DEFAULT NULL,
  `is_primary` tinyint(1) NOT NULL DEFAULT 0,
  `property_json` json DEFAULT NULL,
  `imported_at` datetime NOT NULL,
  PRIMARY KEY (`migration_field_id`),
  UNIQUE KEY `uk_feishu_migration_field_source` (`source_table_id`,`source_field_id`),
  KEY `idx_feishu_migration_field_table` (`source_table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='飞书迁移字段目录';

CREATE TABLE IF NOT EXISTS `app_feishu_migration_record` (
  `migration_record_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `source_table_id` varchar(64) NOT NULL,
  `source_record_id` varchar(64) NOT NULL,
  `fields_json` json NOT NULL,
  `record_hash` char(64) NOT NULL,
  `merge_status` varchar(16) NOT NULL DEFAULT 'pending',
  `target_table` varchar(64) DEFAULT NULL,
  `target_id` bigint unsigned DEFAULT NULL,
  `merge_message` varchar(500) DEFAULT NULL,
  `imported_at` datetime NOT NULL,
  PRIMARY KEY (`migration_record_id`),
  UNIQUE KEY `uk_feishu_migration_record_source` (`source_table_id`,`source_record_id`),
  KEY `idx_feishu_migration_record_table` (`source_table_id`),
  KEY `idx_feishu_migration_record_merge` (`merge_status`,`source_table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='飞书迁移原始记录';
