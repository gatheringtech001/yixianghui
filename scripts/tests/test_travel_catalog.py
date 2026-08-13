import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from travel_asset_policy import COVER_ASSET_INDEX, REJECTED_ASSET_SHA256
from travel_catalog import _bookable_quotes, _gallery, build_content


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

    def test_rejects_known_yellow_icons_from_content_and_gallery(self):
        rejected_hashes = iter(REJECTED_ASSET_SHA256)
        document = {
            "slug": "demo", "title": "测试基地",
            "assets": [
                {
                    "index": 1, "url": "https://x/diamond.png", "status": "downloaded",
                    "sha256": next(rejected_hashes), "width": 82, "height": 91,
                },
                {
                    "index": 2, "url": "https://x/pin.png", "status": "downloaded",
                    "sha256": next(rejected_hashes), "width": 142, "height": 214,
                },
                {
                    "index": 3, "url": "https://x/base.jpg", "status": "downloaded",
                    "sha256": "base-photo", "width": 1200, "height": 800,
                },
            ],
        }
        items = [
            {"kind": "image", "src": "https://x/diamond.png", "alt": ""},
            {"kind": "image", "src": "https://x/pin.png", "alt": ""},
            {"kind": "image", "src": "https://x/base.jpg", "alt": "基地实景"},
        ]
        sections, used = build_content(items, document)
        content = "".join(sections)
        self.assertNotIn("diamond.png", content)
        self.assertNotIn("pin.png", content)
        self.assertEqual({3}, used)
        with self.assertRaisesRegex(ValueError, "人工封面"):
            _gallery(document, set())

    def test_human_reviewed_cover_is_always_first(self):
        document = {
            "slug": "demo", "title": "测试基地",
            "assets": [
                {"index": 1, "status": "downloaded", "sha256": "map", "width": 1600, "height": 900},
                {"index": 3, "status": "downloaded", "sha256": "base", "width": 900, "height": 1200},
            ],
        }
        COVER_ASSET_INDEX["demo"] = 3
        try:
            self.assertEqual([3, 1], _gallery(document, set()))
            self.assertEqual([3, 1], _gallery(document, {3}))
        finally:
            del COVER_ASSET_INDEX["demo"]

    def test_ambiguous_duplicate_price_is_not_bookable(self):
        quotes = [
            {"room": "标间", "duration": "8天7晚", "price": "745.00", "unit": "人"},
            {"room": "标间", "duration": "8天7晚", "price": "885.00", "unit": "人"},
        ]
        self.assertEqual([], _bookable_quotes(quotes))


if __name__ == "__main__":
    unittest.main()
