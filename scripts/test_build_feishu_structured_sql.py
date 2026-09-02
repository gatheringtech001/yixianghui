import gzip
import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from build_feishu_structured_sql import build
from feishu_structured_model import TARGET_TABLES, column_name


EXPORT = Path("/Users/kevin/.codex/yixianghui/imports/feishu-all-20260902.json.gz")


class StructuredSqlTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        with gzip.open(EXPORT, "rt", encoding="utf-8") as source:
            cls.export = json.load(source)
        cls.sql = build(cls.export)

    def test_all_tables_and_records_are_covered(self):
        self.assertEqual(15, len(TARGET_TABLES))
        self.assertEqual(3246, sum(len(t["records"]) for b in self.export["bases"] for t in b["tables"]))
        for target in TARGET_TABLES.values():
            self.assertIn(f"CREATE TABLE IF NOT EXISTS `{target}`", self.sql)

    def test_all_fields_have_column_or_structured_child_storage(self):
        field_count = sum(len(t["fields"]) for b in self.export["bases"] for t in b["tables"])
        self.assertEqual(310, field_count)
        for base in self.export["bases"]:
            for table in base["tables"]:
                for field in table["fields"]:
                    self.assertIn(column_name(field["field_id"]), self.sql)

    def test_relations_are_resolved_after_all_tables_load(self):
        self.assertIn("UPDATE app_feishu_business_relation rel JOIN `app_travel_customer_profile`", self.sql)
        self.assertIn("unresolved_relations", self.sql)

    def test_core_business_domains_receive_canonical_rows(self):
        for table in ("app_customer", "app_goods_order", "app_customer_income", "app_activity", "app_consultant"):
            self.assertIn(table, self.sql)
        self.assertIn("app_customer_feishu_source", self.sql)
        self.assertIn("tmp_feishu_customer_match", self.sql)
        self.assertIn("canonical_status", self.sql)


if __name__ == "__main__":
    unittest.main()
