-- ============================================================
-- 方案 C - 第 C-1 步：老年教育扩展表（可重复执行）
-- 说明：不修改 app_goods 原有结构，不影响全国旅居数据
-- 执行前请备份数据库
-- ============================================================

CREATE TABLE IF NOT EXISTS `app_goods_education_ext` (
  `ext_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '扩展ID',
  `goods_id` bigint unsigned NOT NULL COMMENT '商品ID，关联 app_goods.goods_id',
  `course_time` varchar(100) DEFAULT '' COMMENT '上课时间，如周一09:00-10:30',
  `course_place` varchar(255) DEFAULT '' COMMENT '授课地点',
  `teacher_name` varchar(100) DEFAULT '' COMMENT '授课老师',
  `lesson_count` int unsigned DEFAULT NULL COMMENT '课次，如10',
  `class_size_max` int unsigned DEFAULT NULL COMMENT '班级上限人数',
  `class_size_min` int unsigned DEFAULT NULL COMMENT '开班下限人数',
  `start_date` date DEFAULT NULL COMMENT '开课日期',
  `signup_start` date DEFAULT NULL COMMENT '报名开始日期',
  `signup_end` date DEFAULT NULL COMMENT '报名截止日期',
  `material_note` varchar(255) DEFAULT '' COMMENT '材料备注',
  `consult_phone` varchar(30) DEFAULT '' COMMENT '咨询电话',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ext_id`),
  UNIQUE KEY `uk_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老年教育商品扩展表';

-- ------------------------------------------------------------
-- 验证查询（执行后应能看到空表结构）
-- ------------------------------------------------------------
-- SHOW CREATE TABLE app_goods_education_ext;
-- SELECT COUNT(*) FROM app_goods_education_ext;
