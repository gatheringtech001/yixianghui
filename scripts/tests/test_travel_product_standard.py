import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from travel_product_standard import build_product, validate_product


class TravelProductStandardTest(unittest.TestCase):
    def setUp(self):
        texts = [
            "四钻标准每日打扫清洁每日免费温泉牛奶",
            "免费接送湖泉边核心位置",
            "2026年价格",
            "包含三餐：免费接站（7天及以上免费接站）",
            "舒适标间/大床房：",
            "两人一间：815元/人/7天",
            "两人一间：1580元/人/15天",
            "两人一间：2990元/人/30天",
            "舒适大床房：",
            "单人一间：1500元/人/7天",
            "单人一间：2900元/人/15天",
            "单人一间：5660元/人/30天",
            "雅致标间：",
            "两人一间：896元/人/7天",
            "两人一间：1750元/人/15天",
            "两人一间：3340元/人/30天",
            "豪华大床房：",
            "单人包房：1790元/人/7天",
            "两人一间：955元/人/7天",
            "单人包房：3550元/人/15天",
            "两人一间：1845元/人/15天",
            "单人包房：6780元/人/30天",
            "两人一间：3580元/人/30天",
            "豪华标间",
            "两人一间：1040元/人/7天",
            "两人一间：2040元/人/15天",
            "两人一间：3940元/人/30天",
            "每周卫生清洗2次，分别为周一周五。",
            "餐食标准为自助餐四荤四素，不可退餐，退差价。",
            "酒店接站车辆5座车30元单程每次。",
            "基地位于弥勒湖泉商业圈，毗邻湖泉生态公园和红河水乡。",
            "基地为四星级温泉酒店标准，107间房。基地内提供免费茶室、健身房、棋牌室及温泉设施。基地提供一日三餐。",
            "温泉对糖尿病、痛风、三高等慢性疾病有显著疗效。",
            "舒适双床房间（1张2米特大床 1张1.2米双人床 22m²）",
            "儿童收费：3岁以下免费，3-6岁每天餐费10元。",
            "入住前3天无损取消，入住前3天内扣除房费20%。",
            "弥勒东风韵艺术小镇是一座集葡萄文化、自然风光、人文旅游为一体的特色小镇。",
        ]
        self.items = [{"kind": "paragraph", "text": text} for text in texts]
        self.document = {
            "slug": "demo-miler-lvbao",
            "title": "弥勒二号温泉基地（绿宝基地）",
            "city": "弥勒",
            "source_url": "https://example.com/demo",
            "source_updated_at": "2026-07-29T09:06:26.000Z",
            "docx_sha256": "a" * 64,
            "assets": [
                {"index": index, "status": "downloaded", "width": 1200,
                 "height": 800, "sha256": f"{index:064x}"}
                for index in range(1, 15)
            ],
            "video_urls": [f"https://example.com/{index}.mp4" for index in range(7)],
        }

    def test_builds_traceable_review_required_product(self):
        product = build_product(
            self.document, self.items, generated_at="2026-08-15T00:00:00+00:00"
        )

        self.assertEqual("travel_product.v1", product["schema_version"])
        self.assertEqual("815.00", product["pricing"]["starting_price"])
        self.assertIn("位于弥勒湖泉商业圈", product["display"]["summary"])
        self.assertTrue(product["display"]["summary_source_refs"])
        self.assertEqual(6, product["pricing"]["room_package_count"])
        self.assertEqual(18, product["pricing"]["offer_count"])
        self.assertTrue(product["quality"]["checks"]["source_traceability"])
        self.assertTrue(product["quality"]["checks"]["content_sections_complete"])
        self.assertEqual("review_required", product["quality"]["status"])
        self.assertEqual(
            ["基地特色", "入住须知"],
            [row["section_name"] for row in product["content_sections"]],
        )
        page = product["page_display"]
        self.assertEqual(
            ["introduction", "mainImages", "roomImages", "roomPricePackages",
             "details", "checkInNotice"],
            list(page),
        )
        self.assertEqual(product["display"]["summary"], page["introduction"])
        self.assertEqual(6, len(page["mainImages"]))
        self.assertEqual(6, len(page["roomImages"]))
        self.assertTrue(all(row["image"] for row in page["roomImages"]))
        self.assertTrue(all(row["sourceType"] == "placeholder" for row in page["roomImages"]))
        self.assertEqual(6, len(page["roomPricePackages"]))
        first_package = page["roomPricePackages"][0]["packages"][0]
        self.assertEqual("7天", first_package["duration"])
        self.assertEqual("815.00", first_package["price"])
        self.assertIsNone(first_package["nights"])
        self.assertIsNone(first_package["averagePerNight"])
        self.assertEqual(1, len(page["details"]))
        self.assertEqual("基地特色", page["details"][0]["title"])
        self.assertEqual("入住须知", page["checkInNotice"]["title"])
        codes = {row["code"] for row in product["quality"]["issues"]}
        self.assertTrue({
            "CONFLICT_HOUSEKEEPING", "CONFLICT_PICKUP_FEE", "CONFLICT_RATING",
            "CONFLICT_MEAL_REFUND", "AMBIGUOUS_NIGHTS", "SUSPICIOUS_BED_TYPE",
            "WITHHELD_MEDICAL_CLAIMS",
        }.issubset(codes))
        content = json.dumps(product["content_sections"], ensure_ascii=False)
        self.assertNotIn("显著疗效", content)
        validate_product(product)

    def test_rejects_starting_price_not_backed_by_an_offer(self):
        product = build_product(
            self.document, self.items, generated_at="2026-08-15T00:00:00+00:00"
        )
        product["pricing"]["starting_price"] = "999.00"

        with self.assertRaisesRegex(ValueError, "Starting price"):
            validate_product(product)

    def test_rejects_page_display_drift(self):
        product = build_product(
            self.document, self.items, generated_at="2026-08-15T00:00:00+00:00"
        )
        product["page_display"]["introduction"] = "错误简介"

        with self.assertRaisesRegex(ValueError, "Page introduction"):
            validate_product(product)

    def test_uses_only_room_image_adjacent_to_matching_room_text(self):
        source_url = "https://example.com/room.jpg"
        self.document["assets"][0]["url"] = source_url
        items = self.items + [
            {"kind": "paragraph", "text": "舒适双床房间（2张床）"},
            {"kind": "image", "src": source_url},
        ]

        product = build_product(self.document, items)

        self.assertTrue(product["page_display"]["roomImages"][0]["image"].endswith("-001.jpg"))
        self.assertEqual("real", product["page_display"]["roomImages"][0]["sourceType"])
        self.assertEqual("placeholder", product["page_display"]["roomImages"][1]["sourceType"])

    def test_uses_verified_mile_room_override_only_for_matching_room(self):
        self.document["slug"] = "ptzlh413322upmyo"

        product = build_product(self.document, self.items)
        images = {(row["roomType"], row["occupancy"]): row
                  for row in product["page_display"]["roomImages"]}

        deluxe_twin = images[("豪华标间", "2人一间")]
        self.assertEqual("real", deluxe_twin["sourceType"])
        self.assertTrue(deluxe_twin["image"].endswith("mile-deluxe-twin-real.jpg"))
        self.assertEqual("placeholder", images[("豪华大床房", "2人一间")]["sourceType"])


if __name__ == "__main__":
    unittest.main()
