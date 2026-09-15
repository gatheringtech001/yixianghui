"""Execute Talent's actual MyBatis ownership predicates in local MySQL."""
import re
import subprocess
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

XML = Path(__file__).resolve().parents[1] / 'ruoyi-system/src/main/resources/mapper/system/TalentCenterOperationsMapper.xml'
SETUP = """
CREATE TEMPORARY TABLE app_goods_order(order_id bigint,feishu_record_id varchar(64),service_owner_user_id bigint,user_id bigint,order_origin varchar(30),travel_status varchar(2),pay_status varchar(2),update_time datetime,is_cash int);
CREATE TEMPORARY TABLE app_user_inviter(user_id bigint,new_user_id bigint,status varchar(2));
CREATE TEMPORARY TABLE app_feishu_business_relation(source_table_id varchar(64),source_record_id varchar(64),source_field_id varchar(64),target_source_table_id varchar(64),target_source_record_id varchar(64),target_business_table varchar(64),target_business_id bigint,relation_status varchar(16),relation_message varchar(100));
CREATE TEMPORARY TABLE app_feishu_business_user(source_table_id varchar(64),source_record_id varchar(64),source_field_id varchar(64),feishu_user_id varchar(64),user_name varchar(100));
CREATE TEMPORARY TABLE app_consultant(consultant_id bigint,consultant_name varchar(100),status varchar(2));
CREATE TEMPORARY TABLE app_consultant_feishu(source_table_id varchar(64),feishu_record_id varchar(64),canonical_id bigint,canonical_table varchar(64),canonical_status varchar(16));
CREATE TEMPORARY TABLE app_customer_income(income_id bigint,income_no varchar(100),consultant_id bigint,consultant_income decimal(10,2),trade_date date);
INSERT INTO app_goods_order VALUES (139,'o1',NULL,0,'feishu_history','0','0',NULL,0),(999,'o2',108,0,'feishu_history','0','0',NULL,0);
INSERT INTO app_consultant VALUES (4,'owner','01');
INSERT INTO app_consultant_feishu VALUES ('tblRKs34PUprry5y','c4',4,'app_consultant','linked');
INSERT INTO app_feishu_business_user VALUES ('tblZWWWFyzY1hAyb','o1','fldXxpzt6w','u1','owner');
INSERT INTO app_feishu_business_relation VALUES ('tblZWWWFyzY1hAyb','o1','fldXxpzt6w','tblRKs34PUprry5y','c4','app_consultant',4,'resolved','owner_user_unique_consultant_name');
"""


def statement(name, values):
    root = ET.parse(XML).getroot()
    def expand(node):
        assert node is not None, 'Missing mapper SQL: ' + name
        text = node.text or ''
        for child in node:
            assert child.tag == 'include', 'Unexpected dynamic SQL in predicate'
            text += expand(root.find("./sql[@id='" + child.attrib['refid'] + "']")) + (child.tail or '')
        return text
    sql = expand(root.find("./*[@id='" + name + "']"))
    return re.sub(r'#\{(\w+)\}', lambda m: str(values[m[1]]) if values.get(m[1]) is not None else 'NULL', sql)


class TalentScopeSqlTest(unittest.TestCase):
    def run_sql(self, fixture, sql):
        result = subprocess.run(['/opt/homebrew/opt/mysql@8.4/bin/mysql', '--batch', '--skip-column-names',
            '-u', 'yixianghui_e2e', 'yixianghui_e2e'], input=SETUP + fixture + sql,
            text=True, capture_output=True, timeout=20)
        self.assertEqual(0, result.returncode, result.stderr)
        return result.stdout.strip()

    def read(self, fixture='', consultant=4, actor=None):
        condition = statement('orderReadScope', {'admin': 0, 'actorUserId': actor, 'consultantId': consultant})
        return self.run_sql(fixture, 'SELECT order_id FROM app_goods_order o WHERE ' + condition + ' ORDER BY order_id;')

    def test_confirmed_owner_reads_own_orders_not_testers(self):
        self.assertEqual('139', self.read())
        self.assertEqual('', self.read(consultant=24))
        self.assertEqual('999', self.read(consultant=24, actor=108))

    def test_wrong_field_or_unresolved_link_never_grants_read(self):
        for change in ["source_field_id='Other'", "relation_status='unresolved'", "relation_message=NULL",
                       "target_business_table='app_customer'", "source_table_id='Other'", "target_source_record_id='missing'"]:
            self.assertEqual('', self.read('UPDATE app_feishu_business_relation SET ' + change + ';'))

    def test_missing_current_user_and_inactive_consultant_fail_closed(self):
        self.assertEqual('', self.read('DELETE FROM app_feishu_business_user;'))
        self.assertEqual('', self.read("UPDATE app_consultant SET status='00';"))

    def test_derived_owner_does_not_gain_order_write(self):
        sql = statement('updateOrderStatus', {'id': 139, 'status': "'1'", 'expectedStatus': "'0'", 'actorUserId': 108, 'admin': 0})
        self.assertEqual('0', self.run_sql('', sql + '; SELECT ROW_COUNT();'))

    def test_shared_record_is_counted_once_and_not_personally_allocated(self):
        fixture = """
INSERT INTO app_customer_income VALUES (1,'FS-i1',NULL,80,'2026-09-01'),(2,'FS-i2',4,10,'2026-08-31');
INSERT INTO app_feishu_business_relation VALUES
('tblA33x9gGWM1b51','i1','fld26vaZVW','tblRKs34PUprry5y','c4','app_consultant',4,'resolved',NULL),
('tblA33x9gGWM1b51','i1','fld26vaZVW','tblRKs34PUprry5y','c10','app_consultant',10,'resolved',NULL);
"""
        count = statement('selectSharedCommissionCount', {'consultantId': 4})
        self.assertEqual('1\n10.00', self.run_sql(fixture, count + '; SELECT SUM(consultant_income) FROM app_customer_income WHERE consultant_id=4;'))
        self.assertEqual('0', self.run_sql(fixture, statement('selectSharedCommissionCount', {'consultantId': 24}) + ';'))
        self.assertEqual('0', self.run_sql(fixture, statement('selectSharedCommissionCount', {'consultantId': 4, 'month': "'2026-08'", 'monthStart': "'2026-08-01'", 'monthEnd': "'2026-09-01'"}) + ';'))

    def test_month_boundaries_and_undated_are_disjoint(self):
        fixture = "INSERT INTO app_customer_income VALUES (1,'a',4,1,'2026-08-31'),(2,'b',4,2,'2026-09-01'),(3,'c',4,3,'2026-09-30'),(4,'d',4,4,'2026-10-01'),(5,'e',4,5,NULL);"
        scope = statement('commissionMonthScope', {'month': "'2026-09'", 'monthStart': "'2026-09-01'", 'monthEnd': "'2026-10-01'"})
        self.assertEqual('2\t5.00', self.run_sql(fixture, 'SELECT COUNT(*),SUM(consultant_income) FROM app_customer_income i WHERE '+scope+';'))
        scope = statement('commissionMonthScope', {'month': "'undated'"})
        self.assertEqual('1\t5.00', self.run_sql(fixture, 'SELECT COUNT(*),SUM(consultant_income) FROM app_customer_income i WHERE '+scope+';'))


if __name__ == '__main__':
    unittest.main()
