#!/usr/bin/env python3
"""Plan, rehearse, and apply the protected 2026 autumn education import."""

from __future__ import annotations

import argparse
import json
import os
import shlex
import shutil
import subprocess
import sys
from datetime import datetime, timedelta, timezone
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any

from education_catalog import (
    REPO_ROOT, TABLE_KEYS, build_transaction_sql, cover_assets, desired_rows,
    load_catalog, sha256_file, summarize,
)

SKILL_SCRIPTS = REPO_ROOT / "codex-skills/manage-yixianghui/scripts"
sys.path.insert(0, str(SKILL_SCRIPTS))

from yxh_plans import STATE_DIR, _schema, _schema_signature  # noqa: E402
from yxh_policy import PolicyError, plan_token, sql_literal  # noqa: E402
from yxh_runtime import (  # noqa: E402
    SSH_HOST, RuntimeFailure, _ssh_env, _ssh_script, create_backup, run_mysql,
    verify_gzip,
)

DEFAULT_CATALOG = REPO_ROOT / "scripts/data/education-2026-fall/catalog.json"
PLAN_VERSION = 1
PLAN_TTL = timedelta(minutes=30)
LOCAL_UPLOAD_ROOT = Path(os.environ.get("YXH_E2E_UPLOAD_DIR", "/tmp/yixianghui-e2e/uploads"))
PRODUCTION_PROFILE = "/home/lk-shzxj/uploadPath"


def _json_pairs(schema: list[dict[str, str]]) -> str:
    return ", ".join(f"{sql_literal(row['name'])}, `{row['name']}`" for row in schema)


def _fetch_rows(target: str, table: str, schema: list[dict[str, str]],
                ids: list[Any]) -> list[dict[str, Any]]:
    pk = TABLE_KEYS[table]
    literals = ",".join(sql_literal(value) for value in ids)
    sql = (
        f"SELECT COALESCE(JSON_ARRAYAGG(JSON_OBJECT({_json_pairs(schema)})), JSON_ARRAY()) "
        f"FROM `{table}` WHERE `{pk}` IN ({literals});"
    )
    raw = run_mysql(target, sql, headers=False).strip() or "[]"
    rows = json.loads(raw)
    return sorted(rows, key=lambda row: row[pk])


def _fetch_json(target: str, sql: str) -> Any:
    raw = run_mysql(target, sql, headers=False).strip()
    return json.loads(raw) if raw else None


def capture_snapshot(target: str, catalog: dict[str, Any]) -> dict[str, Any]:
    wanted = desired_rows(catalog)
    tables = {}
    for table, rows in wanted.items():
        schema = _schema(target, table)
        pk = TABLE_KEYS[table]
        tables[table] = {
            "schema": _schema_signature(schema),
            "rows": _fetch_rows(target, table, schema, [row[pk] for row in rows]),
        }
    names = ",".join(sql_literal(row["name"]) for row in catalog["courses"])
    conflicts = _fetch_json(target, (
        "SELECT COALESCE(JSON_ARRAYAGG(JSON_OBJECT('goods_id', goods_id, "
        "'goods_name', goods_name, 'goods_type', goods_type)), JSON_ARRAY()) "
        f"FROM app_goods WHERE goods_type='education' OR goods_name IN ({names});"
    ))
    prerequisites = _fetch_json(target, (
        "SELECT JSON_OBJECT("
        "'parent_category', (SELECT JSON_OBJECT('category_id', category_id, 'category_name', category_name, 'status', status) "
        "FROM app_goods_category WHERE category_id=58), "
        "'department', (SELECT JSON_OBJECT('dept_id', dept_id, 'dept_name', dept_name, 'status', status) "
        "FROM sys_dept WHERE dept_id=100));"
    ))
    return {"tables": tables, "conflicts": sorted(conflicts or [], key=lambda row: row["goods_id"]),
            "prerequisites": prerequisites}


def assert_initial_state(snapshot: dict[str, Any]) -> None:
    expected = {
        "app_goods_category": {59, 60, 61}, "app_goods": {38},
        "app_goods_education_ext": {1}, "app_goods_related": {316, 317, 318},
    }
    for table, ids in expected.items():
        pk = TABLE_KEYS[table]
        actual = {row[pk] for row in snapshot["tables"][table]["rows"]}
        if actual != ids:
            raise PolicyError(f"Unexpected initial rows for {table}: {sorted(actual)}")
    if {row["goods_id"] for row in snapshot["conflicts"]} != {38}:
        raise PolicyError("Education catalog contains unexpected products or duplicate names")
    prerequisite = snapshot["prerequisites"] or {}
    parent = prerequisite.get("parent_category") or {}
    dept = prerequisite.get("department") or {}
    if parent.get("category_name") != "老年教育" or dept.get("dept_name") != "上海智享居":
        raise PolicyError("Required parent category or department is missing")


def _remote_dir(catalog: dict[str, Any]) -> str:
    relative = catalog["remote_asset_dir"].removeprefix("/profile")
    path = PRODUCTION_PROFILE + relative
    if not path.startswith(PRODUCTION_PROFILE + "/upload/") or ".." in path:
        raise PolicyError("Unsafe production asset path")
    return path


def _asset_state(target: str, catalog: dict[str, Any], assets: list[dict[str, Any]]) -> dict[str, str]:
    if target == "local":
        result = {}
        for asset in assets:
            path = LOCAL_UPLOAD_ROOT / asset["remote_path"].removeprefix("/profile/")
            result[asset["file"]] = sha256_file(path) if path.is_file() else "MISSING"
        return result
    remote_dir = _remote_dir(catalog)
    commands = []
    for asset in assets:
        name = shlex.quote(asset["file"])
        commands.append(f"if test -f {name}; then sha256sum -- {name}; else printf 'MISSING  %s\\n' {name}; fi")
    output = _ssh_script(f"set -euo pipefail; if test -d {shlex.quote(remote_dir)}; then cd {shlex.quote(remote_dir)}; {'; '.join(commands)}; else "
                         + "; ".join(f"printf 'MISSING  %s\\n' {shlex.quote(a['file'])}" for a in assets) + "; fi").stdout.decode()
    state = {}
    for line in output.splitlines():
        digest, name = line.split(None, 1)
        state[name.strip()] = digest
    return state


def _write_plan(document: dict[str, Any]) -> Path:
    directory = STATE_DIR / "audit/education-plans"
    directory.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
    path = directory / f"{stamp}-{document['token']}.json"
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
    os.chmod(path, 0o600)
    return path


def create_import_plan(target: str, catalog_path: Path) -> tuple[Path, dict[str, Any]]:
    catalog = load_catalog(catalog_path)
    assets = cover_assets(catalog, catalog_path.parent / "covers")
    snapshot = capture_snapshot(target, catalog)
    assert_initial_state(snapshot)
    payload = {
        "version": PLAN_VERSION, "created_at": datetime.now(timezone.utc).isoformat(),
        "target": target, "catalog_path": str(catalog_path.resolve()),
        "catalog_sha256": sha256_file(catalog_path), "assets": assets,
        "asset_state": _asset_state(target, catalog, assets), "snapshot": snapshot,
    }
    token = plan_token(payload)
    document = {"token": token, "status": "planned", "payload": payload}
    path = _write_plan(document)
    preview = {"plan": str(path), "token": token, "target": target,
               **summarize(catalog), "assets": [{key: row[key] for key in
               ("file", "bytes", "sha256", "remote_path")} for row in assets],
               "safety": {"preserves_goods_id_38": True, "deletes": 0,
                          "full_backup_before_production": True}}
    return path, preview


def _load_plan(path: Path) -> dict[str, Any]:
    document = json.loads(path.read_text(encoding="utf-8"))
    payload = document.get("payload", {})
    if document.get("token") != plan_token(payload) or document.get("status") != "planned":
        raise PolicyError("Plan is invalid or no longer pending")
    created = datetime.fromisoformat(payload["created_at"])
    if datetime.now(timezone.utc) - created > PLAN_TTL:
        raise PolicyError("Plan expired; create a fresh preview")
    return document


def _install_assets(target: str, catalog: dict[str, Any], assets: list[dict[str, Any]]) -> None:
    if target == "local":
        for asset in assets:
            destination = LOCAL_UPLOAD_ROOT / asset["remote_path"].removeprefix("/profile/")
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(asset["path"], destination)
        return
    remote_dir = _remote_dir(catalog)
    _ssh_script(f"install -d -m 0755 {shlex.quote(remote_dir)}")
    command = ["scp", "-o", "ConnectTimeout=10", "-o", "NumberOfPasswordPrompts=1",
               "-o", "PreferredAuthentications=password", "-o", "StrictHostKeyChecking=yes"]
    command.extend(asset["path"] for asset in assets)
    command.append(f"{SSH_HOST}:{remote_dir}/")
    result = subprocess.run(command, env=_ssh_env(), capture_output=True, check=False)
    if result.returncode:
        raise RuntimeFailure(result.stderr.decode(errors="replace").strip() or "Asset upload failed")


def _same_value(expected: Any, actual: Any) -> bool:
    if expected is None or isinstance(expected, int):
        return expected == actual
    try:
        if re_full_decimal(str(expected)) and re_full_decimal(str(actual)):
            return Decimal(str(expected)) == Decimal(str(actual))
    except InvalidOperation:
        pass
    return str(expected) == str(actual)


def re_full_decimal(value: str) -> bool:
    return bool(value) and all(char in "-0123456789." for char in value) and value.count(".") <= 1


def verify_desired(catalog: dict[str, Any], snapshot: dict[str, Any]) -> None:
    for table, expected_rows in desired_rows(catalog).items():
        pk = TABLE_KEYS[table]
        actual = {row[pk]: row for row in snapshot["tables"][table]["rows"]}
        if set(actual) != {row[pk] for row in expected_rows}:
            raise PolicyError(f"Postcondition row set mismatch for {table}")
        for expected in expected_rows:
            row = actual[expected[pk]]
            if any(not _same_value(value, row.get(key)) for key, value in expected.items()):
                raise PolicyError(f"Postcondition value mismatch for {table} {expected[pk]}")


def apply_import_plan(path: Path, confirmation: str | None) -> dict[str, Any]:
    document = _load_plan(path)
    payload = document["payload"]
    token, target = document["token"], payload["target"]
    if target == "production" and confirmation != token:
        raise PolicyError("Production apply requires the exact plan token")
    catalog_path = Path(payload["catalog_path"])
    if sha256_file(catalog_path) != payload["catalog_sha256"]:
        raise PolicyError("Catalog changed after preview")
    catalog = load_catalog(catalog_path)
    assets = cover_assets(catalog, catalog_path.parent / "covers")
    if assets != payload["assets"] or _asset_state(target, catalog, assets) != payload["asset_state"]:
        raise PolicyError("Assets or target asset state changed after preview")
    snapshot = capture_snapshot(target, catalog)
    if snapshot != payload["snapshot"]:
        raise PolicyError("Database state or schema changed after preview")
    backup = None
    if target == "production":
        stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
        backup_path = STATE_DIR / "backups" / f"production-full-education-{stamp}.sql.gz"
        backup = create_backup(target, backup_path)
        backup.update(verify_gzip(backup_path))
    _install_assets(target, catalog, assets)
    installed_state = _asset_state(target, catalog, assets)
    if any(installed_state[row["file"]] != row["sha256"] for row in assets):
        raise PolicyError("Uploaded asset checksum verification failed")
    run_mysql(target, build_transaction_sql(catalog, snapshot), headers=False, write=True)
    after = capture_snapshot(target, catalog)
    verify_desired(catalog, after)
    document.update({"status": "applied", "applied_at": datetime.now(timezone.utc).isoformat(),
                     "backup": backup, "postcondition": summarize(catalog)["row_impact"]})
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
    os.chmod(path, 0o600)
    return {"plan": str(path), "token": token, "status": "applied", "target": target,
            "backup": backup, "postcondition": document["postcondition"]}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    preview = sub.add_parser("preview")
    preview.add_argument("--env", choices=("local", "production"), required=True)
    preview.add_argument("--catalog", type=Path, default=DEFAULT_CATALOG)
    apply = sub.add_parser("apply")
    apply.add_argument("--plan", type=Path, required=True)
    apply.add_argument("--confirm-production")
    args = parser.parse_args()
    try:
        if args.command == "preview":
            _, result = create_import_plan(args.env, args.catalog)
        else:
            result = apply_import_plan(args.plan, args.confirm_production)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return 0
    except (PolicyError, RuntimeFailure, OSError, ValueError, json.JSONDecodeError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
