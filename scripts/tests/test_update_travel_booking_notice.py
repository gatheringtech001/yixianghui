import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from update_travel_booking_notice import build_transaction_sql


class UpdateTravelBookingNoticeTest(unittest.TestCase):
    def test_transaction_updates_only_notice_one_with_drift_guard(self):
        snapshot = {"notice_id": 1, "notice_title": "旧标题", "notice_type": "2",
                    "notice_content": "旧内容", "status": "0"}
        desired = {"notice_title": "逸享旅居平台预订及入住须知",
                   "notice_content": "<p>正文</p><p>2026年8月20日</p>"}

        sql = build_transaction_sql(snapshot, desired)

        self.assertIn("START TRANSACTION", sql)
        self.assertIn("WHERE notice_id=1", sql)
        self.assertIn("SHA2", sql)
        self.assertNotIn("DELETE", sql)
        self.assertTrue(sql.rstrip().endswith("COMMIT;"))


if __name__ == "__main__":
    unittest.main()
