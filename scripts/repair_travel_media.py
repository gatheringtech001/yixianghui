#!/usr/bin/env python3
"""Safely replace travel covers, galleries, and knowledge-detail image URLs."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from import_travel_catalog import (
    DEFAULT_KNOWLEDGE_ROOT,
    STATE_DIR,
    PolicyError,
    RuntimeFailure,
    _asset_state,
    _fetch_rows,
    _install_assets,
    _load_plan,
    _schema,
    _schema_signature,
    _validate_files,
    create_backup,
    plan_token,
    prepare_assets,
    run_mysql,
    sql_literal,
    verify_gzip,
)
from travel_asset_policy import REJECTED_ASSET_SHA256
from travel_catalog import load_catalog

TABLE_KEYS = {"app_goods": "goods_id", "app_goods_related": "id"}
PLAN_VERSION = 1


def _names_sql(products: list[dict[str, Any]]) -> str:
    return ",".join(sql_literal(row["name"]) for row in products)


def _expected_sections(products: list[dict[str, Any]]) -> dict[str, dict[str, Any]]:
    expected = {}
    for product in products:
        for index, content in enumerate(product["sections"], start=1):
            section_id = f"kb_{product['slug'][:12]}_{index}"
            expected[section_id] = {"product": product, "content": content}
    return expected


def capture_snapshot(target: str, catalog: dict[str, Any]) -> dict[str, Any]:
    schemas = {table: _schema(target, table) for table in TABLE_KEYS}
    products = catalog["products"]
    goods = _fetch_rows(
        target, "app_goods", schemas["app_goods"], f"goods_name IN ({_names_sql(products)})",
    )
    expected_names = {row["name"] for row in products}
    actual_names = [row["goods_name"] for row in goods]
    if len(goods) != len(products) or set(actual_names) != expected_names:
        raise PolicyError("Travel product names are missing, duplicated, or drifted")
    sections = _expected_sections(products)
    section_sql = ",".join(sql_literal(value) for value in sections)
    related = _fetch_rows(
        target, "app_goods_related", schemas["app_goods_related"],
        f"section_id IN ({section_sql})",
    )
    if len(related) != len(sections) or {row["section_id"] for row in related} != set(sections):
        raise PolicyError("Travel knowledge-detail rows are missing, duplicated, or drifted")
    goods_ids = {row["goods_name"]: row["goods_id"] for row in goods}
    for row in related:
        expected_goods_id = goods_ids[sections[row["section_id"]]["product"]["name"]]
        if row["goods_id"] != expected_goods_id:
            raise PolicyError(f"Travel detail ownership drifted: {row['section_id']}")
    return {
        "schemas": {table: _schema_signature(value) for table, value in schemas.items()},
        "goods": goods,
        "related": related,
    }


def build_desired(catalog: dict[str, Any], snapshot: dict[str, Any]) -> dict[str, Any]:
    goods_by_name = {row["goods_name"]: row for row in snapshot["goods"]}
    related_by_section = {row["section_id"]: row for row in snapshot["related"]}
    goods_updates = []
    related_updates = []
    for product in catalog["products"]:
        current = goods_by_name[product["name"]]
        goods_updates.append({
            "goods_id": current["goods_id"],
            "goods_name": product["name"],
            "goods_cover": product["gallery"][0],
            "goods_images": ",".join(product["gallery"]),
        })
        for index, content in enumerate(product["sections"], start=1):
            section_id = f"kb_{product['slug'][:12]}_{index}"
            current_related = related_by_section[section_id]
            related_updates.append({
                "id": current_related["id"], "goods_id": current["goods_id"],
                "section_id": section_id, "content": content,
            })
    return {"goods_updates": goods_updates, "related_updates": related_updates}


def _binary_guard(column: str, value: Any) -> str:
    return f"BINARY `{column}` <=> BINARY {sql_literal(value)}"


def build_transaction_sql(payload: dict[str, Any]) -> str:
    snapshot = payload["snapshot"]
    desired = payload["desired"]
    goods_before = {row["goods_id"]: row for row in snapshot["goods"]}
    related_before = {row["id"]: row for row in snapshot["related"]}
    guards = []
    for row in desired["goods_updates"]:
        before = goods_before[row["goods_id"]]
        guards.append(
            "EXISTS(SELECT 1 FROM app_goods WHERE "
            f"goods_id={row['goods_id']} AND {_binary_guard('goods_name', before['goods_name'])} "
            f"AND {_binary_guard('goods_cover', before['goods_cover'])} "
            f"AND {_binary_guard('goods_images', before['goods_images'])})"
        )
    for row in desired["related_updates"]:
        before = related_before[row["id"]]
        digest = hashlib.sha256((before.get("content") or "").encode("utf-8")).hexdigest()
        guards.append(
            "EXISTS(SELECT 1 FROM app_goods_related WHERE "
            f"id={row['id']} AND goods_id={row['goods_id']} "
            f"AND BINARY section_id=BINARY {sql_literal(row['section_id'])} "
            f"AND SHA2(COALESCE(content,''),256)={sql_literal(digest)})"
        )
    statements = []
    for row in desired["goods_updates"]:
        statements.append(
            "UPDATE app_goods SET "
            f"goods_cover={sql_literal(row['goods_cover'])},"
            f"goods_images={sql_literal(row['goods_images'])},update_time=NOW() "
            f"WHERE goods_id={row['goods_id']};"
        )
    for row in desired["related_updates"]:
        statements.append(
            f"UPDATE app_goods_related SET content={sql_literal(row['content'])} "
            f"WHERE id={row['id']} AND goods_id={row['goods_id']};"
        )
    guard = " AND ".join(f"({value})" for value in guards)
    return "\n".join([
        "START TRANSACTION;",
        "CREATE TEMPORARY TABLE yxh_travel_media_guard (ok TINYINT NOT NULL);",
        f"INSERT INTO yxh_travel_media_guard (ok) SELECT CASE WHEN {guard} THEN 1 ELSE NULL END;",
        "DROP TEMPORARY TABLE yxh_travel_media_guard;",
        *statements,
        "COMMIT;",
    ])


def _write_plan(document: dict[str, Any]) -> Path:
    directory = STATE_DIR / "audit/travel-media-plans"
    directory.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
    path = directory / f"{stamp}-{document['token']}.json"
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
    os.chmod(path, 0o600)
    return path


def _preview_examples(snapshot: dict[str, Any], desired: dict[str, Any]) -> list[dict[str, Any]]:
    before = {row["goods_name"]: row["goods_cover"] for row in snapshot["goods"]}
    examples = []
    for row in desired["goods_updates"]:
        if row["goods_name"] in {"昆明五号基地", "昆明八号新官渡基地"}:
            examples.append({
                "goods_name": row["goods_name"], "before": before[row["goods_name"]],
                "after": row["goods_cover"],
            })
    return examples


def create_plan(target: str, knowledge_root: Path) -> tuple[Path, dict[str, Any]]:
    catalog = load_catalog(knowledge_root)
    assets = prepare_assets(catalog)
    if any(row["source_sha256"] in REJECTED_ASSET_SHA256 for row in assets):
        raise PolicyError("Rejected yellow icon reached the staged asset set")
    snapshot = capture_snapshot(target, catalog)
    desired = build_desired(catalog, snapshot)
    current_assets = _asset_state(target, catalog)
    expected_assets = {row["file"]: row["sha256"] for row in assets}
    if current_assets and current_assets != expected_assets:
        raise PolicyError("Target travel-v2 asset directory contains unexpected files")
    payload = {
        "version": PLAN_VERSION, "created_at": datetime.now(timezone.utc).isoformat(),
        "target": target,
        "catalog_meta": {
            "knowledge_root": catalog["knowledge_root"],
            "remote_asset_dir": catalog["remote_asset_dir"],
            "source_hashes": catalog["source_hashes"],
        },
        "assets": assets, "asset_state": current_assets,
        "snapshot": snapshot, "desired": desired,
    }
    token = plan_token(payload)
    path = _write_plan({"token": token, "status": "planned", "payload": payload})
    return path, {
        "plan": str(path), "token": token, "target": target,
        "asset_count": len(assets), "asset_bytes": sum(row["bytes"] for row in assets),
        "row_impact": {
            "app_goods": len(desired["goods_updates"]),
            "app_goods_related": len(desired["related_updates"]),
            "other_tables": 0, "deletes": 0,
        },
        "updated_fields": ["app_goods.goods_cover", "app_goods.goods_images", "app_goods_related.content"],
        "examples": _preview_examples(snapshot, desired),
        "safety": {"full_backup_before_production": True, "rejected_icon_references_after": 0},
    }


def verify_desired(target: str, payload: dict[str, Any]) -> None:
    catalog = load_catalog(Path(payload["catalog_meta"]["knowledge_root"]))
    snapshot = capture_snapshot(target, catalog)
    actual_goods = {row["goods_id"]: row for row in snapshot["goods"]}
    actual_related = {row["id"]: row for row in snapshot["related"]}
    for row in payload["desired"]["goods_updates"]:
        actual = actual_goods[row["goods_id"]]
        if actual["goods_cover"] != row["goods_cover"] or actual["goods_images"] != row["goods_images"]:
            raise PolicyError(f"Travel media postcondition failed for goods {row['goods_id']}")
    for row in payload["desired"]["related_updates"]:
        if actual_related[row["id"]]["content"] != row["content"]:
            raise PolicyError(f"Travel detail postcondition failed for related {row['id']}")


def apply_plan(path: Path, confirmation: str | None) -> dict[str, Any]:
    document = _load_plan(path)
    payload = document["payload"]
    target, token = payload["target"], document["token"]
    if target == "production" and confirmation != token:
        raise PolicyError("Production apply requires the exact plan token")
    _validate_files(payload)
    catalog = load_catalog(Path(payload["catalog_meta"]["knowledge_root"]))
    if capture_snapshot(target, catalog) != payload["snapshot"]:
        raise PolicyError("Database state or schema changed after preview")
    current_assets = _asset_state(target, catalog)
    expected_assets = {row["file"]: row["sha256"] for row in payload["assets"]}
    if current_assets not in (payload["asset_state"], expected_assets):
        raise PolicyError("Target asset state changed after preview")
    backup = None
    if target == "production":
        stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
        backup_path = STATE_DIR / "backups" / f"production-full-travel-media-{stamp}.sql.gz"
        backup = create_backup(target, backup_path)
        backup.update(verify_gzip(backup_path))
    _install_assets(target, payload["catalog_meta"], payload["assets"])
    if _asset_state(target, catalog) != expected_assets:
        raise PolicyError("Uploaded travel-v2 asset checksum verification failed")
    run_mysql(target, build_transaction_sql(payload), headers=False, write=True)
    verify_desired(target, payload)
    document.update({
        "status": "applied", "applied_at": datetime.now(timezone.utc).isoformat(),
        "backup": backup,
    })
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
    os.chmod(path, 0o600)
    return {
        "plan": str(path), "token": token, "target": target, "status": "applied",
        "backup": backup, "row_impact": {
            "app_goods": len(payload["desired"]["goods_updates"]),
            "app_goods_related": len(payload["desired"]["related_updates"]),
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    preview = sub.add_parser("preview")
    preview.add_argument("--env", choices=("local", "production"), required=True)
    preview.add_argument("--knowledge-root", type=Path, default=DEFAULT_KNOWLEDGE_ROOT)
    apply = sub.add_parser("apply")
    apply.add_argument("--plan", type=Path, required=True)
    apply.add_argument("--confirm-production")
    args = parser.parse_args()
    try:
        if args.command == "preview":
            _, result = create_plan(args.env, args.knowledge_root)
        else:
            result = apply_plan(args.plan, args.confirm_production)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return 0
    except (PolicyError, RuntimeFailure, OSError, ValueError) as error:
        print(str(error), file=__import__("sys").stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
