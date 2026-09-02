-- 旅居订单履约状态。支付/退款状态继续使用 status/pay_status，避免影响非旅居订单。
SET @migration_sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'app_goods_order'
          AND column_name = 'travel_status'
    ),
    'DO 0',
    'ALTER TABLE `app_goods_order` ADD COLUMN `travel_status` varchar(1) DEFAULT NULL COMMENT ''旅居状态:0待确认,1已确认,2已取消,3已入住,4已离店,5已结算,6退款中,7已退款'' AFTER `status`'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @migration_sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'app_goods_order'
          AND column_name = 'travel_status_before_refund'
    ),
    'DO 0',
    'ALTER TABLE `app_goods_order` ADD COLUMN `travel_status_before_refund` varchar(1) DEFAULT NULL COMMENT ''退款前旅居状态'' AFTER `travel_status`'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

-- 仅回填旅居商品历史订单；养老、教育、活动等订单保持 NULL。
UPDATE app_goods_order orders
INNER JOIN app_goods goods ON goods.goods_id = orders.goods_id
SET orders.travel_status = CASE orders.status
    WHEN '0' THEN '0'
    WHEN '1' THEN '1'
    WHEN '2' THEN '2'
    WHEN '3' THEN '6'
    WHEN '4' THEN '7'
    ELSE NULL
END
WHERE goods.goods_type = 'hotel'
  AND orders.travel_status IS NULL;
