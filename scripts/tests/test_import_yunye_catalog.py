import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from import_yunye_catalog import build_catalog, build_sql


class ImportYunyeCatalogTest(unittest.TestCase):
    def test_maps_single_spec_to_published_and_multi_spec_to_draft(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)
            (source / "details").mkdir()
            (source / "classes").mkdir()
            self.write_json(source / "products.json", {
                "complete": True, "truncated": False, "unique_count": 2,
                "items": [{"id": 1}, {"id": 2}],
            })
            self.write_json(source / "categories.json", {
                "code": 200, "data": [{"id": 10, "cate_name": "昆明集"}],
            })
            self.write_json(source / "classes/10.json", {
                "complete": True, "unique_count": 2,
                "items": [{"id": 1}, {"id": 2}],
            })
            self.write_detail(source, 1, [{"spec_value_name": "一盒"}])
            self.write_detail(source, 2, [
                {"spec_value_name": "小盒"}, {"spec_value_name": "大盒"},
            ])

            catalog = build_catalog(source)

        self.assertEqual(["昆明集"], catalog["classes"])
        self.assertEqual(["1", "0"], [row["status"] for row in catalog["goods"]])
        self.assertEqual("YUNYE:1", catalog["goods"][0]["specifications"])
        self.assertIn("多规格", catalog["goods"][1]["draft_reason"])

    def test_sql_contains_exact_aggregate_counts(self):
        catalog = {
            "classes": ["昆明集"],
            "goods": [{
                "category_name": "昆明集", "goods_name": "鲜花饼", "goods_cover": "https://x/a.jpg",
                "goods_images": "https://x/a.jpg", "description": "一盒", "tags": "云南好物,云野集",
                "price": "10.00", "vip_price": "10.00", "unit": "件", "specifications": "YUNYE:1",
                "stock": 2, "goods_type": "online", "attr_values": "[]", "content": "", "express_fee": "0",
                "weight": 0, "view_count": 0, "sale_count": 0, "status": "1",
            }],
        }

        sql = build_sql(catalog)

        self.assertEqual(1, sql.count("INSERT INTO app_goods_category"))
        self.assertEqual(1, sql.count("INSERT INTO app_goods ("))
        self.assertIn("UPDATE app_goods_category", sql)

    def test_keeps_storefront_information_pages_as_drafts(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)
            (source / "details").mkdir()
            (source / "classes").mkdir()
            self.write_json(source / "products.json", {
                "complete": True, "truncated": False, "unique_count": 1,
                "items": [{"id": 16885}],
            })
            self.write_json(source / "categories.json", {
                "code": 200, "data": [{"id": 10, "cate_name": "other"}],
            })
            self.write_json(source / "classes/10.json", {
                "complete": True, "unique_count": 1,
                "items": [{"id": 16885}],
            })
            self.write_detail(source, 16885, [])

            catalog = build_catalog(source)

        self.assertEqual("0", catalog["goods"][0]["status"])
        self.assertEqual("云野集说明页，不可售卖", catalog["goods"][0]["draft_reason"])

    def write_detail(self, source, goods_id, specs):
        self.write_json(source / f"details/{goods_id}.json", {
            "code": 200,
            "data": {
                "goods": {
                    "id": goods_id, "goods_name": f"商品{goods_id}", "goods_type": 0,
                    "pic": "https://x/a.jpg", "pic_group": '["https://x/a.jpg"]',
                    "instruction": "说明", "description": "<p>详情</p>", "price": "10.00",
                    "stock": 2, "shipping_fee": "0.00", "weight": 0, "click": 0,
                    "sales_volume": 0,
                },
                "spec_relate": specs,
            },
        })

    @staticmethod
    def write_json(path, value):
        path.write_text(json.dumps(value, ensure_ascii=False), encoding="utf-8")


if __name__ == "__main__":
    unittest.main()
