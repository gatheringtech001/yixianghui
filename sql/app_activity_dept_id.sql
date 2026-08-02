-- Plan B: 活动按站点/城市筛选（小程序聚会活动 Tab 使用）
-- 在 PC 端连接的数据库中执行本脚本

ALTER TABLE app_activity
    ADD COLUMN dept_id bigint NULL COMMENT '归属站点(sys_dept.dept_id)' AFTER category_id;
