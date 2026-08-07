import sys
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))

from education_catalog import (  # noqa: E402
    _row_predicate, build_transaction_sql, desired_rows, load_catalog, summarize,
)


class EducationCatalogTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.catalog_path = SCRIPTS / "data/education-2026-fall/catalog.json"
        cls.catalog = load_catalog(cls.catalog_path)
        cls.rows = desired_rows(cls.catalog)

    def test_catalog_has_complete_course_graph(self):
        self.assertEqual(5, len(self.rows["app_goods_category"]))
        self.assertEqual(15, len(self.rows["app_goods"]))
        self.assertEqual(15, len(self.rows["app_goods_education_ext"]))
        self.assertEqual(45, len(self.rows["app_goods_related"]))
        self.assertEqual(set(range(38, 53)), {row["goods_id"] for row in self.rows["app_goods"]})
        self.assertEqual(17, self.rows["app_goods"][0]["stock"])
        self.assertTrue(all(row["vip_price"] == row["price"] for row in self.rows["app_goods"]))

    def test_every_course_has_three_visible_sections_and_cover(self):
        counts = {}
        for row in self.rows["app_goods_related"]:
            counts[row["goods_id"]] = counts.get(row["goods_id"], 0) + 1
        self.assertEqual({goods_id: 3 for goods_id in range(38, 53)}, counts)
        for row in self.rows["app_goods"]:
            self.assertTrue(row["goods_cover"].startswith("/profile/upload/"))
            self.assertEqual(row["goods_cover"], row["goods_images"])

    def test_transaction_is_zero_delete_and_matches_preview_counts(self):
        snapshot = {"tables": {}}
        existing = {
            "app_goods_category": self.rows["app_goods_category"][:3],
            "app_goods": self.rows["app_goods"][:1],
            "app_goods_education_ext": self.rows["app_goods_education_ext"][:1],
            "app_goods_related": self.rows["app_goods_related"][:3],
        }
        for table, rows in existing.items():
            snapshot["tables"][table] = {"rows": rows}
        sql = build_transaction_sql(self.catalog, snapshot)
        self.assertNotIn("DELETE FROM", sql)
        self.assertIn("START TRANSACTION;", sql)
        self.assertIn("CREATE TEMPORARY TABLE yxh_education_guard", sql)
        self.assertIn("BINARY `category_name` <=> BINARY", sql)
        self.assertTrue(sql.rstrip().endswith("COMMIT;"))
        impact = summarize(self.catalog)["row_impact"]
        self.assertEqual(3, impact["category_updates"])
        self.assertEqual(42, impact["related_inserts"])

    def test_transaction_guard_ignores_database_managed_timestamps(self):
        predicate = _row_predicate("app_goods", {
            "goods_name": "水彩绘画",
            "create_time": "2026-07-10 11:15:36.000000",
            "update_time": "2026-07-29 18:46:00.000000",
        })
        self.assertIn("goods_name", predicate)
        self.assertNotIn("create_time", predicate)
        self.assertNotIn("update_time", predicate)


if __name__ == "__main__":
    unittest.main()
