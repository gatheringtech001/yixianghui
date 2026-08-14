#!/usr/bin/env python3
"""Replace imported travel details with exactly two audited tabs."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from import_travel_catalog import (
    DEFAULT_KNOWLEDGE_ROOT,
    PLAN_TTL,
    STATE_DIR,
    PolicyError,
    RuntimeFailure,
    _fetch_json,
    _fetch_rows,
    _schema,
    _schema_signature,
    create_backup,
    plan_token,
    run_mysql,
    sql_literal,
    verify_gzip,
)
from travel_catalog import load_catalog, sha256_file

PLAN_VERSION = 1
TAB_IDS = ("basic", "policy")


def _names_sql(products: list[dict[str, Any]]) -> str:
    return ",".join(sql_literal(row["name"]) for row in products)


def capture_snapshot(target: str, catalog: dict[str, Any]) -> dict[str, Any]:
    schemas = {table: _schema(target, table) for table in ("app_goods", "app_goods_related")}
    goods = _fetch_rows(
        target, "app_goods", schemas["app_goods"],
        f"goods_name IN ({_names_sql(catalog['products'])})",
    )
    expected_names = {row["name"] for row in catalog["products"]}
    if len(goods) != 56 or {row["goods_name"] for row in goods} != expected_names:
        raise PolicyError("Travel product names are missing, duplicated, or drifted")
    goods_sql = ",".join(str(row["goods_id"]) for row in goods)
    related = _fetch_rows(
        target, "app_goods_related", schemas["app_goods_related"],
        f"goods_id IN ({goods_sql})",
    )
    maximum = _fetch_json(target, "SELECT JSON_OBJECT('id',COALESCE(MAX(id),0)) FROM app_goods_related;")
    return {
        "schemas": {table: _schema_signature(value) for table, value in schemas.items()},
        "goods": goods, "related": related, "related_max": int(maximum["id"]),
    }


def _desired_row(row_id: int, goods_id: int, tab: dict[str, Any]) -> dict[str, Any]:
    return {
        "id": row_id, "goods_id": goods_id, "section_id": tab["section_id"],
        "section_name": tab["section_name"], "content": tab["content"],
        "sort_order": tab["sort_order"], "min_content_length": tab["min_content_length"],
    }


def build_desired(catalog: dict[str, Any], snapshot: dict[str, Any]) -> dict[str, Any]:
    goods_by_name = {row["goods_name"]: row for row in snapshot["goods"]}
    related_by_goods: dict[int, list[dict[str, Any]]] = {}
    for row in snapshot["related"]:
        related_by_goods.setdefault(row["goods_id"], []).append(row)
    next_id = snapshot["related_max"] + 1
    updates, inserts, deletes = [], [], []
    for product in catalog["products"]:
        goods_id = goods_by_name[product["name"]]["goods_id"]
        existing = sorted(related_by_goods.get(goods_id, []), key=lambda row: row["id"])
        by_section: dict[str, list[dict[str, Any]]] = {}
        for row in existing:
            by_section.setdefault(row["section_id"], []).append(row)
        if any(len(by_section.get(section_id, [])) > 1 for section_id in TAB_IDS):
            raise PolicyError(f"Duplicate target tabs for goods {goods_id}")
        keep_ids = set()
        for tab in product["tabs"]:
            candidates = by_section.get(tab["section_id"], [])
            if not candidates and tab["section_id"] == "basic":
                prefix = f"kb_{product['slug'][:12]}_"
                candidates = [row for row in existing if row["section_id"].startswith(prefix)]
            if candidates:
                current = candidates[0]
                keep_ids.add(current["id"])
                updates.append(_desired_row(current["id"], goods_id, tab))
            else:
                inserts.append(_desired_row(next_id, goods_id, tab))
                keep_ids.add(next_id)
                next_id += 1
        deletes.extend(row for row in existing if row["id"] not in keep_ids)
    return {"updates": updates, "inserts": inserts, "deletes": deletes}


def _binary_guard(column: str, value: Any) -> str:
    return f"BINARY `{column}` <=> BINARY {sql_literal(value)}"


def _row_guard(row: dict[str, Any]) -> str:
    digest = hashlib.sha256((row.get("content") or "").encode("utf-8")).hexdigest()
    return (
        "EXISTS(SELECT 1 FROM app_goods_related WHERE "
        f"id={row['id']} AND goods_id={row['goods_id']} "
        f"AND {_binary_guard('section_id', row['section_id'])} "
        f"AND {_binary_guard('section_name', row['section_name'])} "
        f"AND SHA2(COALESCE(content,''),256)={sql_literal(digest)} "
        f"AND sort_order <=> {sql_literal(row['sort_order'])} "
        f"AND min_content_length <=> {sql_literal(row['min_content_length'])})"
    )


def _insert_sql(row: dict[str, Any]) -> str:
    columns = ",".join(f"`{key}`" for key in row) + ",`create_time`"
    values = ",".join(sql_literal(value) for value in row.values()) + ",NOW()"
    return f"INSERT INTO app_goods_related ({columns}) VALUES ({values});"


def build_transaction_sql(payload: dict[str, Any]) -> str:
    snapshot, desired = payload["snapshot"], payload["desired"]
    goods_ids = ",".join(str(row["goods_id"]) for row in snapshot["goods"])
    guards = [
        f"(SELECT MAX(id) FROM app_goods_related)={snapshot['related_max']}",
        f"(SELECT COUNT(*) FROM app_goods_related WHERE goods_id IN ({goods_ids}))="
        f"{len(snapshot['related'])}",
    ]
    guards.extend(
        "EXISTS(SELECT 1 FROM app_goods WHERE "
        f"goods_id={row['goods_id']} AND {_binary_guard('goods_name', row['goods_name'])})"
        for row in snapshot["goods"]
    )
    guards.extend(_row_guard(row) for row in snapshot["related"])
    statements = []
    for row in desired["updates"]:
        statements.append(
            "UPDATE app_goods_related SET "
            f"section_id={sql_literal(row['section_id'])},"
            f"section_name={sql_literal(row['section_name'])},"
            f"content={sql_literal(row['content'])},"
            f"sort_order={row['sort_order']},"
            f"min_content_length={row['min_content_length']} WHERE id={row['id']};"
        )
    statements.extend(_insert_sql(row) for row in desired["inserts"])
    delete_ids = ",".join(str(row["id"]) for row in desired["deletes"])
    if delete_ids:
        statements.append(f"DELETE FROM app_goods_related WHERE id IN ({delete_ids});")
    guard = " AND ".join(f"({value})" for value in guards)
    return "\n".join([
        "START TRANSACTION;",
        "CREATE TEMPORARY TABLE yxh_travel_tab_guard (ok TINYINT NOT NULL);",
        f"INSERT INTO yxh_travel_tab_guard (ok) SELECT CASE WHEN {guard} THEN 1 ELSE NULL END;",
        "DROP TEMPORARY TABLE yxh_travel_tab_guard;", *statements, "COMMIT;",
    ])


def verify_desired(target: str, catalog: dict[str, Any]) -> None:
    snapshot = capture_snapshot(target, catalog)
    names = {row["goods_id"]: row["goods_name"] for row in snapshot["goods"]}
    expected = {product["name"]: product["tabs"] for product in catalog["products"]}
    grouped: dict[int, list[dict[str, Any]]] = {}
    for row in snapshot["related"]:
        grouped.setdefault(row["goods_id"], []).append(row)
    for goods_id, name in names.items():
        actual = sorted(grouped.get(goods_id, []), key=lambda row: row["sort_order"])
        tabs = expected[name]
        if len(actual) != 2:
            raise PolicyError(f"Travel tab count mismatch for goods {goods_id}")
        for row, tab in zip(actual, tabs):
            fields = ("section_id", "section_name", "content", "sort_order", "min_content_length")
            if any(row[field] != tab[field] for field in fields):
                raise PolicyError(f"Travel tab value mismatch for goods {goods_id}")


def _write_plan(document: dict[str, Any]) -> Path:
    directory = STATE_DIR / "audit/travel-tab-plans"
    directory.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
    path = directory / f"{stamp}-{document['token']}.json"
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
    os.chmod(path, 0o600)
    return path


def create_plan(target: str, knowledge_root: Path) -> tuple[Path, dict[str, Any]]:
    catalog = load_catalog(knowledge_root)
    snapshot = capture_snapshot(target, catalog)
    desired = build_desired(catalog, snapshot)
    payload = {
        "version": PLAN_VERSION, "created_at": datetime.now(timezone.utc).isoformat(),
        "target": target, "catalog_meta": {
            "knowledge_root": catalog["knowledge_root"], "source_hashes": catalog["source_hashes"],
        },
        "snapshot": snapshot, "desired": desired,
    }
    token = plan_token(payload)
    path = _write_plan({"token": token, "status": "planned", "payload": payload})
    return path, {
        "plan": str(path), "token": token, "target": target, "products": 56,
        "before_related_rows": len(snapshot["related"]), "after_related_rows": 112,
        "row_impact": {key: len(desired[key]) for key in ("updates", "inserts", "deletes")},
        "tabs_after": ["基本特色", "入住须知"], "full_backup_before_production": True,
    }


def _load_plan(path: Path) -> dict[str, Any]:
    document = json.loads(path.read_text(encoding="utf-8"))
    payload = document.get("payload") or {}
    if document.get("status") != "planned" or document.get("token") != plan_token(payload):
        raise PolicyError("Plan is invalid or no longer pending")
    created = datetime.fromisoformat(payload["created_at"])
    if datetime.now(timezone.utc) - created > PLAN_TTL:
        raise PolicyError("Plan expired; create a fresh preview")
    return document


def apply_plan(path: Path, production: str | None, delete: str | None) -> dict[str, Any]:
    document = _load_plan(path)
    payload, token = document["payload"], document["token"]
    if payload["target"] == "production" and production != token:
        raise PolicyError("Production apply requires the exact plan token")
    if payload["desired"]["deletes"] and delete != token:
        raise PolicyError("Delete apply requires the exact plan token")
    for source, digest in payload["catalog_meta"]["source_hashes"].items():
        if sha256_file(Path(source)) != digest:
            raise PolicyError(f"Knowledge source changed after preview: {source}")
    catalog = load_catalog(Path(payload["catalog_meta"]["knowledge_root"]))
    if capture_snapshot(payload["target"], catalog) != payload["snapshot"]:
        raise PolicyError("Database state or schema changed after preview")
    backup = None
    if payload["target"] == "production":
        stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
        backup_path = STATE_DIR / "backups" / f"production-full-travel-tabs-{stamp}.sql.gz"
        backup = create_backup("production", backup_path)
        backup.update(verify_gzip(backup_path))
    run_mysql(payload["target"], build_transaction_sql(payload), headers=False, write=True)
    verify_desired(payload["target"], catalog)
    document.update({
        "status": "applied", "applied_at": datetime.now(timezone.utc).isoformat(),
        "backup": backup,
    })
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
    os.chmod(path, 0o600)
    return {"plan": str(path), "token": token, "status": "applied", "backup": backup}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    preview = sub.add_parser("preview")
    preview.add_argument("--env", choices=("local", "production"), required=True)
    preview.add_argument("--knowledge-root", type=Path, default=DEFAULT_KNOWLEDGE_ROOT)
    apply = sub.add_parser("apply")
    apply.add_argument("--plan", type=Path, required=True)
    apply.add_argument("--confirm-production")
    apply.add_argument("--confirm-delete")
    args = parser.parse_args()
    try:
        if args.command == "preview":
            _, result = create_plan(args.env, args.knowledge_root)
        else:
            result = apply_plan(args.plan, args.confirm_production, args.confirm_delete)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return 0
    except (PolicyError, RuntimeFailure, OSError, ValueError) as error:
        print(str(error), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
