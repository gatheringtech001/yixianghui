-- 达人中心 HMAC 内部管理接口。生产执行前应先备份并核对重复 UnionID。
alter table sys_auth_user
    add column union_id varchar(128) null comment '微信开放平台UnionID' after source,
    add column app_id varchar(64) null comment '微信应用AppID' after union_id,
    add unique key uk_sys_auth_user_union_id (union_id);

create table talent_center_admin_audit (
    audit_id bigint not null auto_increment,
    service_id varchar(64) not null,
    actor_user_id bigint null,
    resource_type varchar(16) not null,
    resource_id bigint not null,
    before_status varchar(8) null,
    after_status varchar(8) null,
    confirmation_id varchar(128) not null,
    idempotency_key_hash char(64) not null,
    request_time datetime not null,
    result varchar(32) not null,
    ip varchar(128) null,
    create_time datetime not null,
    primary key (audit_id),
    unique key uk_talent_service_idempotency (service_id, idempotency_key_hash),
    unique key uk_talent_service_confirmation (service_id, confirmation_id),
    key idx_talent_actor_time (actor_user_id, request_time),
    key idx_talent_resource (resource_type, resource_id)
) engine=InnoDB default charset=utf8mb4 comment='达人中心内部管理审计';
