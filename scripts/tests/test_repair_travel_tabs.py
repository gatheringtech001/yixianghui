import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from repair_travel_tabs import build_desired, build_transaction_sql


def tab(section_id, section_name, content, sort_order):
    return {
        "section_id": section_id, "section_name": section_name, "content": content,
        "sort_order": sort_order, "min_content_length": 250,
    }


class RepairTravelTabsTest(unittest.TestCase):
    def test_reuses_existing_rows_and_deletes_non_target_tabs(self):
        catalog = {"products": [
            {
                "name": "基地甲", "slug": "slug-a",
                "tabs": [tab("basic", "基本特色", "甲特色", 1), tab("policy", "政策", "甲政策", 2)],
            },
            {
                "name": "基地乙", "slug": "slug-b",
                "tabs": [tab("basic", "基本特色", "乙特色", 1), tab("policy", "政策", "乙政策", 2)],
            },
        ]}
        snapshot = {
            "goods": [
                {"goods_id": 1, "goods_name": "基地甲"},
                {"goods_id": 2, "goods_name": "基地乙"},
            ],
            "related": [
                {"id": 10, "goods_id": 1, "section_id": "kb_slug-a_1"},
                {"id": 11, "goods_id": 2, "section_id": "basic"},
                {"id": 12, "goods_id": 2, "section_id": "policy"},
                {"id": 13, "goods_id": 2, "section_id": "餐饮"},
            ],
            "related_max": 13,
        }

        desired = build_desired(catalog, snapshot)

        self.assertEqual([10, 11, 12], [row["id"] for row in desired["updates"]])
        self.assertEqual([14], [row["id"] for row in desired["inserts"]])
        self.assertEqual([13], [row["id"] for row in desired["deletes"]])
        self.assertEqual("basic", desired["updates"][0]["section_id"])
        self.assertEqual("policy", desired["inserts"][0]["section_id"])

    def test_transaction_guards_goods_names_before_deleting_tabs(self):
        snapshot = {
            "goods": [{"goods_id": 1, "goods_name": "基地甲"}],
            "related": [{
                "id": 10, "goods_id": 1, "section_id": "旧", "section_name": "旧标签",
                "content": "旧内容", "sort_order": 3, "min_content_length": 250,
            }],
            "related_max": 10,
        }
        desired = {"updates": [], "inserts": [], "deletes": snapshot["related"]}

        sql = build_transaction_sql({"snapshot": snapshot, "desired": desired})

        self.assertIn("goods_id=1 AND BINARY `goods_name` <=> BINARY", sql)
        self.assertIn("DELETE FROM app_goods_related WHERE id IN (10)", sql)


if __name__ == "__main__":
    unittest.main()
