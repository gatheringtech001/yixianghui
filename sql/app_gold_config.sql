-- 金币赠送/扣回配置（缺省时后端使用代码默认值，本脚本可选执行）
-- gold.scope.card 默认 false：会员暂不接入，后续改为 true 并在开通/退款处调用 IAppGoldService 即可

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '金币-支付赠送开关', 'gold.pay.enabled', 'true', 'N', 'admin', sysdate(), '支付成功是否赠币'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'gold.pay.enabled');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '金币-每元赠送数量', 'gold.pay.rate', '1', 'N', 'admin', sysdate(), '实付1元赠送金币数'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'gold.pay.rate');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '金币-退款扣回开关', 'gold.refund.reverse.enabled', 'true', 'N', 'admin', sysdate(), '退款成功是否扣回赠币'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'gold.refund.reverse.enabled');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '金币-商品订单范围', 'gold.scope.goods', 'true', 'N', 'admin', sysdate(), '商城/酒店/教育是否参与'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'gold.scope.goods');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '金币-活动订单范围', 'gold.scope.activity', 'true', 'N', 'admin', sysdate(), '活动付费报名是否参与'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'gold.scope.activity');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '金币-会员卡范围', 'gold.scope.card', 'false', 'N', 'admin', sysdate(), '会员开通/退款是否参与（预留，默认关）'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'gold.scope.card');
