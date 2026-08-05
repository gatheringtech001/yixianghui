#!/usr/bin/env python3
"""Create and apply exact-row content mutation plans."""

from __future__ import annotations

import json
import os
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any

from yxh_policy import (
    PolicyError,
    check_content_change,
    plan_token,
    require_identifier,
    sql_literal,
)
from yxh_runtime import run_mysql

STATE_DIR = Path(os.environ.get("YXH_STATE_DIR", str(Path.home() / ".codex/yixianghui")))


def _schema(target: str, table: str) -> list[dict[str, str]]:
    require_identifier(table)
    sql = f"""
SELECT column_name, data_type, is_nullable, COALESCE(column_default, '<NULL>'), column_key, extra
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = {sql_literal(table)}
ORDER BY ordinal_position;
"""
    rows = []
    for line in run_mysql(target, sql, headers=False).splitlines():
        parts = line.split("\t")
        if len(parts) == 6:
            rows.append(dict(zip(("name", "type", "nullable", "default", "key", "extra"), parts)))
    if not rows:
        raise PolicyError(f"Table does not exist: {table}")
    return rows


def _primary_key(schema: list[dict[str, str]]) -> str:
    keys = [column["name"] for column in schema if column["key"] == "PRI"]
    if len(keys) != 1:
        raise PolicyError("Exactly one primary key is required")
    return keys[0]


def _schema_signature(schema: list[dict[str, str]]) -> list[list[str]]:
    return [[column[key] for key in ("name", "type", "nullable", "default", "key", "extra")]
            for column in schema]


def _fetch_row(target: str, table: str, pk: str, record_id: Any,
               schema: list[dict[str, str]]) -> dict[str, Any] | None:
    pairs = ", ".join(
        f"{sql_literal(column['name'])}, `{column['name']}`" for column in schema
    )
    sql = f"SELECT JSON_OBJECT({pairs}) FROM `{table}` WHERE `{pk}` = {sql_literal(record_id)} LIMIT 1;"
    raw = run_mysql(target, sql, headers=False).strip()
    return json.loads(raw) if raw else None


def _preimage_predicate(before: dict[str, Any]) -> str:
    return " AND ".join(
        f"`{column}` <=> {sql_literal(value)}" for column, value in before.items()
    )


def _write_plan(document: dict[str, Any]) -> Path:
    plan_dir = STATE_DIR / "audit" / "plans"
    plan_dir.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
    path = plan_dir / f"{stamp}-{document['token']}.json"
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
    os.chmod(path, 0o600)
    return path


def _save_document(path: Path, document: dict[str, Any]) -> None:
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
    os.chmod(path, 0o600)


def create_plan(target: str, action: str, table: str, data: dict[str, Any],
                record_id: Any | None = None) -> tuple[Path, dict[str, Any]]:
    schema = _schema(target, table)
    pk = _primary_key(schema)
    names = {column["name"] for column in schema}
    unknown = sorted(set(data) - names)
    if unknown:
        raise PolicyError("Unknown columns: " + ", ".join(unknown))
    nested = sorted(key for key, value in data.items() if isinstance(value, (dict, list)))
    if nested:
        raise PolicyError("Nested JSON values are not allowed: " + ", ".join(nested))
    check_content_change(table, action, set(data), pk)

    before = None
    if action in {"update", "delete"}:
        if record_id is None:
            raise PolicyError("--id is required")
        before = _fetch_row(target, table, pk, record_id, schema)
        if before is None:
            raise PolicyError("Target row does not exist")
    elif action == "insert":
        required = [
            column["name"] for column in schema
            if column["nullable"] == "NO" and column["default"] == "<NULL>"
            and "auto_increment" not in column["extra"]
            and column["name"] not in {"create_time", "update_time"}
        ]
        missing = sorted(set(required) - set(data))
        if missing:
            raise PolicyError("Missing required columns: " + ", ".join(missing))
    else:
        raise PolicyError(f"Unknown action: {action}")

    if action == "update":
        no_op = [key for key, value in data.items() if before.get(key) == value]
        if len(no_op) == len(data):
            raise PolicyError("Plan has no effective changes")

    payload = {
        "version": 1,
        "created_at": datetime.now(timezone.utc).isoformat(),
        "target": target,
        "action": action,
        "table": table,
        "primary_key": pk,
        "record_id": record_id,
        "data": data,
        "before": before,
        "schema": _schema_signature(schema),
    }
    token = plan_token(payload)
    document = {"token": token, "status": "planned", "payload": payload}
    path = _write_plan(document)
    preview = {
        "plan": str(path), "token": token, "target": target,
        "action": action, "table": table, "record_id": record_id,
        "changes": {key: {"before": before.get(key) if before else None, "after": value}
                    for key, value in data.items()},
    }
    if action == "delete":
        preview["delete_row"] = before
    return path, preview


def _load_plan(path: Path) -> dict[str, Any]:
    document = json.loads(path.read_text(encoding="utf-8"))
    if document.get("token") != plan_token(document.get("payload", {})):
        raise PolicyError("Plan token does not match its payload")
    if document.get("status") != "planned":
        raise PolicyError(f"Plan status is {document.get('status')}")
    created = datetime.fromisoformat(document["payload"]["created_at"])
    if datetime.now(timezone.utc) - created > timedelta(minutes=30):
        raise PolicyError("Plan expired; create a fresh preview")
    return document


def apply_plan(path: Path, *, production_confirmation: str | None,
               delete_confirmation: str | None) -> dict[str, Any]:
    document = _load_plan(path)
    payload = document["payload"]
    token = document["token"]
    target = payload["target"]
    action = payload["action"]
    table = payload["table"]
    pk = payload["primary_key"]
    data = payload["data"]
    if target == "production" and production_confirmation != token:
        raise PolicyError("Production apply requires --confirm-production with the plan token")
    if action == "delete" and delete_confirmation != token:
        raise PolicyError("Delete requires --confirm-delete with the plan token")

    schema = _schema(target, table)
    if _schema_signature(schema) != payload["schema"]:
        raise PolicyError("Schema changed after preview")
    if action in {"update", "delete"}:
        current = _fetch_row(target, table, pk, payload["record_id"], schema)
        if current != payload["before"]:
            raise PolicyError("Row changed after preview")

    if action == "update":
        assignments = [f"`{key}` = {sql_literal(value)}" for key, value in data.items()]
        if any(column["name"] == "update_time" for column in schema):
            assignments.append("`update_time` = NOW()")
        statement = (
            f"UPDATE `{table}` SET {', '.join(assignments)} "
            f"WHERE `{pk}` = {sql_literal(payload['record_id'])} "
            f"AND {_preimage_predicate(payload['before'])}; SELECT ROW_COUNT();"
        )
    elif action == "insert":
        values = dict(data)
        for name in ("create_time", "update_time"):
            if any(column["name"] == name for column in schema):
                values[name] = {"$now": True}
        columns = ", ".join(f"`{key}`" for key in values)
        literals = ", ".join("NOW()" if value == {"$now": True} else sql_literal(value)
                             for value in values.values())
        statement = f"INSERT INTO `{table}` ({columns}) VALUES ({literals}); SELECT LAST_INSERT_ID();"
    else:
        statement = (
            f"DELETE FROM `{table}` WHERE `{pk}` = {sql_literal(payload['record_id'])} "
            f"AND {_preimage_predicate(payload['before'])}; "
            "SELECT ROW_COUNT();"
        )

    output = run_mysql(target, "START TRANSACTION; " + statement + " COMMIT;",
                       headers=False, write=True).strip().splitlines()
    result_id = output[-1].strip() if output else ""
    if not result_id or (action != "insert" and result_id != "1"):
        raise PolicyError(f"Unexpected affected-row result: {result_id or '<empty>'}")
    document["status"] = "applied_pending_verification"
    document["applied_at"] = datetime.now(timezone.utc).isoformat()
    document["result_id"] = result_id
    _save_document(path, document)
    verified_id = result_id if action == "insert" else payload["record_id"]
    try:
        after = _fetch_row(target, table, pk, verified_id, schema)
        if action == "delete" and after is not None:
            raise PolicyError("Postcondition failed: deleted row still exists")
        if action != "delete" and after is None:
            raise PolicyError("Postcondition failed: resulting row does not exist")
    except Exception:
        document["status"] = "applied_unverified"
        _save_document(path, document)
        raise
    document["status"] = "applied"
    document["after"] = after
    _save_document(path, document)
    return {"plan": str(path), "token": token, "status": "applied", "result_id": result_id}
