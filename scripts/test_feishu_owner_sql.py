"""Exercise derived owner links with session-local MySQL fixtures."""
import subprocess
import unittest

from feishu_owner_sql import owner_relations_sql

TABLE = {'table_id': 'orders', 'records': [{'record_id': 'o1'}],
         'fields': [{'field_id': 'Owner', 'field_name': '客服负责人', 'ui_type': 'User',
                     'property': {'multiple': False}}]}
SETUP = """
SET NAMES utf8mb4;
CREATE TEMPORARY TABLE app_travel_order_profile(source_table_id varchar(64),feishu_record_id varchar(64),canonical_id bigint,canonical_status varchar(16));
CREATE TEMPORARY TABLE app_consultant(consultant_id bigint,consultant_name varchar(100),status varchar(2));
CREATE TEMPORARY TABLE app_consultant_feishu(source_table_id varchar(64),feishu_record_id varchar(64),canonical_id bigint,canonical_table varchar(64),canonical_status varchar(16));
CREATE TEMPORARY TABLE app_feishu_business_user(source_table_id varchar(64),source_record_id varchar(64),source_field_id varchar(64),feishu_user_id varchar(128),user_name varchar(100),user_order int);
CREATE TEMPORARY TABLE app_feishu_business_relation(source_table_id varchar(64),source_record_id varchar(64),source_field_id varchar(64),target_source_table_id varchar(64),target_source_record_id varchar(64),target_business_table varchar(64),target_business_id bigint,display_text varchar(500),relation_status varchar(16),relation_message varchar(500),relation_order int);
INSERT INTO app_travel_order_profile VALUES ('orders','o1',139,'linked');
INSERT INTO app_consultant VALUES (4,'管家甲','01');
INSERT INTO app_consultant_feishu VALUES ('consultants','c1',4,'app_consultant','linked');
INSERT INTO app_feishu_business_user VALUES ('orders','o1','Owner','u1','管家甲',0);
"""


class OwnerSqlTest(unittest.TestCase):
    def run_case(self, fixture, expected):
        sql = '\n'.join(owner_relations_sql(TABLE))
        result = subprocess.run(['/opt/homebrew/opt/mysql@8.4/bin/mysql', '--batch', '--skip-column-names',
            '--default-character-set=utf8mb4', '-u', 'yixianghui_e2e', 'yixianghui_e2e'],
            input=SETUP + fixture + sql + sql + 'SELECT COUNT(*),MIN(target_business_id) FROM app_feishu_business_relation;',
            text=True, capture_output=True, timeout=20)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(expected, result.stdout.strip())

    def test_unique_verified_owner_is_idempotent(self):
        self.run_case('', '1\t4')

    def test_ambiguous_name_never_chooses_minimum(self):
        self.run_case("INSERT INTO app_consultant VALUES (5,'管家甲','01');", '0\tNULL')

    def test_unverified_consultant_source_is_not_an_owner(self):
        self.run_case("UPDATE app_consultant_feishu SET canonical_status='needs_review';", '0\tNULL')

    def test_other_user_field_never_grants_order_ownership(self):
        self.run_case("UPDATE app_feishu_business_user SET source_field_id='Creator';", '0\tNULL')

    def test_multiple_users_and_missing_user_id_fail_closed(self):
        self.run_case("INSERT INTO app_feishu_business_user VALUES ('orders','o1','Owner','u2','管家乙',1);", '0\tNULL')
        self.run_case("UPDATE app_feishu_business_user SET feishu_user_id=NULL;", '0\tNULL')

    def test_other_export_and_skipped_order_are_untouched(self):
        self.run_case("UPDATE app_travel_order_profile SET feishu_record_id='old';", '0\tNULL')
        self.run_case("UPDATE app_travel_order_profile SET canonical_status='skipped';", '0\tNULL')


if __name__ == '__main__':
    unittest.main()
