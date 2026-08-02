-- Verify activity schema after migration (read-only, safe to run all)

SHOW COLUMNS FROM app_activity LIKE 'dept_id';
SHOW COLUMNS FROM app_activity LIKE 'is_free';
SHOW COLUMNS FROM app_activity LIKE 'vip_price';

SHOW COLUMNS FROM app_activity_order LIKE 'order_no';
SHOW COLUMNS FROM app_activity_order LIKE 'pay_status';
SHOW COLUMNS FROM app_activity_order LIKE 'pay_time';

-- Check latest activity orders (replace 1 with your real order_id)
SELECT order_id, order_no, pay_status, status, pay_money, money_payable, sign_count
FROM app_activity_order
ORDER BY order_id DESC
LIMIT 5;

SELECT pay_no, order_type, status, order_id
FROM app_pay_log
WHERE order_type = '3'
ORDER BY log_id DESC
LIMIT 5;
