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

    def test_travel_and_education_views_are_separate(self):
        source = VIEW.read_text(encoding="utf-8")
        self.assertIn('name="travel"', source)
        self.assertIn('name="education"', source)
        self.assertIn("businessType: 'travel'", source)

    def test_backend_filters_the_two_business_types(self):
        source = MAPPER.read_text(encoding="utf-8")
        self.assertIn("businessType == 'travel'", source)
        self.assertIn("businessType == 'education'", source)
        self.assertIn("goods.goods_type", source)


if __name__ == "__main__":
    unittest.main()
