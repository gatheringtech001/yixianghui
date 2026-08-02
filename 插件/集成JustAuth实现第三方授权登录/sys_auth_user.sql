DROP TABLE IF EXISTS sys_auth_user;
CREATE TABLE sys_auth_user (
  auth_id           BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '授权ID',
  uuid              VARCHAR(500)    NOT NULL                   COMMENT '第三方平台用户唯一ID',
  user_id           BIGINT(20)      NOT NULL                   COMMENT '系统用户ID',
  user_name         VARCHAR(30)     NOT NULL                   COMMENT '登录账号',
  nick_name         VARCHAR(30)     DEFAULT ''                 COMMENT '用户昵称',
  avatar            VARCHAR(500)    DEFAULT ''                 COMMENT '头像地址',
  email             VARCHAR(255)    DEFAULT ''                 COMMENT '用户邮箱',
  source            VARCHAR(255)    DEFAULT ''                 COMMENT '用户来源',
  create_time       DATETIME                                   COMMENT '创建时间',
  PRIMARY KEY (auth_id)
) ENGINE=INNODB AUTO_INCREMENT=100 COMMENT = '第三方授权表';