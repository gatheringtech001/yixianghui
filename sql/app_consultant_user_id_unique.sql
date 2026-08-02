-- ���ʹ����û�Ψһ���������� user_id Ϊ NULL���� NULL ����Ψһ
-- ִ��ǰ���������ظ��󶨣�SELECT user_id, COUNT(*) FROM app_consultant WHERE user_id IS NOT NULL GROUP BY user_id HAVING COUNT(*) > 1;
ALTER TABLE app_consultant ADD UNIQUE INDEX uk_app_consultant_user_id (user_id);
