from __future__ import annotations

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from yxh_policy import PolicyError, check_content_change, sql_literal, validate_read_sql


class ReadPolicyTests(unittest.TestCase):
    def test_adds_limit_to_select(self) -> None:
        sql = validate_read_sql("SELECT goods_id FROM app_goods", production=False)
        self.assertEqual(sql, "SELECT goods_id FROM app_goods LIMIT 200;")

    def test_rejects_write(self) -> None:
        with self.assertRaises(PolicyError):
            validate_read_sql("UPDATE app_goods SET status='0'", production=False)

    def test_rejects_stacked_statement(self) -> None:
        with self.assertRaises(PolicyError):
            validate_read_sql("SELECT 1; SELECT 2", production=False)

    def test_rejects_production_select_star(self) -> None:
        with self.assertRaises(PolicyError):
            validate_read_sql("SELECT * FROM app_goods LIMIT 1", production=True)

    def test_rejects_sensitive_column_by_default(self) -> None:
        with self.assertRaises(PolicyError):
            validate_read_sql("SELECT mobile FROM app_consultant", production=False)

    def test_allows_sensitive_column_when_explicit(self) -> None:
        sql = validate_read_sql(
            "SELECT mobile FROM app_consultant LIMIT 1",
            production=True,
            include_sensitive=True,
        )
        self.assertEqual(sql, "SELECT mobile FROM app_consultant LIMIT 1;")


class ContentPolicyTests(unittest.TestCase):
    def test_empty_string_literal_is_valid(self) -> None:
        self.assertEqual(sql_literal(""), "''")

    def test_non_finite_number_is_rejected(self) -> None:
        with self.assertRaises(PolicyError):
            sql_literal(float("nan"))

    def test_rejects_order_table(self) -> None:
        with self.assertRaises(PolicyError):
            check_content_change("app_goods_order", "update", {"status"}, "order_id")

    def test_rejects_primary_key_change(self) -> None:
        with self.assertRaises(PolicyError):
            check_content_change("app_goods", "update", {"goods_id"}, "goods_id")


if __name__ == "__main__":
    unittest.main()
