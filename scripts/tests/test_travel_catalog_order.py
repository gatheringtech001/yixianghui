"""Exercise the actual MyBatis ORDER BY with read-only MySQL fixture queries."""
import json
import os
from pathlib import Path
import sys
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
MAPPER = ROOT / 'ruoyi-system/src/main/resources/mapper/system/AppGoodsMapper.xml'


def fragment(name):
    root = ET.parse(MAPPER).getroot()
    def expand(node):
        parts = [node.text or '']
        for child in node:
            if child.tag != 'include':
                raise AssertionError('Unexpected dynamic node in sort fragment')
            parts += [expand(root.find("sql[@id='%s']" % child.attrib['refid'])), child.tail or '']
        return ''.join(parts)
    return expand(root.find("sql[@id='%s']" % name))


class TravelCatalogOrderContractTest(unittest.TestCase):
    def test_shared_mapper_orders_before_pagination_without_mutating_content(self):
        root = ET.parse(MAPPER).getroot()
        select = root.find("select[@id='selectAppGoodsList']")
        self.assertEqual(list(select)[-1].attrib['refid'], 'catalogNameOrder')
        sql = fragment('catalogNameOrder')
        self.assertNotIn('${', sql)
        self.assertIn('over () = 0', sql)
        self.assertIn('g.goods_id desc', sql)


@unittest.skipUnless(os.environ.get('YXH_SORT_MYSQL_ENV'), 'Set YXH_SORT_MYSQL_ENV for read-only MySQL tests')
class TravelCatalogOrderMySQLTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        sys.path.insert(0, str(Path.home() / '.codex/skills/manage-yixianghui/scripts'))
        from yxh_runtime import run_mysql
        from yxh_policy import validate_read_sql
        cls.run_query = staticmethod(lambda sql: run_mysql(
            os.environ['YXH_SORT_MYSQL_ENV'],
            validate_read_sql(sql, production=True), headers=False))

    def sorted_ids(self, rows, limit=200, offset=0):
        data = json.dumps([dict(goods_id=i, goods_name=n, goods_type=t) for i, n, t in rows], ensure_ascii=False)
        sql = "SELECT g.goods_id FROM JSON_TABLE(CONVERT(0x%s USING utf8mb4), '$[*]' COLUMNS (goods_id BIGINT PATH '$.goods_id', goods_name VARCHAR(255) PATH '$.goods_name', goods_type VARCHAR(50) PATH '$.goods_type')) g %s LIMIT %d OFFSET %d" % (data.encode().hex(), fragment('catalogNameOrder'), limit, offset)
        return [int(value) for value in self.run_query(sql).split()]

    def test_chinese_and_arabic_numbers_are_numeric_not_lexical(self):
        names = ['弥勒十号基地', '弥勒二号基地', '弥勒一号温泉基地', '弥勒九号基地',
                 '弥勒十一号基地', '弥勒二十号基地', '弥勒二十一号基地', '弥勒九十九号基地',
                 '弥勒100号基地', '弥勒3号基地']
        self.assertEqual(self.sorted_ids([(i, n, 'hotel') for i, n in enumerate(names, 1)]),
                         [3, 2, 10, 4, 1, 5, 6, 7, 8, 9])

    def test_localities_stay_grouped_and_unnumbered_bases_follow(self):
        rows = [(1, '昆明安宁三号温泉基地', 'hotel'), (2, '腾冲玛御谷温泉基地', 'hotel'),
                (3, '昆明二号基地', 'hotel'), (4, '昆明安宁一号基地', 'hotel'),
                (5, '昆明一号基地', 'hotel'), (6, '腾冲二号基地', 'hotel')]
        self.assertEqual(self.sorted_ids(rows), [5, 3, 4, 1, 6, 2])

    def test_non_travel_and_mixed_results_keep_original_id_order(self):
        for kinds in [('education', 'education', 'education'), ('hotel', 'education', 'hotel'),
                      ('hotel', None, 'hotel')]:
            self.assertEqual(self.sorted_ids([(i, '元江%s号基地' % n, kind) for i, n, kind in
                                             zip([1, 2, 3], ['三', '一', '二'], kinds)]), [3, 2, 1])

    def test_sorting_precedes_limit_and_has_stable_ties(self):
        rows = [(1, '元江三号基地', 'hotel'), (2, '元江一号基地', 'hotel'),
                (3, '元江二号基地', 'hotel'), (4, '元江二号基地', 'hotel')]
        self.assertEqual(self.sorted_ids(rows, 2), [2, 4])
        self.assertEqual(self.sorted_ids(rows, 2, 2), [3, 1])


if __name__ == '__main__':
    unittest.main()
