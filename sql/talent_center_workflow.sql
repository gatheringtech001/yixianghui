-- 用户已批准：新增真实负责人账号绑定与身份审批流程。先完整备份。
-- 不回填历史姓名，不改订单金额、履约状态或退款结果。
ALTER TABLE app_consultant ENGINE=InnoDB;
ALTER TABLE app_goods_order
  ADD COLUMN service_owner_user_id bigint NULL COMMENT '明确绑定的后台负责人账号ID',
  ADD KEY idx_order_service_owner (service_owner_user_id, travel_status, order_id);
ALTER TABLE talent_center_admin_actor
  ADD COLUMN consultant_id bigint NULL COMMENT '管理员明确绑定的管家档案',
  ADD UNIQUE KEY uk_talent_bound_consultant (consultant_id);

CREATE TABLE talent_center_workflow_audit (
  operation_id varchar(36) NOT NULL,
  actor_id varchar(128) NOT NULL,
  action_kind varchar(32) NOT NULL,
  input_hash char(64) NOT NULL,
  input_json json NOT NULL,
  changes_json json NULL,
  result_json text NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (operation_id),
  KEY idx_talent_workflow_actor (actor_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='身份审批、绑定及负责人分配幂等审计';
