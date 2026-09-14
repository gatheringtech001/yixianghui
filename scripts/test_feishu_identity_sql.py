"""Run generated identity SQL against session-local MySQL fixtures only."""
import subprocess
import unittest
from pathlib import Path

from feishu_canonical_sql import _consultant_sql, _customer_sql, _income_sql

MYSQL = Path('/opt/homebrew/opt/mysql@8.4/bin/mysql')
TABLE = {'table_id': 'source-advisors', 'name': '养老顾问列表', 'fields': [
    {'field_name': name, 'field_id': field}
    for name, field in [('养老顾问', 'Name'), ('电话', 'Phone'), ('备注', 'Note')]
], 'records': [{'record_id': 'r1'}, {'record_id': 'r2'}]}
SETUP = """
SET NAMES utf8mb4;
CREATE TEMPORARY TABLE app_consultant (
 consultant_id bigint PRIMARY KEY AUTO_INCREMENT,consultant_no varchar(100),
 consultant_name varchar(100),mobile varchar(100),remark varchar(100),status varchar(2),create_time datetime);
CREATE TEMPORARY TABLE app_consultant_feishu (
 source_table_id varchar(64),feishu_record_id varchar(64) PRIMARY KEY,
 fs_name text,fs_phone text,fs_note text,canonical_table varchar(64),canonical_id bigint,
 canonical_status varchar(16),canonical_message varchar(500));
CREATE TEMPORARY TABLE app_consultant_feishu_source (
 source_table_id varchar(64),source_record_id varchar(64),consultant_id bigint,
 match_status varchar(16),PRIMARY KEY(source_table_id,source_record_id));
CREATE TEMPORARY TABLE app_feishu_migration_record (
 source_table_id varchar(64),source_record_id varchar(64),merge_status varchar(16),
 target_table varchar(64),target_id bigint,merge_message varchar(500));
"""


class IdentitySqlTest(unittest.TestCase):
    def run_case(self, fixture, query, repeat=False):
        _, statements = _consultant_sql([({'key': 'eldercare'}, TABLE)])
        generated = '\n'.join(statements)
        result = subprocess.run([str(MYSQL), '--batch', '--skip-column-names',
            '--default-character-set=utf8mb4', '-u', 'yixianghui_e2e', 'yixianghui_e2e'],
            input=SETUP.replace(');', ') DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;') + fixture + generated + (generated if repeat else '') + query,
            text=True, capture_output=True, timeout=20)
        self.assertEqual(0, result.returncode, result.stderr)
        return result.stdout.strip().splitlines()

    def test_same_name_without_phone_is_reviewed_not_created(self):
        rows = self.run_case("""
INSERT INTO app_consultant VALUES (4,NULL,'同名管家',NULL,NULL,'01',NULL);
INSERT INTO app_consultant_feishu VALUES ('source-advisors','r1','同名管家',NULL,NULL,NULL,NULL,NULL,NULL);
""", "SELECT COUNT(*) FROM app_consultant; SELECT canonical_status,canonical_id FROM app_consultant_feishu;", repeat=True)
        self.assertEqual(['1', 'needs_review\tNULL'], rows)

    def test_duplicate_phone_never_creates_or_chooses_first_candidate(self):
        rows = self.run_case("""
INSERT INTO app_consultant VALUES (4,NULL,'同名管家','13000000001',NULL,'01',NULL),(5,NULL,'同名管家','13000000001',NULL,'01',NULL);
INSERT INTO app_consultant_feishu VALUES ('source-advisors','r1','同名管家','13000000001',NULL,NULL,NULL,NULL,NULL);
""", "SELECT COUNT(*) FROM app_consultant; SELECT canonical_status FROM app_consultant_feishu;")
        self.assertEqual(['2', 'needs_review'], rows)

    def test_unique_phone_and_name_match_existing_without_changing_status(self):
        rows = self.run_case("""
INSERT INTO app_consultant VALUES (4,NULL,'管家甲','13000000001',NULL,'01',NULL);
INSERT INTO app_consultant_feishu VALUES ('source-advisors','r1','管家甲','13000000001',NULL,NULL,NULL,NULL,NULL);
""", "SELECT COUNT(*),MIN(status) FROM app_consultant; SELECT canonical_id,canonical_status FROM app_consultant_feishu;", repeat=True)
        self.assertEqual(['1\t01', '4\tlinked'], rows)

    def test_new_complete_identity_is_pending_and_replay_is_idempotent(self):
        rows = self.run_case("""
INSERT INTO app_consultant_feishu VALUES ('source-advisors','r1','新管家','13000000001',NULL,NULL,NULL,NULL,NULL);
""", "SELECT COUNT(*),MIN(status) FROM app_consultant; SELECT COUNT(*) FROM app_consultant_feishu_source;", repeat=True)
        self.assertEqual(['1\t00', '1'], rows)

    def test_same_key_in_source_is_not_duplicated(self):
        rows = self.run_case("""
INSERT INTO app_consultant_feishu VALUES ('source-advisors','r1','管家甲','13000000001',NULL,NULL,NULL,NULL,NULL),('source-advisors','r2','管家甲','13000000001',NULL,NULL,NULL,NULL,NULL);
""", "SELECT COUNT(*) FROM app_consultant; SELECT COUNT(*) FROM app_consultant_feishu WHERE canonical_status='needs_review';")
        self.assertEqual(['0', '2'], rows)

    def test_existing_mapping_is_not_reassigned_by_changed_phone(self):
        rows = self.run_case("""
INSERT INTO app_consultant VALUES (4,NULL,'管家甲','13000000001',NULL,'01',NULL),(5,NULL,'管家乙','13000000002',NULL,'01',NULL);
INSERT INTO app_consultant_feishu VALUES ('source-advisors','r1','管家乙','13000000002',NULL,NULL,NULL,NULL,NULL);
INSERT INTO app_consultant_feishu_source VALUES ('source-advisors','r1',4,'matched');
""", "SELECT consultant_id FROM app_consultant_feishu_source; SELECT canonical_status FROM app_consultant_feishu;")
        self.assertEqual(['4', 'needs_review'], rows)

    def test_missing_mapped_identity_does_not_create_a_replacement(self):
        rows = self.run_case("""
INSERT INTO app_consultant_feishu VALUES ('source-advisors','r1','管家甲','13000000001',NULL,NULL,NULL,NULL,NULL);
INSERT INTO app_consultant_feishu_source VALUES ('source-advisors','r1',4,'matched');
""", "SELECT COUNT(*) FROM app_consultant; SELECT canonical_status FROM app_consultant_feishu;")
        self.assertEqual(['0', 'needs_review'], rows)

    def test_blank_identity_never_becomes_a_business_person(self):
        rows = self.run_case("""
INSERT INTO app_consultant_feishu VALUES ('source-advisors','r1',NULL,NULL,NULL,NULL,NULL,NULL,NULL);
""", "SELECT COUNT(*) FROM app_consultant; SELECT canonical_status FROM app_consultant_feishu;")
        self.assertEqual(['0', 'needs_review'], rows)

    def test_manually_skipped_source_is_not_reenabled(self):
        rows = self.run_case("""
INSERT INTO app_consultant_feishu VALUES ('source-advisors','r1','管家甲','13000000001',NULL,NULL,NULL,'skipped','manual exclusion');
""", "SELECT COUNT(*) FROM app_consultant; SELECT canonical_status,canonical_message FROM app_consultant_feishu;", repeat=True)
        self.assertEqual(['0', 'skipped\tmanual exclusion'], rows)

    def test_source_rows_outside_this_export_are_preserved_not_imported(self):
        rows = self.run_case("""
INSERT INTO app_consultant_feishu VALUES ('source-advisors','old-record','旧快照','13000000009',NULL,NULL,NULL,'unresolved',NULL);
""", "SELECT COUNT(*) FROM app_consultant; SELECT canonical_status FROM app_consultant_feishu;", repeat=True)
        self.assertEqual(['0', 'unresolved'], rows)


INCOME_FIELDS = ['销售内容', '充值金额', '消费金额', '余额', '积分', '成交日期',
                 '是否结算', '公司收入', '管家提成', '产品类别', '备注']
INCOME_TABLE = {'table_id': 'source-income', 'name': '🧾收入明细数据',
                'fields': [{'field_name': name, 'field_id': f'f{i}'} for i, name in enumerate(INCOME_FIELDS)],
                'records': [{'record_id': 'i1'}]}
INCOME_SETUP = """
SET NAMES utf8mb4;
CREATE TEMPORARY TABLE app_customer_income (
 income_id bigint PRIMARY KEY AUTO_INCREMENT,user_id bigint,product_name text,income_no varchar(100),dept_id bigint,
 charge_amount decimal(10,2),purchase_amount decimal(10,2),balance decimal(10,2),score bigint,trade_date date,
 settlement smallint,company_income decimal(10,2),consultant_income decimal(10,2),product_type text,remark text,
 customer_id bigint,consultant_id bigint,create_by varchar(64),create_time datetime);
CREATE TEMPORARY TABLE app_customer_income_feishu (
 source_table_id varchar(64),feishu_record_id varchar(64) PRIMARY KEY,
 fs_f0 text,fs_f1 decimal(10,2),fs_f2 decimal(10,2),fs_f3 decimal(10,2),fs_f4 bigint,fs_f5 datetime,
 fs_f6 tinyint,fs_f7 decimal(10,2),fs_f8 decimal(10,2),fs_f9 text,fs_f10 text,
 canonical_table varchar(64),canonical_id bigint,canonical_status varchar(16),canonical_message text);
CREATE TEMPORARY TABLE app_feishu_business_relation (
 source_table_id varchar(64),source_record_id varchar(64),target_business_table varchar(64),
 target_business_id bigint,relation_status varchar(16));
CREATE TEMPORARY TABLE app_feishu_migration_record (
 source_table_id varchar(64),source_record_id varchar(64),merge_status varchar(16),
 target_table varchar(64),target_id bigint,merge_message text);
INSERT INTO app_customer_income_feishu VALUES ('source-income','i1','商品',0,100,0,0,'2026-01-01',NULL,10,NULL,NULL,NULL,NULL,NULL,'unresolved',NULL);
INSERT INTO app_feishu_business_relation VALUES ('source-income','i1','app_customer',1,'resolved');
"""


class IncomeSqlTest(unittest.TestCase):
    def run_case(self, fixture, query, between=''):
        sql = '\n'.join(_income_sql([({'key': 'eldercare'}, INCOME_TABLE)]))
        schema, records = INCOME_SETUP.split('INSERT INTO app_customer_income_feishu', 1)
        result = subprocess.run([str(MYSQL), '--batch', '--skip-column-names', '--default-character-set=utf8mb4',
            '-u', 'yixianghui_e2e', 'yixianghui_e2e'], input=schema.replace(');',
            ') DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;') + 'INSERT INTO app_customer_income_feishu' + records + fixture + sql + between + sql + query,
            text=True, capture_output=True, timeout=20)
        self.assertEqual(0, result.returncode, result.stderr)
        return result.stdout.strip().splitlines()

    def test_multiple_consultants_are_not_reduced_to_min_id(self):
        rows = self.run_case("""
INSERT INTO app_feishu_business_relation VALUES ('source-income','i1','app_consultant',4,'resolved'),('source-income','i1','app_consultant',42,'resolved');
""", "SELECT COUNT(*) FROM app_customer_income; SELECT canonical_status FROM app_customer_income_feishu;")
        self.assertEqual(['0', 'needs_review'], rows)

    def test_missing_or_unresolved_owner_is_not_imported_as_complete(self):
        rows = self.run_case("""
INSERT INTO app_feishu_business_relation VALUES ('source-income','i1','app_consultant',42,'unresolved');
""", "SELECT COUNT(*) FROM app_customer_income; SELECT canonical_status FROM app_customer_income_feishu;")
        self.assertEqual(['0', 'needs_review'], rows)

    def test_unique_relation_preserves_unknown_money_and_settlement(self):
        rows = self.run_case("""
INSERT INTO app_feishu_business_relation VALUES ('source-income','i1','app_consultant',4,'resolved');
""", "SELECT COUNT(*),MIN(consultant_id),MIN(settlement),MIN(consultant_income) FROM app_customer_income;")
        self.assertEqual(['1\t4\tNULL\tNULL'], rows)

    def test_source_change_does_not_overwrite_business_money_or_claim_merged(self):
        rows = self.run_case("""
INSERT INTO app_feishu_business_relation VALUES ('source-income','i1','app_consultant',4,'resolved');
""", "SELECT purchase_amount FROM app_customer_income; SELECT canonical_status FROM app_customer_income_feishu;",
            "UPDATE app_customer_income_feishu SET fs_f2=999;")
        self.assertEqual(['100.00', 'needs_review'], rows)


CUSTOMER_TABLES = [({'key': key}, {'table_id': key, 'name': table_name, 'records': [{'record_id': 't1'}], 'fields': [
    {'field_name': name, 'field_id': field} for name, field in fields]})
    for key, table_name, fields in [
        ('travel', '客户信息表', [('客户编号', 'Key'), ('客户名称', 'Name'), ('联系方式', 'Phone'),
                               ('来源', 'Source'), ('客户状态', 'Label'), ('日期', 'Date')]),
        ('eldercare', '客户档案', [('客户名称', 'Name'), ('电话', 'Phone'),
                               ('客户获取渠道', 'Source'), ('客户标签', 'Label'), ('登记日期', 'Date')])]]
CUSTOMER_SETUP = """
SET NAMES utf8mb4;
CREATE TEMPORARY TABLE app_customer (customer_id bigint PRIMARY KEY AUTO_INCREMENT,customer_no varchar(64),customer_name varchar(100),
 link_mobile varchar(64),acquisition_channel varchar(100),customer_label varchar(100),sign_time datetime,
 status varchar(2),create_by varchar(64),create_time datetime,del_flag varchar(1));
CREATE TEMPORARY TABLE app_travel_customer_profile (source_table_id varchar(64),feishu_record_id varchar(64),
 fs_key decimal(20,4),fs_name text,fs_phone text,fs_source text,fs_label text,fs_date datetime,
 canonical_table varchar(64),canonical_id bigint,canonical_status varchar(16),canonical_message text);
CREATE TEMPORARY TABLE app_eldercare_customer_profile LIKE app_travel_customer_profile;
CREATE TEMPORARY TABLE app_customer_feishu_source (source_table_id varchar(64),source_record_id varchar(64),customer_id bigint,
 business_line varchar(20),match_method varchar(32),match_status varchar(16),match_message text,
 PRIMARY KEY(source_table_id,source_record_id));
CREATE TEMPORARY TABLE app_feishu_migration_record (source_table_id varchar(64),source_record_id varchar(64),
 merge_status varchar(16),target_table varchar(64),target_id bigint,merge_message text);
"""


class CustomerSqlTest(unittest.TestCase):
    def run_case(self, fixture):
        _, statements = _customer_sql(CUSTOMER_TABLES)
        sql = '\n'.join(statements)
        query = "SELECT COUNT(*) FROM app_customer; SELECT canonical_status FROM app_travel_customer_profile;"
        result = subprocess.run([str(MYSQL), '--batch', '--skip-column-names', '--default-character-set=utf8mb4',
            '-u', 'yixianghui_e2e', 'yixianghui_e2e'], input=CUSTOMER_SETUP.replace(');',
            ') DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;') + fixture + sql + sql + query,
            text=True, capture_output=True, timeout=20)
        self.assertEqual(0, result.returncode, result.stderr)
        return result.stdout.strip().splitlines()

    def test_numeric_customer_source_is_stable_on_replay(self):
        rows = self.run_case("""
INSERT INTO app_travel_customer_profile VALUES ('travel','t1',123,'新客户','13000000001',NULL,NULL,NULL,NULL,NULL,NULL,NULL);
""")
        self.assertEqual(['1', 'linked'], rows)

    def test_missing_customer_key_does_not_create_same_name_person(self):
        rows = self.run_case("""
INSERT INTO app_customer VALUES (1,NULL,'同名客户',NULL,NULL,NULL,NULL,'0',NULL,NULL,'0');
INSERT INTO app_travel_customer_profile VALUES ('travel','t1',NULL,'同名客户',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL);
""")
        self.assertEqual(['1', 'needs_review'], rows)

    def test_deleted_customer_is_not_claimed_or_recreated(self):
        rows = self.run_case("""
INSERT INTO app_customer VALUES (1,'123','旧客户',NULL,NULL,NULL,NULL,'0',NULL,NULL,'1');
INSERT INTO app_travel_customer_profile VALUES ('travel','t1',123,'旧客户',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL);
""")
        self.assertEqual(['1', 'needs_review'], rows)


if __name__ == '__main__':
    unittest.main()
