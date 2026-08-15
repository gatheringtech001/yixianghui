import sys
import unittest
from decimal import Decimal
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from travel_price_parser import (
    duration_values,
    extract_quotes,
    parse_table_quotes,
    parse_text_quotes,
)


class TravelPriceParserTest(unittest.TestCase):
    def test_parses_standard_price_table(self):
        rows = [
            ["2026年含三餐价格表"],
            ["普通标间两人一间", "8天7晚", "31天30晚"],
            ["745/人", "2980/人"],
        ]
        quotes = parse_table_quotes(rows)
        self.assertEqual(2, len(quotes))
        self.assertEqual((8, 7), (quotes[0].days, quotes[0].nights))
        self.assertEqual(Decimal("745"), quotes[0].price)
        self.assertEqual("人", quotes[0].unit)

    def test_parses_amount_before_duration(self):
        text = "两人一间：普通房：699元/人/7天；2680元/人/30天"
        quotes = parse_text_quotes(text)
        self.assertEqual([Decimal("699"), Decimal("2680")], [row.price for row in quotes])
        self.assertTrue(all(row.room.startswith("两人一间") for row in quotes))

    def test_parses_duration_before_amount(self):
        quotes = parse_text_quotes("湖景标间:5晚/人750元、7晚/人988元")
        self.assertEqual([Decimal("750"), Decimal("988")], [row.price for row in quotes])
        self.assertEqual((6, 5), (quotes[0].days, quotes[0].nights))

    def test_parses_labeled_package(self):
        quotes = parse_text_quotes("八天七晚体验套餐：仅799元/人")
        self.assertEqual(Decimal("799"), quotes[0].price)
        self.assertEqual((8, 7), (quotes[0].days, quotes[0].nights))

    def test_chinese_duration(self):
        self.assertEqual((6, 5), duration_values("六天五晚"))
        self.assertEqual((31, 30), duration_values("一个月"))

    def test_keeps_room_context_across_paragraphs(self):
        items = [
            {"kind": "paragraph", "text": "舒适标间/大床房："},
            {"kind": "paragraph", "text": "两人一间：815元/人/7天"},
            {"kind": "paragraph", "text": "两人一间：1580元/人/15天"},
            {"kind": "paragraph", "text": "两人一间：2990元/人/30天"},
            {"kind": "paragraph", "text": "豪华大床房："},
            {"kind": "paragraph", "text": "单人包房:1790元/人/7天"},
            {"kind": "paragraph", "text": "两人一间：955元/人/7天"},
        ]

        quotes = extract_quotes(items)

        self.assertEqual(5, len(quotes))
        self.assertEqual(
            [
                "舒适标间/大床房（2人一间）",
                "舒适标间/大床房（2人一间）",
                "舒适标间/大床房（2人一间）",
                "豪华大床房（1人包房）",
                "豪华大床房（2人一间）",
            ],
            [row.room for row in quotes],
        )
        self.assertEqual(("舒适标间/大床房", "2人一间"),
                         (quotes[0].room_type, quotes[0].occupancy))
        self.assertEqual((0, 1), quotes[0].source_refs)


if __name__ == "__main__":
    unittest.main()
