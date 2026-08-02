-- 扩大用户头像字段长度，避免小程序上传路径超过 varchar(100) 导致登录保存失败
ALTER TABLE sys_user MODIFY COLUMN avatar varchar(500) DEFAULT '' COMMENT '头像地址';
