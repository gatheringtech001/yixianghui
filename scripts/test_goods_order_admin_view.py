#!/usr/bin/env python3
"""Regression checks for the split goods-order administration view."""

from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[1]
VIEW = ROOT / "ruoyi-ui/src/views/system/app_goods_order/index.vue"
MAPPER = ROOT / "ruoyi-system/src/main/resources/mapper/system/AppGoodsOrderMapper.xml"


class GoodsOrderAdminViewTest(unittest.TestCase):
    def test_table_expands_all_rows_and_uses_payment_status_dictionary(self):
        source = VIEW.read_text(encoding="utf-8")
        self.assertNotIn('max-height="400"', source)
        self.assertIn("'order_pay_status'", source)
        self.assertIn(':options="dict.type.order_pay_status"', source)

    def test_tabs_reuse_mini_program_services_and_category_tree(self):
        source = VIEW.read_text(encoding="utf-8")
        self.assertIn('v-for="entry in serviceEntries"', source)
        self.assertIn('listApp_goods_category()', source)
        self.assertIn("this.handleTree(this.categoryRows, 'categoryId', 'parentId')", source)
        self.assertIn('label="商品分类"', source)
        self.assertNotIn('dict.type.goods_type', source)
        self.assertIn('this.$router.push(entry.route)', source)
        self.assertIn("orderScope: 'all'", source)
        self.assertEqual(source.count('exportQuery(goodsOrderQuery(this.queryParams, this.orderScope, this.serviceEntries))'), 2)
        self.assertIn('if (request !== this.listRequest) return', source)

    def test_backend_filters_the_two_business_types(self):
        source = MAPPER.read_text(encoding="utf-8")
        self.assertIn("businessType == 'travel'", source)
        self.assertIn("businessType == 'education'", source)
        self.assertIn("goods.goods_type", source)


if __name__ == "__main__":
    unittest.main()
