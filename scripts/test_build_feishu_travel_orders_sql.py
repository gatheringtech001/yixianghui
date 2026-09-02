#!/usr/bin/env python3
import gzip
import json
import tempfile
import unittest
from pathlib import Path

from build_feishu_travel_orders_sql import build


class BuildTravelOrdersSqlTest(unittest.TestCase):
    def test_builds_one_formal_order_per_feishu_record(self):
        export = {
            "bases": [{
                "key": "travel",
                "tables": [{
                    "name": "预订订单表",
                    "table_id": "orders",
                    "records": [
                        {"record_id": "rec1", "fields": {
                            "订单编号": "LJ1", "关联基地": [{"record_ids": ["base1"], "text": "昆明一号基地"}],
                            "消费金额": "100", "同行人数": 2, "房间数": 1, "房型": ["标准双人间"],
                            "入住日期": 1780000000000, "离店日期": 1780086400000,
                            "订单状态": "已确认", "渠道": "云旅"
                        }},
                        {"record_id": "rec2", "fields": {
                            "订单编号": "LJ2", "关联基地": [{"text": "未匹配基地"}],
                            "消费金额": "200", "订单状态": "已取消"
                        }}
                    ]
                }]
            }]
        }
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "export.json.gz"
            with gzip.open(source, "wt", encoding="utf-8") as handle:
                json.dump(export, handle, ensure_ascii=False)
            sql, report = build(source)

        self.assertEqual(2, report["orders"])
        self.assertEqual(1, report["matched_goods"])
        self.assertEqual(1, report["unmatched_goods"])
        self.assertEqual(2, sql.count("INSERT INTO app_goods_order"))
        self.assertIn("feishu_history", sql)
        self.assertIn("0x72656331", sql)
        self.assertIn("travel_base_name", sql)


if __name__ == "__main__":
    unittest.main()
