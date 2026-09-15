"""Test real customer/order relation SQL, with no production writes."""
import unittest, subprocess, uuid
from test_talent_feishu_scope_sql import SETUP, statement

FIXTURE = """
CREATE TEMPORARY TABLE app_customer(customer_id bigint,consultant_id bigint,del_flag varchar(1));
CREATE TEMPORARY TABLE app_customer_feishu_source(customer_id bigint,source_table_id varchar(64),source_record_id varchar(64),business_line varchar(16),match_status varchar(16));
CREATE TEMPORARY TABLE app_travel_customer_profile(source_table_id varchar(64),feishu_record_id varchar(64),canonical_table varchar(64),canonical_id bigint,canonical_status varchar(16));
INSERT INTO app_customer VALUES (1,4,'0'),(2,4,'0'),(3,5,'0'),(4,4,'0'),(5,4,'0'),(6,4,'1');
INSERT INTO app_customer_feishu_source VALUES
(1,'tblXVuYCto8OOmAz','cust1','travel','matched'),(3,'tblXVuYCto8OOmAz','cust3','travel','matched'),
(4,'tblXVuYCto8OOmAz','cust4','travel','matched'),(5,'tblXVuYCto8OOmAz','cust5','travel','matched'),(6,'tblXVuYCto8OOmAz','cust6','travel','matched');
INSERT INTO app_travel_customer_profile VALUES
('tblXVuYCto8OOmAz','cust1','app_customer',1,'linked'),('tblXVuYCto8OOmAz','cust3','app_customer',3,'linked'),
('tblXVuYCto8OOmAz','cust4','app_customer',4,'needs_review'),('tblXVuYCto8OOmAz','cust5','app_customer',5,'linked'),('tblXVuYCto8OOmAz','cust6','app_customer',6,'linked');
INSERT INTO app_feishu_business_relation VALUES
('tblZWWWFyzY1hAyb','o1','fldioLyM1q','tblXVuYCto8OOmAz','cust1','app_customer',1,'resolved',NULL),
('tblZWWWFyzY1hAyb','o1','fldioLyM1q','tblXVuYCto8OOmAz','cust1','app_customer',1,'resolved',NULL),
('tblZWWWFyzY1hAyb','o2','fldioLyM1q','tblXVuYCto8OOmAz','cust1','app_customer',1,'resolved',NULL),
('tblZWWWFyzY1hAyb','o1','fldioLyM1q','tblXVuYCto8OOmAz','cust3','app_customer',3,'resolved',NULL),
('tblZWWWFyzY1hAyb','o1','fldioLyM1q','tblXVuYCto8OOmAz','cust4','app_customer',4,'resolved',NULL),
('tblZWWWFyzY1hAyb','o1','fldioLyM1q','tblXVuYCto8OOmAz','cust5','app_customer',5,'superseded',NULL);
"""

class CustomerOrderMetricsSqlTest(unittest.TestCase):
    def run_sql(self, fixture, sql):
        schema='codex_customer_order_'+uuid.uuid4().hex[:8]
        command=['/opt/homebrew/opt/mysql@8.4/bin/mysql','-uroot','--batch','--skip-column-names']
        def execute(text, selected=False):
            result=subprocess.run(command+([schema] if selected else []),input=text,text=True,capture_output=True,timeout=20)
            self.assertEqual(0,result.returncode,result.stderr)
            return result.stdout.strip()
        execute('CREATE DATABASE `'+schema+'`;')
        try:return execute(SETUP.replace('CREATE TEMPORARY TABLE','CREATE TABLE')+fixture.replace('CREATE TEMPORARY TABLE','CREATE TABLE')+sql,True)
        finally:execute('DROP DATABASE `'+schema+'`;')

    def test_counts_distinct_visible_orders_and_keeps_verified_zero(self):
        sql=statement('selectCustomerOrderLinks', {'actorUserId':None,'consultantId':4,'admin':0})
        rows=self.run_sql(FIXTURE,sql+';').splitlines()
        self.assertEqual(['customer:1\tNULL','customer:1\torder:139','customer:5\tNULL'],rows)

    def test_no_identity_never_receives_customers_or_orders(self):
        sql=statement('selectCustomerOrderLinks', {'actorUserId':None,'consultantId':None,'admin':0})
        self.assertEqual('',self.run_sql(FIXTURE,sql+';'))

    def test_corrupt_target_mapping_is_not_counted(self):
        sql=statement('selectCustomerOrderLinks', {'actorUserId':None,'consultantId':4,'admin':0})
        rows=self.run_sql(FIXTURE+"UPDATE app_feishu_business_relation SET target_source_record_id='wrong' WHERE target_business_table='app_customer';",sql+';')
        self.assertNotIn('order:139',rows)

if __name__=='__main__':unittest.main()
