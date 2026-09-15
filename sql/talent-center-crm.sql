-- CRM 原生记录，不覆盖客户原始备注、订单或消费数据。时间字段以 UTC 写入。
CREATE TABLE app_crm_event (
  event_id bigint unsigned NOT NULL AUTO_INCREMENT,
  request_id varchar(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  customer_id bigint unsigned NOT NULL,
  actor_user_id bigint NOT NULL,
  actor_name varchar(100) DEFAULT NULL,
  kind varchar(32) NOT NULL,
  content text NOT NULL,
  method varchar(16) DEFAULT NULL,
  occurred_at datetime(3) NOT NULL,
  created_at datetime(3) NOT NULL,
  payload_hash char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  payload_json text NOT NULL,
  PRIMARY KEY (event_id),
  UNIQUE KEY uk_crm_event_request (request_id),
  KEY idx_crm_event_customer (customer_id,event_id),
  CONSTRAINT fk_crm_event_customer FOREIGN KEY (customer_id) REFERENCES app_customer(customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE app_crm_task (
  task_id varchar(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  customer_id bigint unsigned NOT NULL,
  created_by bigint NOT NULL,
  title varchar(200) NOT NULL,
  due_at datetime(3) NOT NULL,
  status varchar(16) NOT NULL,
  version int unsigned NOT NULL DEFAULT 1,
  created_at datetime(3) NOT NULL,
  updated_at datetime(3) NOT NULL,
  PRIMARY KEY (task_id),
  KEY idx_crm_task_customer_due (customer_id,status,due_at),
  CONSTRAINT fk_crm_task_customer FOREIGN KEY (customer_id) REFERENCES app_customer(customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE app_crm_opportunity (
  opportunity_id varchar(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  customer_id bigint unsigned NOT NULL,
  created_by bigint NOT NULL,
  title varchar(200) NOT NULL,
  business_line varchar(16) NOT NULL,
  stage varchar(16) NOT NULL,
  version int unsigned NOT NULL DEFAULT 1,
  created_at datetime(3) NOT NULL,
  updated_at datetime(3) NOT NULL,
  PRIMARY KEY (opportunity_id),
  KEY idx_crm_opportunity_customer (customer_id,stage),
  CONSTRAINT fk_crm_opportunity_customer FOREIGN KEY (customer_id) REFERENCES app_customer(customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
