-- 未支付订单超时关单定时任务（与收银台/微信预下单 30 分钟对齐）
-- 在业务库执行一次后重启后端（或到「系统管理 → 定时任务」点启用），确认状态=正常

INSERT INTO sys_job (
    job_name, job_group, invoke_target, cron_expression,
    misfire_policy, concurrent, status, create_by, create_time, remark
)
SELECT
    '未支付订单超时关单',
    'DEFAULT',
    'appOrderTimeoutTask.closeExpiredOrders',
    '0 */1 * * * ?',
    '3',
    '1',
    '0',
    'admin',
    sysdate(),
    '每分钟扫描并关闭超过30分钟未支付的商品/教育/活动/会员卡订单；教育课会释放预占名额'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_job WHERE invoke_target = 'appOrderTimeoutTask.closeExpiredOrders'
);
