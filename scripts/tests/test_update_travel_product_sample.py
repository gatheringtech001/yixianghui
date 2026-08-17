import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from update_travel_product_sample import build_desired, build_transaction_sql


class UpdateTravelProductSampleTest(unittest.TestCase):
    def setUp(self):
        specs = [
            ("舒适标间/大床房", "2人一间", ("815.00", "1580.00", "2990.00")),
            ("雅致标间", "2人一间", ("896.00", "1750.00", "3340.00")),
            ("豪华大床房", "2人一间", ("955.00", "1845.00", "3580.00")),
            ("豪华标间", "2人一间", ("1040.00", "2040.00", "3940.00")),
            ("舒适大床房", "1人一间", ("1500.00", "2900.00", "5660.00")),
            ("豪华大床房", "1人包房", ("1790.00", "3550.00", "6780.00")),
        ]
        room_prices = [{"roomType": room, "occupancy": occupancy, "packages": [
            {"duration": f"{days}天", "days": days, "mealPlan": "含三餐", "price": price}
            for days, price in zip((7, 15, 30), prices)
        ]} for room, occupancy, prices in specs]
        self.product = {
            "display": {"tags": [{"label": "温泉"}, {"label": "含三餐"}]},
            "pricing": {"starting_price": "815.00"},
            "page_display": {
                "introduction": "基地位于弥勒湖泉商业圈。",
                "mainImages": [f"/profile/sample-{index}.jpg" for index in range(6)],
                "roomImages": [{"roomType": room, "occupancy": occupancy,
                                "image": "/profile/upload/2026/08/17/travel-room-v1/placeholder-twin.jpg",
                                "sourceType": "placeholder", "placeholderType": "twin"}
                               for room, occupancy, _ in specs],
                "roomPricePackages": room_prices,
                "details": [{"title": "基地特色", "content": "<p>基地特色</p>"}],
                "checkInNotice": {"title": "入住须知", "content": "<p>须知</p>"},
            },
        }
        self.snapshot = {
            "maxima": {"sku_id": 300, "option_id": 900, "related_id": 500},
            "skus": [{"sku_id": value} for value in range(225, 233)],
            "options": [{"option_id": value} for value in range(749, 777)],
            "related": [
                {"id": 394, "section_name": "基地概览"},
                {"id": 501, "section_name": "餐饮"},
                {"id": 502, "section_name": "交通接送"},
                {"id": 503, "section_name": "温泉与设施"},
                {"id": 504, "section_name": "周边景点"},
                {"id": 448, "section_name": "入住须知"},
            ],
        }

    def test_builds_six_room_groups_with_explicit_room_images(self):
        desired = build_desired(self.product, self.snapshot)

        self.assertEqual(24, len(desired["skus"]))
        self.assertEqual(72, len(desired["options"]))
        self.assertEqual(2, len(desired["related"]))
        self.assertEqual(4, len(desired["related_delete_ids"]))
        self.assertEqual("815.00", desired["goods"]["price"])
        self.assertTrue(all(row["price"] == "0.00" for row in desired["skus"] if row["sku_type"] == "200"))
        self.assertNotIn("301", {row["option_type"] for row in desired["options"]})
        self.assertEqual(6, sum(row["option_type"] == "305" for row in desired["options"]))
        self.assertEqual(["基地特色", "入住须知"],
                         [row["section_name"] for row in desired["related"]])

    def test_transaction_deletes_only_superseded_detail_rows(self):
        desired = build_desired(self.product, self.snapshot)
        sql = build_transaction_sql(desired, self.snapshot)

        self.assertIn("START TRANSACTION", sql)
        self.assertIn("UPDATE app_goods_sku SET status='0'", sql)
        self.assertIn("UPDATE app_goods_sku_option SET status='0'", sql)
        self.assertIn("DELETE FROM app_goods_related", sql)
        self.assertNotIn("DELETE FROM app_goods_sku", sql)
        self.assertNotIn("DELETE FROM app_goods_sku_option", sql)
        self.assertTrue(sql.rstrip().endswith("COMMIT;"))


if __name__ == "__main__":
    unittest.main()
