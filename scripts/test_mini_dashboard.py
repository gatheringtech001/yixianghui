#!/usr/bin/env python3
"""Execute the actual Java SQL in an isolated synthetic, in-memory database."""
import re, sqlite3, unittest
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
SOURCE = (ROOT / 'lankong-admin/src/main/java/com/ruoyi/web/service/MiniDashboardService.java').read_text()
def constant(name):
    expr = re.search(r'static final String ' + name + r' = (.*?);', SOURCE, re.S).group(1)
    parts = re.findall(r'"((?:\\.|[^"\\])*)"|\b(CATEGORY|ORDERS|REFUNDS|METRICS)\b', expr)
    return ''.join(bytes(s, 'utf8').decode('unicode_escape').encode('latin1').decode('utf8') if s else constant(ref) for s, ref in parts)
class DashboardSqlTest(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(':memory:'); self.db.row_factory = sqlite3.Row
        self.db.create_function('FIND_IN_SET', 2, lambda x, values: str(x) in str(values).split(','))
        self.db.create_function('CONCAT', -1, lambda *args: ''.join(map(str, args)))
        self.db.executescript('''
          CREATE TABLE app_goods_category(category_id INTEGER,parent_id INTEGER,parent_ids TEXT,category_name TEXT,status TEXT);
          INSERT INTO app_goods_category VALUES(58,0,'0','老年教育','1'),(60,58,'0,58','摄影数码','1');
          CREATE TABLE app_goods(goods_id INTEGER,category_id INTEGER); INSERT INTO app_goods VALUES(1,60);
          CREATE TABLE app_goods_order(order_id INTEGER,goods_id INTEGER,order_origin TEXT,create_time TEXT,pay_status TEXT,pay_money NUMERIC,pay_time TEXT);
          INSERT INTO app_goods_order VALUES(1,1,'mini_program','2026-11-12','1',100.01,'2026-11-12'),(2,1,'mini_program','2026-11-12','2',999,NULL),(3,1,'mini_program','2026-11-12','0',0,NULL),(4,1,'feishu_history','2026-11-12','1',9999,'2026-11-12'),(5,1,'mini_program','2026-10-01','4',200,'2026-10-01'),(6,1,'mini_program','2026-11-12','1',50,NULL);
          CREATE TABLE app_activity_order(order_id INTEGER,create_time TEXT,pay_status TEXT,pay_money NUMERIC,pay_time TEXT);
          INSERT INTO app_activity_order VALUES(1,'2026-11-12','4',50,'2026-11-12'),(2,'2026-11-12','1',0,NULL),(3,'2026-11-12',NULL,999,NULL);
          CREATE TABLE app_pay_refund_log(log_id INTEGER,order_type TEXT,order_id INTEGER,agent_refund_no TEXT,refund_money NUMERIC,status TEXT);
          INSERT INTO app_pay_refund_log VALUES(1,'3',1,'refund-1',5000,'1'),(2,'3',1,'refund-1',5000,'1'),(3,'2',5,'refund-2',20000,'1'),(4,'2',999,'orphan',999999,'1'),(5,'2',1,'pending',10001,'0');
        ''')
    def rows(self, where='', args=()):
        sql = 'SELECT ' + constant('METRICS') + ' FROM ' + constant('ORDERS') + ' LEFT JOIN ' + constant('REFUNDS') + ' ON r.order_type=o.kind AND r.order_id=o.order_id' + where
        return dict(self.db.execute(sql, args).fetchone())
    def test_receipts_not_order_count_or_cancelled_amounts(self):
        row=self.rows(); self.assertEqual(row['orderCount'],8); self.assertAlmostEqual(row['receivedAmount'],400.01); self.assertEqual(row['pendingCount'],1); self.assertEqual(row['paidCount'],5)
    def test_refund_dedup_fen_conversion_and_orphan_exclusion(self):
        row=self.rows(); self.assertEqual(row['refundAmount'],250); self.assertAlmostEqual(row['netAmount'],150.01)
    def test_date_cohort_and_missing_status_are_explicit(self):
        row=self.rows(' WHERE o.create_time>=? AND o.create_time<?', ('2026-11-12','2026-11-13')); self.assertEqual(row['orderCount'],7); self.assertEqual(row['refundAmount'],50); self.assertEqual(row['unknownStatusCount'],1); self.assertEqual(row['missingPayTimeCount'],1)
    def test_empty_range_is_real_zero(self):
        row=self.rows(' WHERE o.create_time>=?', ('2027-01-01',)); self.assertEqual(row['orderCount'],0); self.assertEqual(row['receivedAmount'],0); self.assertEqual(row['refundAmount'],0)
    def test_business_classification_matches_mini_program(self):
        row=self.db.execute('SELECT o.business, COUNT(*) n FROM '+constant('ORDERS')+' GROUP BY o.business').fetchall(); self.assertEqual({r['business']:r['n'] for r in row},{'芳华学院':5,'聚会活动':3})
if __name__ == '__main__': unittest.main()
