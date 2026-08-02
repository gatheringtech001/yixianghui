-- 活动结束时间：仅用于判断是否进入小程序「已结束」，不在小程序展示
-- 请在业务库执行本脚本后再重启后端

ALTER TABLE app_activity
    ADD COLUMN activity_end_time DATETIME NULL COMMENT '活动结束时间(仅用于结束判断，不在小程序展示)' AFTER activity_time;
