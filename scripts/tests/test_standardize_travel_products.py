import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from standardize_travel_products import (
    _room_identity, _verified_existing, build_desired, build_transaction_sql,
)


class StandardizeTravelProductsTest(unittest.TestCase):
    def setUp(self):
        self.catalog = {"products": [{
            "name": "测试基地", "description": "知识库简介",
            "gallery": ["/profile/cover.jpg", "/profile/two.jpg"],
            "tabs": [
                {"section_id": "basic", "section_name": "基地特色", "content": "<p>特色</p>"},
                {"section_id": "policy", "section_name": "入住须知", "content": "<p>须知</p>"},
            ],
        }]}
        self.snapshot = {
            "goods": [{"goods_id": 1, "goods_name": "测试基地", "description": "旧",
                       "goods_cover": "旧图", "goods_images": "旧图"}],
            "related": [
                {"id": 10, "goods_id": 1, "section_id": "basic", "section_name": "基本特色", "content": "旧"},
                {"id": 11, "goods_id": 1, "section_id": "policy", "section_name": "入住须知", "content": "旧"},
            ],
            "options": [{"option_id": 20, "goods_id": 1, "sku_id": 30,
                         "option_value": "/profile/old.jpg"}],
        }

    def test_builds_six_slot_display_updates_without_touching_prices(self):
        rooms = [{"option_id": 20, "goods_id": 1, "sku_id": 30,
                  "option_value": "/profile/placeholder-twin-clean-v2.jpg",
                  "source_type": "placeholder"}]
        desired = build_desired(self.catalog, self.snapshot, rooms)

        self.assertEqual("知识库简介", desired["goods_updates"][0]["description"])
        self.assertEqual(["基地特色", "入住须知"],
                         [row["section_name"] for row in desired["related_updates"]])
        self.assertEqual(rooms, desired["room_updates"])
        self.assertNotIn("price", str(desired))

    def test_transaction_is_guarded_and_updates_only_display_fields(self):
        desired = build_desired(self.catalog, self.snapshot, [])
        sql = build_transaction_sql({"snapshot": self.snapshot, "desired": desired})

        self.assertIn("START TRANSACTION", sql)
        self.assertIn("description=", sql)
        self.assertIn("section_name=", sql)
        self.assertNotIn("DELETE", sql)
        self.assertNotIn("price=", sql)

    def test_only_explicit_legacy_and_reviewed_paths_are_preserved(self):
        self.assertTrue(_verified_existing(31, "/profile/upload/2026/04/28/room1.jpg"))
        self.assertFalse(_verified_existing(53, "/profile/upload/2026/08/13/base.jpg"))
        self.assertEqual(("豪华标间", "2人一间"), _room_identity("豪华标间（2人一间）"))


if __name__ == "__main__":
    unittest.main()
