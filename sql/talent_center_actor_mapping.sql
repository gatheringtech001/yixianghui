-- 达人中心稳定用户 ID 与小程序后台管理员映射。
-- status: 0=启用，1=停用。生产执行后需由管理员写入实际映射记录。
create table talent_center_admin_actor (
    actor_id varchar(128) not null comment '达人中心稳定用户ID',
    user_id bigint not null comment '小程序后台sys_user.user_id',
    display_name varchar(64) not null comment '审计展示名，不参与鉴权',
    status char(1) not null default '0' comment '0启用 1停用',
    create_time datetime not null default current_timestamp,
    update_time datetime null default null on update current_timestamp,
    primary key (actor_id),
    unique key uk_talent_actor_user (user_id),
    key idx_talent_actor_status (status)
) engine=InnoDB default charset=utf8mb4 comment='达人中心管理员映射';
