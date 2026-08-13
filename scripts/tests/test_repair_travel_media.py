import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from travel_asset_sync import PolicyError, _pending_assets


class RepairTravelMediaTest(unittest.TestCase):
    def setUp(self):
        self.assets = [
            {"file": "a.jpg", "sha256": "aaa"},
            {"file": "b.jpg", "sha256": "bbb"},
        ]

    def test_resumes_missing_and_corrupt_staged_assets(self):
        self.assertEqual(["b.jpg"], [row["file"] for row in _pending_assets(self.assets, {"a.jpg": "aaa"})])
        self.assertEqual(["a.jpg"], [row["file"] for row in _pending_assets(self.assets, {
            "a.jpg": "partial", "b.jpg": "bbb",
        })])

    def test_rejects_unexpected_staged_asset(self):
        with self.assertRaisesRegex(PolicyError, "unexpected"):
            _pending_assets(self.assets, {"foreign.jpg": "fff"})


if __name__ == "__main__":
    unittest.main()
