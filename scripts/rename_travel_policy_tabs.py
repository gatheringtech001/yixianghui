#!/usr/bin/env python3
"""Safely rename every travel policy tab to 入住须知."""

from __future__ import annotations

import argparse
import json
import os
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parent.parent
SKILL_SCRIPTS = REPO_ROOT / "codex-skills/manage-yixianghui/scripts"
sys.path.insert(0, str(SKILL_SCRIPTS))

from yxh_plans import STATE_DIR, _schema, _schema_signature  # noqa: E402
from yxh_policy import PolicyError, plan_token, sql_literal  # noqa: E402
from yxh_runtime import RuntimeFailure, create_backup, run_mysql, verify_gzip  # noqa: E402

PLAN_TTL = timedelta(minutes=30)
PLAN_VERSION = 1
OLD_LABEL = "政策"
NEW_LABEL = "入住须知"
SECTION_ID = "policy"
MAX_ROWS = 200


def capture_snapshot(target: str) -> dict[str, Any]:
    schemas = {
        table: _schema_signature(_schema(target, table))
        for table in ("app_goods", "app_goods_related")
    }
    raw = run_mysql(target, (
        "SELECT COALESCE(JSON_ARRAYAGG(JSON_OBJECT("
        "'id',r.id,'goods_id',r.goods_id,'goods_name',g.goods_name,"
        "'goods_type',g.goods_type,'goods_status',g.status,"
        "'section_id',r.section_id,'section_name',r.section_name,"
        "'sort_order',r.sort_order)),JSON_ARRAY()) "
        "FROM app_goods_related r INNER JOIN app_goods g ON g.goods_id=r.goods_id "
        "WHERE BINARY g.goods_type=BINARY 'hotel' "
        "AND BINARY r.section_id=BINARY 'policy';"
    ), headers=False).strip()
    rows = sorted(json.loads(raw) if raw else [], key=lambda row: row["id"])
    if not rows:
        raise PolicyError("No travel policy tabs found")
    if len(rows) > MAX_ROWS:
        raise PolicyError(f"Travel policy tab count exceeds safety limit: {len(rows)}")
    if len({row["id"] for row in rows}) != len(rows):
        raise PolicyError("Duplicate travel policy tab ids found")
    unexpected = [
        row for row in rows
        if row["section_name"] not in {OLD_LABEL, NEW_LABEL}
    ]
    if unexpected:
        raise PolicyError(f"Unexpected travel policy labels: {[row['id'] for row in unexpected]}")
    return {"schemas": schemas, "rows": rows}


def build_updates(snapshot: dict[str, Any]) -> list[dict[str, Any]]:
    return [
        {"id": row["id"], "goods_id": row["goods_id"], "before": OLD_LABEL, "after": NEW_LABEL}
        for row in snapshot.get("rows", snapshot.get("related", []))
        if row["section_name"] == OLD_LABEL
    ]


def _row_guard(row: dict[str, Any]) -> str:
    return (
        "EXISTS(SELECT 1 FROM app_goods_related r "
        "INNER JOIN app_goods g ON g.goods_id=r.goods_id WHERE "
        f"r.id={row['id']} AND r.goods_id={row['goods_id']} "
        f"AND BINARY g.goods_name <=> BINARY {sql_literal(row['goods_name'])} "
        f"AND BINARY g.goods_type=BINARY {sql_literal(row['goods_type'])} "
        f"AND BINARY r.section_id=BINARY {sql_literal(row['section_id'])} "
        f"AND BINARY r.section_name=BINARY {sql_literal(row['section_name'])})"
    )


def build_transaction_sql(snapshot: dict[str, Any], updates: list[dict[str, Any]]) -> str:
    if not updates:
        raise PolicyError("No old travel policy labels remain")
    rows = snapshot.get("rows", snapshot.get("related", []))
    ids = ",".join(str(row["id"]) for row in updates)
    guards = [
        "(SELECT COUNT(*) FROM app_goods_related r INNER JOIN app_goods g "
        "ON g.goods_id=r.goods_id WHERE BINARY g.goods_type=BINARY 'hotel' "
        f"AND BINARY r.section_id=BINARY 'policy')={len(rows)}",
        *(_row_guard(row) for row in rows),
    ]
    guard = " AND ".join(f"({value})" for value in guards)
    return "\n".join([
        "START TRANSACTION;",
        "CREATE TEMPORARY TABLE yxh_travel_label_guard (ok TINYINT NOT NULL);",
        f"INSERT INTO yxh_travel_label_guard (ok) SELECT CASE WHEN {guard} THEN 1 ELSE NULL END;",
        "UPDATE app_goods_related "
        f"SET section_name={sql_literal(NEW_LABEL)} WHERE id IN ({ids}) "
        f"AND BINARY section_id=BINARY {sql_literal(SECTION_ID)} "
        f"AND BINARY section_name=BINARY {sql_literal(OLD_LABEL)};",
        "INSERT INTO yxh_travel_label_guard (ok) "
        f"SELECT CASE WHEN ROW_COUNT()={len(updates)} THEN 1 ELSE NULL END;",
        "DROP TEMPORARY TABLE yxh_travel_label_guard;",
        "COMMIT;",
    ])


def _write_plan(document: dict[str, Any]) -> Path:
    directory = STATE_DIR / "audit/travel-label-plans"
    directory.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
    path = directory / f"{stamp}-{document['token']}.json"
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
    os.chmod(path, 0o600)
    return path


def create_plan(target: str) -> tuple[Path, dict[str, Any]]:
    snapshot = capture_snapshot(target)
    updates = build_updates(snapshot)
    if not updates:
        raise PolicyError("All travel policy tabs are already named 入住须知")
    payload = {
        "version": PLAN_VERSION,
        "created_at": datetime.now(timezone.utc).isoformat(),
        "target": target,
        "snapshot": snapshot,
        "updates": updates,
    }
    token = plan_token(payload)
    path = _write_plan({"token": token, "status": "planned", "payload": payload})
    names = {row["goods_id"]: row["goods_name"] for row in snapshot["rows"]}
    return path, {
        "plan": str(path), "token": token, "target": target,
        "table": "app_goods_related", "field": "section_name",
        "before": OLD_LABEL, "after": NEW_LABEL, "affected_rows": len(updates),
        "items": [
            {"related_id": row["id"], "goods_id": row["goods_id"],
             "goods_name": names[row["goods_id"]]}
            for row in updates
        ],
    }


def _load_plan(path: Path) -> dict[str, Any]:
    document = json.loads(path.read_text(encoding="utf-8"))
    payload = document.get("payload") or {}
    if document.get("status") != "planned" or document.get("token") != plan_token(payload):
        raise PolicyError("Plan is invalid or no longer pending")
    if datetime.now(timezone.utc) - datetime.fromisoformat(payload["created_at"]) > PLAN_TTL:
        raise PolicyError("Plan expired; create a fresh preview")
    return document


def verify_applied(target: str, expected_ids: set[int]) -> None:
    snapshot = capture_snapshot(target)
    rows = snapshot["rows"]
    if {row["id"] for row in rows} != expected_ids:
        raise PolicyError("Travel policy tab row set changed during apply")
    if any(row["section_name"] != NEW_LABEL for row in rows):
        raise PolicyError("Travel policy label postcondition failed")


def apply_plan(path: Path, confirmation: str | None) -> dict[str, Any]:
    document = _load_plan(path)
    payload, token = document["payload"], document["token"]
    if payload["target"] == "production" and confirmation != token:
        raise PolicyError("Production apply requires the exact plan token")
    if capture_snapshot(payload["target"]) != payload["snapshot"]:
        raise PolicyError("Database state or schema changed after preview")
    backup = None
    if payload["target"] == "production":
        stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
        backup_path = STATE_DIR / "backups" / f"production-before-travel-label-{stamp}.sql.gz"
        backup = create_backup("production", backup_path)
        backup.update(verify_gzip(backup_path))
    sql = build_transaction_sql(payload["snapshot"], payload["updates"])
    run_mysql(payload["target"], sql, headers=False, write=True)
    expected_ids = {row["id"] for row in payload["snapshot"]["rows"]}
    verify_applied(payload["target"], expected_ids)
    document.update({
        "status": "applied", "applied_at": datetime.now(timezone.utc).isoformat(),
        "backup": backup,
    })
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
    os.chmod(path, 0o600)
    return {
        "plan": str(path), "token": token, "status": "applied",
        "affected_rows": len(payload["updates"]), "backup": backup,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    preview = sub.add_parser("preview")
    preview.add_argument("--env", choices=("local", "production"), required=True)
    apply = sub.add_parser("apply")
    apply.add_argument("--plan", type=Path, required=True)
    apply.add_argument("--confirm-production")
    args = parser.parse_args()
    try:
        if args.command == "preview":
            _, result = create_plan(args.env)
        else:
            result = apply_plan(args.plan, args.confirm_production)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return 0
    except (PolicyError, RuntimeFailure, OSError, ValueError, json.JSONDecodeError) as error:
        print(str(error), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
