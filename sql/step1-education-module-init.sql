-- ============================================================
-- 老年教育模块 - 第 1 步：后台数据基础（可重复执行）
-- 说明：仅新增字典 + 分类树，不涉及小程序代码
-- 执行前请备份数据库
-- ============================================================

-- ------------------------------------------------------------
-- 1. 商品类型字典：新增 education（老年教育）
-- ------------------------------------------------------------
INSERT INTO sys_dict_data (
    dict_sort, dict_label, dict_value, dict_type,
    css_class, list_class, is_default, status,
    create_by, create_time, remark
)
SELECT
    5, '老年教育', 'education', 'goods_type',
    '', 'default', 'N', '0',
    'admin', NOW(), '老年教育课程商品'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data
    WHERE dict_type = 'goods_type' AND dict_value = 'education'
);

-- ------------------------------------------------------------
-- 2. 顶级分类：老年教育
-- ------------------------------------------------------------
INSERT INTO app_goods_category (
    parent_id, parent_ids, category_name, category_icon,
    is_hot, link_type, link_id, order_num, status
)
SELECT
    0, '0', '老年教育', '',
    0, 'goods', 0, 3, '1'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM app_goods_category
    WHERE parent_id = 0 AND category_name = '老年教育'
);

-- ------------------------------------------------------------
-- 3. 子分类：健康 / 手机 / 兴趣
-- ------------------------------------------------------------
SET @edu_parent_id = (
    SELECT category_id FROM app_goods_category
    WHERE parent_id = 0 AND category_name = '老年教育'
    LIMIT 1
);

INSERT INTO app_goods_category (
    parent_id, parent_ids, category_name, category_icon,
    is_hot, link_type, link_id, order_num, status
)
SELECT @edu_parent_id, CONCAT('0,', @edu_parent_id), '健康', '', 0, 'goods', 0, 1, '1'
FROM DUAL
WHERE @edu_parent_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM app_goods_category
      WHERE parent_id = @edu_parent_id AND category_name = '健康'
  );

INSERT INTO app_goods_category (
    parent_id, parent_ids, category_name, category_icon,
    is_hot, link_type, link_id, order_num, status
)
SELECT @edu_parent_id, CONCAT('0,', @edu_parent_id), '手机', '', 0, 'goods', 0, 2, '1'
FROM DUAL
WHERE @edu_parent_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM app_goods_category
      WHERE parent_id = @edu_parent_id AND category_name = '手机'
  );

INSERT INTO app_goods_category (
    parent_id, parent_ids, category_name, category_icon,
    is_hot, link_type, link_id, order_num, status
)
SELECT @edu_parent_id, CONCAT('0,', @edu_parent_id), '兴趣', '', 0, 'goods', 0, 3, '1'
FROM DUAL
WHERE @edu_parent_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM app_goods_category
      WHERE parent_id = @edu_parent_id AND category_name = '兴趣'
  );

-- ------------------------------------------------------------
-- 4. 验证查询（执行后应能看到结果）
-- ------------------------------------------------------------
-- SELECT dict_label, dict_value FROM sys_dict_data WHERE dict_type = 'goods_type' ORDER BY dict_sort;
-- SELECT category_id, parent_id, category_name, link_type, order_num, status
-- FROM app_goods_category
-- WHERE category_name IN ('老年教育', '健康', '手机', '兴趣')
--    OR parent_id = (SELECT category_id FROM app_goods_category WHERE category_name = '老年教育' AND parent_id = 0 LIMIT 1)
-- ORDER BY parent_id, order_num;
