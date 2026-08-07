#!/usr/bin/env python3
"""Secret-safe local and SSH-backed MySQL execution for Yixianghui."""

from __future__ import annotations

import hashlib
import os
import shlex
import shutil
import subprocess
from pathlib import Path

SKILL_DIR = Path(__file__).resolve().parent.parent
ASKPASS = SKILL_DIR / "scripts" / "yxh_askpass.py"
LOCAL_MYSQL = Path(os.environ.get("YXH_MYSQL_BIN", "/opt/homebrew/opt/mysql@8.4/bin/mysql"))
LOCAL_DUMP = Path(os.environ.get("YXH_MYSQLDUMP_BIN", "/opt/homebrew/bin/mysqldump"))
LOCAL_DB = os.environ.get("YXH_LOCAL_DB", "yixianghui_e2e")
LOCAL_USER = os.environ.get("YXH_LOCAL_DB_USER", "yixianghui_e2e")
SSH_HOST = os.environ.get("YXH_PRODUCTION_SSH", "root@8.155.28.116")
REMOTE_JAR = os.environ.get("YXH_REMOTE_JAR", "/home/shzxj/lankong-admin.jar")


class RuntimeFailure(RuntimeError):
    pass


def _sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        while chunk := source.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def _run(args: list[str], *, env: dict[str, str] | None = None,
         stdout=None, input_bytes: bytes | None = None) -> subprocess.CompletedProcess[bytes]:
    result = subprocess.run(
        args,
        input=input_bytes,
        stdout=stdout or subprocess.PIPE,
        stderr=subprocess.PIPE,
        env=env,
        check=False,
    )
    if result.returncode:
        message = result.stderr.decode("utf-8", errors="replace").strip()
        raise RuntimeFailure(message or f"Command failed with exit code {result.returncode}")
    return result


def _ssh_env() -> dict[str, str]:
    env = os.environ.copy()
    env.update({
        "DISPLAY": env.get("DISPLAY", "codex-yxh"),
        "SSH_ASKPASS": str(ASKPASS),
        "SSH_ASKPASS_REQUIRE": "force",
        "YXH_ALLOW_ASKPASS": "1",
    })
    return env


def _remote_prelude() -> str:
    jar = shlex.quote(REMOTE_JAR)
    return f"""
set -euo pipefail
config=$(unzip -p {jar} BOOT-INF/classes/application-druid-prod.yml | tr -d '\\r')
master=$(printf '%s\\n' "$config" | awk '/^[[:space:]]*master:/{{f=1;next}} /^[[:space:]]*slave:/{{f=0}} f')
url=$(printf '%s\\n' "$master" | sed -n 's/^[[:space:]]*url:[[:space:]]*//p' | head -n 1)
db_user=$(printf '%s\\n' "$master" | sed -n 's/^[[:space:]]*username:[[:space:]]*//p' | head -n 1)
db_password=$(printf '%s\\n' "$master" | sed -n 's/^[[:space:]]*password:[[:space:]]*//p' | head -n 1)
db_name=$(printf '%s' "$url" | sed -E 's#.*:[0-9]+/([^?]+).*#\\1#')
db_port=$(printf '%s' "$url" | sed -E 's#.*:([0-9]+)/.*#\\1#')
test -n "$db_user" && test -n "$db_password" && test -n "$db_name" && test -n "$db_port"
""".strip()


def _ssh_script(script: str, *, stdout=None,
                input_bytes: bytes = b"") -> subprocess.CompletedProcess[bytes]:
    remote = [
        "ssh",
        "-o", "ConnectTimeout=10",
        "-o", "NumberOfPasswordPrompts=1",
        "-o", "PreferredAuthentications=password",
        "-o", "StrictHostKeyChecking=yes",
        SSH_HOST,
        "bash", "-lc", shlex.quote(script),
    ]
    return _run(remote, env=_ssh_env(), stdout=stdout, input_bytes=input_bytes)


def _mysql_args(headers: bool) -> list[str]:
    args = ["--batch", "--raw", "--default-character-set=utf8mb4"]
    if not headers:
        args.append("--skip-column-names")
    return args


def run_mysql(target: str, sql: str, *, headers: bool = True,
              write: bool = False) -> str:
    if target == "local":
        args = [str(LOCAL_MYSQL), f"--user={LOCAL_USER}", f"--database={LOCAL_DB}"]
        args.extend(_mysql_args(headers))
        result = _run(args, input_bytes=sql.encode("utf-8"))
    elif target == "production":
        mode = "" if write else "SET SESSION MAX_EXECUTION_TIME=15000; SET TRANSACTION READ ONLY; START TRANSACTION; "
        tail = "" if write else " ROLLBACK;"
        header_flag = "" if headers else "--skip-column-names"
        script = _remote_prelude() + f"""
MYSQL_PWD="$db_password" mysql --host=127.0.0.1 --port="$db_port" \\
  --user="$db_user" --database="$db_name" --batch --raw {header_flag} \\
  --default-character-set=utf8mb4
"""
        payload = (mode + sql + tail).encode("utf-8")
        result = _ssh_script(script, input_bytes=payload)
    else:
        raise RuntimeFailure(f"Unknown target: {target}")
    return result.stdout.decode("utf-8", errors="replace")


def create_backup(target: str, output: Path, *, schema_only: bool = False) -> dict[str, str | int]:
    output.parent.mkdir(parents=True, exist_ok=True)
    flags = ["--single-transaction", "--quick", "--hex-blob", "--routines", "--triggers", "--events"]
    if schema_only:
        flags.append("--no-data")

    import gzip

    if target == "local":
        args = [str(LOCAL_DUMP), f"--user={LOCAL_USER}", *flags, LOCAL_DB]
        with output.open("wb") as raw, gzip.GzipFile(fileobj=raw, mode="wb") as zipped:
            process = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            assert process.stdout is not None
            shutil.copyfileobj(process.stdout, zipped, length=1024 * 1024)
            stderr = process.stderr.read() if process.stderr else b""
            if process.wait():
                raise RuntimeFailure(stderr.decode("utf-8", errors="replace").strip())
    elif target == "production":
        flag_text = " ".join(shlex.quote(flag) for flag in flags)
        script = _remote_prelude() + f"""
MYSQL_PWD="$db_password" mysqldump --host=127.0.0.1 --port="$db_port" \\
  --user="$db_user" {flag_text} "$db_name" | gzip -c
"""
        with output.open("wb") as raw:
            _ssh_script(script, stdout=raw)
    else:
        raise RuntimeFailure(f"Unknown target: {target}")

    os.chmod(output, 0o600)
    digest = _sha256_file(output)
    return {"path": str(output), "bytes": output.stat().st_size, "sha256": digest}


def verify_gzip(path: Path) -> dict[str, str | int]:
    import gzip

    decompressed = 0
    with path.open("rb") as source, gzip.GzipFile(fileobj=source) as zipped:
        while chunk := zipped.read(1024 * 1024):
            decompressed += len(chunk)
    return {"path": str(path), "bytes": path.stat().st_size,
            "decompressed_bytes": decompressed, "sha256": _sha256_file(path)}
