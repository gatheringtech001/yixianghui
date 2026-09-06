-- Only for an empty, isolated yxh_workflow_test_* database. No production records.
CREATE TABLE sys_user (user_id bigint PRIMARY KEY, nick_name varchar(30), status char(1) DEFAULT '0', del_flag char(1) DEFAULT '0') ENGINE=InnoDB;
CREATE TABLE talent_center_admin_actor (
  actor_id varchar(128) PRIMARY KEY, user_id bigint NOT NULL UNIQUE, display_name varchar(64),
  status char(1) DEFAULT '0', update_time datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
CREATE TABLE app_consultant (
  consultant_id bigint PRIMARY KEY AUTO_INCREMENT, consultant_no varchar(50), consultant_name varchar(100),
  mobile varchar(20), dept_id bigint, status char(2), remark varchar(255), user_id bigint,
  create_time datetime, update_time datetime
) ENGINE=MyISAM;
CREATE TABLE app_goods (goods_id bigint PRIMARY KEY, goods_name varchar(255), goods_type varchar(50)) ENGINE=InnoDB;
CREATE TABLE app_goods_order (
  order_id bigint PRIMARY KEY, order_no varchar(50), goods_id bigint, service_owner varchar(100),
  travel_status char(1), pay_status char(1), order_origin varchar(50), travel_base_name varchar(255),
  feishu_order_no varchar(50), contact_name varchar(30), check_in_date date, check_out_date date,
  money_payable decimal(10,2), pay_money decimal(10,2), channel varchar(50), status char(1), user_id bigint, update_time datetime
) ENGINE=InnoDB;
CREATE TABLE app_user_inviter (user_id bigint,new_user_id bigint,status char(1)) ENGINE=InnoDB;
CREATE TABLE app_goods_order_after (
  after_id bigint PRIMARY KEY, order_id bigint, goods_id bigint, out_order_no varchar(50), status char(1),
  app_refund_money decimal(10,2), refund_money decimal(10,2), reason_description varchar(255), remark varchar(255)
) ENGINE=InnoDB;
CREATE TABLE app_customer (
  customer_id bigint PRIMARY KEY, customer_name varchar(30), consultant_id bigint, return_visit tinyint, del_flag char(1)
) ENGINE=InnoDB;
