import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from check_miniprogram_bundle import BundlePolicyError, verify_bundle


class CheckMiniprogramBundleTest(unittest.TestCase):
    def test_accepts_production_api_bundle(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "vendor.js").write_text(
                "const api='https://api.example.com/api'", encoding="utf-8")

            result = verify_bundle(
                root,
                "https://api.example.com/api",
                ["http://127.0.0.1:18080/api"],
            )

        self.assertEqual(1, result["checked_files"])

    def test_rejects_local_api_bundle(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "vendor.js").write_text(
                "const api='http://127.0.0.1:18080/api'", encoding="utf-8")

            with self.assertRaisesRegex(BundlePolicyError, "forbidden local API"):
                verify_bundle(
                    root,
                    "https://api.example.com/api",
                    ["http://127.0.0.1:18080/api"],
                )


if __name__ == "__main__":
    unittest.main()
