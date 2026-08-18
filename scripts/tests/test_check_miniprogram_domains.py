import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from check_miniprogram_domains import DomainPolicyError, verify_domains


class CheckMiniprogramDomainsTest(unittest.TestCase):
    def _write_cache(self, root: Path, download_domains: list[str]) -> None:
        target = root / "profile" / "WeappLocalData" / "localstorage_project.json"
        target.parent.mkdir(parents=True)
        target.write_text(json.dumps({
            "appid": "wx-test",
            "runtimeAttr": {
                "network": {
                    "RequestDomain": ["https://api.example.com"],
                    "UploadDomain": ["https://api.example.com"],
                    "DownloadDomain": download_domains,
                }
            },
        }), encoding="utf-8")

    def test_accepts_domain_configured_for_request_upload_and_download(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self._write_cache(root, ["https://api.example.com"])
            unrelated = root / "other" / "WeappLocalData" / "localstorage_flag.json"
            unrelated.parent.mkdir(parents=True)
            unrelated.write_text("true", encoding="utf-8")

            result = verify_domains(root, "wx-test", "https://api.example.com")

        self.assertEqual(
            ["RequestDomain", "UploadDomain", "DownloadDomain"],
            result["checked"],
        )

    def test_rejects_missing_download_domain_with_actionable_message(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self._write_cache(root, [])

            with self.assertRaisesRegex(
                DomainPolicyError,
                r"DownloadDomain.*https://api\.example\.com",
            ):
                verify_domains(root, "wx-test", "https://api.example.com")


if __name__ == "__main__":
    unittest.main()
