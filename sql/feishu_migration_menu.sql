-- 飞书迁移数据只读菜单。超级管理员自动拥有；其他角色按权限分配。
INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2290, '飞书迁移数据', 1, 9, 'feishu_migration', 'system/feishu_migration/index', NULL,
       'FeishuMigration', 1, 0, 'C', '0', '0', 'system:feishu_migration:list',
       'documentation', 'admin', NOW(), '飞书一次性迁移原始数据只读查询'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE perms = 'system:feishu_migration:list'
);
