import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from travel_catalog import _bookable_quotes, build_content


class TravelCatalogTest(unittest.TestCase):
    def test_redacts_phone_text_and_adjacent_image(self):
        document = {
            "slug": "demo", "title": "测试基地",
            "assets": [{"index": 1, "url": "https://x/qr.jpg", "status": "downloaded"}],
        }
        items = [
            {"kind": "paragraph", "text": "联系电话：13800138000"},
            {"kind": "image", "src": "https://x/qr.jpg", "alt": "二维码"},
            {"kind": "paragraph", "text": "正常基地介绍"},
        ]
        sections, used = build_content(items, document)
        content = "".join(sections)
        self.assertNotIn("13800138000", content)
        self.assertNotIn("qr.jpg", content)
        self.assertIn("正常基地介绍", content)
        self.assertEqual(set(), used)

    def test_deduplicates_images(self):
        document = {
            "slug": "demo", "title": "测试基地",
            "assets": [{"index": 1, "url": "https://x/a.jpg", "status": "downloaded"}],
        }
        items = [{"kind": "image", "src": "https://x/a.jpg", "alt": ""}] * 2
        sections, used = build_content(items, document)
        self.assertEqual(1, "".join(sections).count("<img"))
        self.assertEqual({1}, used)

    def test_ambiguous_duplicate_price_is_not_bookable(self):
        quotes = [
            {"room": "标间", "duration": "8天7晚", "price": "745.00", "unit": "人"},
            {"room": "标间", "duration": "8天7晚", "price": "885.00", "unit": "人"},
        ]
        self.assertEqual([], _bookable_quotes(quotes))


if __name__ == "__main__":
    unittest.main()
