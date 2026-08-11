from __future__ import annotations

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from yxh_plans import _preimage_predicate


class PreimagePredicateTests(unittest.TestCase):
    def test_compares_text_as_binary_to_avoid_collation_drift(self) -> None:
        predicate = _preimage_predicate({
            "activity_name": "肌肉训练 免费体验课",
            "max_count": 15,
            "sign_count": None,
            "sign_end_time": "2026-06-16 02:00:00.000000",
        }, {
            "activity_name": "varchar",
            "max_count": "int",
            "sign_count": "int",
            "sign_end_time": "datetime",
        })

        self.assertIn("BINARY `activity_name` <=> BINARY CONVERT(", predicate)
        self.assertIn("`max_count` <=> 15", predicate)
        self.assertIn("`sign_count` <=> NULL", predicate)
        self.assertIn("`sign_end_time` <=> CAST(CONVERT(", predicate)
        self.assertIn(" AS DATETIME(6))", predicate)


if __name__ == "__main__":
    unittest.main()
