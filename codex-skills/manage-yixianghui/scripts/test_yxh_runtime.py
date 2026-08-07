from __future__ import annotations

import subprocess
import sys
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent))

from yxh_runtime import run_mysql


class ProductionMysqlRuntimeTests(unittest.TestCase):
    @patch("yxh_runtime._ssh_script")
    def test_large_sql_is_streamed_over_stdin(self, ssh_script) -> None:
        sql = "SELECT '" + ("x" * 300_000) + "';"
        ssh_script.return_value = subprocess.CompletedProcess([], 0, stdout=b"ok\n", stderr=b"")

        self.assertEqual("ok\n", run_mysql("production", sql, headers=False, write=True))

        script = ssh_script.call_args.args[0]
        self.assertNotIn(sql, script)
        self.assertNotIn("base64", script)
        self.assertNotIn(" -e ", script)
        self.assertEqual(sql.encode("utf-8"), ssh_script.call_args.kwargs["input_bytes"])


if __name__ == "__main__":
    unittest.main()
