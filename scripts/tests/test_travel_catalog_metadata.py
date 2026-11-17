"""Overview list projection: one SQL row per goods, no detail/SKU N+1 calls."""
import json
import os
import unittest
import xml.etree.ElementTree as ET
from scripts.tests.test_travel_catalog_order import MAPPER, fragment
from scripts.tests import test_travel_catalog_order as order_tests

class TravelCatalogMetadataContractTest(unittest.TestCase):
    def test_only_list_mapping_projects_one_optional_hotel_overview(self):
        root = ET.parse(MAPPER).getroot()
        self.assertEqual(root.find("select[@id='selectAppGoodsList']").attrib['resultMap'], 'AppGoodsListResult')
        self.assertEqual(root.find("select[@id='selectAppGoodsByGoodsId']").attrib['resultMap'], 'AppGoodsResult')
        collection = root.find("resultMap[@id='AppGoodsListResult']/collection")
        self.assertEqual(collection.attrib['property'], 'features')
        self.assertEqual(collection.attrib['columnPrefix'], 'catalog_')
        self.assertEqual(collection.attrib['notNullColumn'], 'id')
        self.assertNotIn('select', collection.attrib)
        sql = fragment('selectAppGoodsListVo')
        self.assertIn("on g.goods_type = 'hotel'", sql)
        self.assertIn('overview.goods_id = g.goods_id', sql)
        self.assertIn('order by overview.sort_order asc, overview.id asc limit 1', sql)
        self.assertIn("overview.section_name like '%基地概况%'", sql)
        self.assertNotIn('${', sql)

@unittest.skipUnless(os.environ.get('YXH_SORT_MYSQL_ENV'), 'Set YXH_SORT_MYSQL_ENV for read-only native tests')
class TravelCatalogMetadataMySQLTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        order_tests.TravelCatalogOrderMySQLTest.setUpClass.__func__(cls)

    def test_live_projection_preserves_every_goods_id_and_price(self):
        sql = "SELECT JSON_OBJECT('id',goods_id,'type',goods_type,'price',price,'vipPrice',vip_price,'featureId',catalog_id,'featureGoodsId',catalog_goods_id,'section',catalog_section_name,'content',catalog_content) FROM (%s) projected ORDER BY goods_id LIMIT 200" % fragment('selectAppGoodsListVo')
        rows = [json.loads(row) for row in self.run_query(sql).splitlines()]
        baseline = [json.loads(row) for row in self.run_query("SELECT JSON_OBJECT('id',goods_id,'type',goods_type,'price',price,'vipPrice',vip_price) FROM app_goods ORDER BY goods_id LIMIT 200").splitlines()]
        self.assertEqual([{k:r[k] for k in ('id','type','price','vipPrice')} for r in rows], baseline)
        self.assertEqual(len({r['id'] for r in rows}), len(rows))
        for row in rows:
            if row['type'] != 'hotel':
                self.assertIsNone(row['featureId'])
            elif row['featureId'] is not None:
                self.assertEqual(row['featureGoodsId'], row['id'])
                self.assertIn('基地概况', row['section'])
        sample = next(r for r in rows if r['id'] == 56)
        for value in ['1891', '2019', '市中心 / 近商圈 / 花园庭院 / 全年不加价']:
            self.assertIn(value, sample['content'])

if __name__ == '__main__':
    unittest.main()
