-- 商品订单住宿/预约字段，与 AppGoodsOrderMapper 保持一致。
-- 每列独立判断，兼容全新、部分迁移和已完成迁移的数据库。
SET @migration_sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'app_goods_order'
          AND column_name = 'check_in_date'
    ),
    'DO 0',
    'ALTER TABLE `app_goods_order` ADD COLUMN `check_in_date` date DEFAULT NULL COMMENT ''预订开始日期'' AFTER `status`'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @migration_sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'app_goods_order'
          AND column_name = 'check_out_date'
    ),
    'DO 0',
    'ALTER TABLE `app_goods_order` ADD COLUMN `check_out_date` date DEFAULT NULL COMMENT ''预订结束日期'' AFTER `check_in_date`'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @migration_sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'app_goods_order'
          AND column_name = 'contact_name'
    ),
    'DO 0',
    'ALTER TABLE `app_goods_order` ADD COLUMN `contact_name` varchar(50) DEFAULT NULL COMMENT ''联系人姓名'' AFTER `check_out_date`'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @migration_sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'app_goods_order'
          AND column_name = 'contact_phone'
    ),
    'DO 0',
    'ALTER TABLE `app_goods_order` ADD COLUMN `contact_phone` varchar(20) DEFAULT NULL COMMENT ''联系人电话'' AFTER `contact_name`'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;
