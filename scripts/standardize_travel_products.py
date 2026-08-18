#!/usr/bin/env python3
"""Safely standardize every active travel product from the knowledge base."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from import_travel_catalog import (
    DEFAULT_KNOWLEDGE_ROOT, STATE_DIR, PolicyError, RuntimeFailure,
    _asset_state, _fetch_rows, _load_plan, _schema, _schema_signature,
    _validate_files, create_backup, plan_token, prepare_assets, run_mysql,
    sql_literal, verify_gzip,
)
from travel_asset_sync import sync_assets
from travel_catalog import TARGET_CITIES, _load_extractor, load_catalog
from travel_product_page import ROOM_PLACEHOLDERS, _placeholder_type, _room_media

TABLE_KEYS = {
    "app_goods": "goods_id", "app_goods_related": "id",
    "app_goods_sku": "sku_id", "app_goods_sku_option": "option_id",
}
BASE_SECTION_IDS = {"basic", "base_features"}
VERIFIED_REAL_ROOM_PATHS = {
    "/profile/upload/2026/08/17/travel-room-v1/mile-deluxe-twin-real.jpg",
}
PLAN_VERSION = 1


def _names_sql(products: list[dict[str, Any]]) -> str:
    return ",".join(sql_literal(row["name"]) for row in products)


def _section_kind(section_id: str) -> str:
    if section_id in BASE_SECTION_IDS:
        return "basic"
    if section_id == "policy":
        return "policy"
    raise PolicyError(f"Unexpected travel detail section: {section_id}")


def capture_snapshot(target: str, catalog: dict[str, Any]) -> dict[str, Any]:
    schemas = {table: _schema(target, table) for table in TABLE_KEYS}
    goods = _fetch_rows(target, "app_goods", schemas["app_goods"],
                        f"goods_name IN ({_names_sql(catalog['products'])})")
    expected_names = {row["name"] for row in catalog["products"]}
    if len(goods) != 56 or {row["goods_name"] for row in goods} != expected_names:
        raise PolicyError("Active travel product names are missing, duplicated, or drifted")
    if any(row["status"] != "1" or row["goods_type"] != "hotel" for row in goods):
        raise PolicyError("The 56-product knowledge set is not fully active travel inventory")
    goods_ids = ",".join(str(row["goods_id"]) for row in goods)
    related = _fetch_rows(
        target, "app_goods_related", schemas["app_goods_related"],
        f"goods_id IN ({goods_ids}) AND section_id IN ('basic','base_features','policy')",
    )
    section_keys = [(row["goods_id"], _section_kind(row["section_id"])) for row in related]
    if len(related) != 112 or len(section_keys) != len(set(section_keys)):
        raise PolicyError("Each active travel product must have one feature and one notice row")
    skus = _fetch_rows(
        target, "app_goods_sku", schemas["app_goods_sku"],
        f"goods_id IN ({goods_ids}) AND sku_type='200' AND status='1'",
    )
    options = _fetch_rows(
        target, "app_goods_sku_option", schemas["app_goods_sku_option"],
        f"goods_id IN ({goods_ids}) AND option_type='305' AND status='1'",
    )
    sku_ids = {row["sku_id"] for row in skus}
    option_skus = [row["sku_id"] for row in options]
    if len(options) != len(skus) or set(option_skus) != sku_ids or len(option_skus) != len(set(option_skus)):
        raise PolicyError("Every active parent room must have exactly one active room image")
    return {
        "schemas": {table: _schema_signature(value) for table, value in schemas.items()},
        "goods": goods, "related": related, "skus": skus, "options": options,
    }


def _verified_existing(goods_id: int, path: str) -> bool:
    legacy = goods_id in {31, 32} and path.startswith("/profile/upload/2026/04/28/room")
    return legacy or path in VERIFIED_REAL_ROOM_PATHS


def _room_identity(name: str) -> tuple[str, str | None]:
    match = re.fullmatch(r"(.+?)[（(]([^）)]*(?:人|包房)[^）)]*)[）)]", name)
    return (match.group(1), match.group(2)) if match else (name, None)


def resolve_room_updates(catalog: dict[str, Any], snapshot: dict[str, Any]) -> list[dict[str, Any]]:
    root = Path(catalog["knowledge_root"])
    output = root / "output"
    state = json.loads((output / "migration-state.json").read_text(encoding="utf-8"))
    documents = {row["title"]: row for row in state["documents"]
                 if row.get("category") == "基地资料" and row.get("city") in TARGET_CITIES}
    extractor = _load_extractor(root)
    goods = {row["goods_id"]: row for row in snapshot["goods"]}
    skus = {row["sku_id"]: row for row in snapshot["skus"]}
    allowed_assets = {asset["remote_path"] for product in catalog["products"]
                      for asset in product["assets"]}
    item_cache: dict[str, tuple[dict[str, Any], list[dict[str, Any]]]] = {}
    updates = []
    for option in snapshot["options"]:
        goods_id = option["goods_id"]
        current = option["option_value"] or ""
        source_type = "verified-real"
        if _verified_existing(goods_id, current):
            image = current
        else:
            title = goods[goods_id]["goods_name"]
            if title not in item_cache:
                document = documents[title]
                cache = json.loads((output / "browser-cache" / f"{document['slug']}.json").read_text())
                item_cache[title] = (document, extractor(cache["body_html"]))
            document, items = item_cache[title]
            room, occupancy = _room_identity(skus[option["sku_id"]]["sku_name"])
            media = _room_media(document, items, room, occupancy)
            image, source_type = media["image"], media["sourceType"]
            if source_type == "real" and image not in allowed_assets:
                image = ROOM_PLACEHOLDERS[_placeholder_type(room)]
                source_type = "placeholder-unpublishable"
        if image != current:
            updates.append({
                "option_id": option["option_id"], "goods_id": goods_id,
                "sku_id": option["sku_id"], "option_value": image,
                "source_type": source_type,
            })
    return updates


def build_desired(catalog: dict[str, Any], snapshot: dict[str, Any],
                  room_updates: list[dict[str, Any]]) -> dict[str, Any]:
    goods_by_name = {row["goods_name"]: row for row in snapshot["goods"]}
    related = {(row["goods_id"], _section_kind(row["section_id"])): row
               for row in snapshot["related"]}
    goods_updates, related_updates = [], []
    for product in catalog["products"]:
        current = goods_by_name[product["name"]]
        after = {
            "goods_id": current["goods_id"], "description": product["description"],
            "goods_cover": product["gallery"][0], "goods_images": ",".join(product["gallery"]),
        }
        if any(current[key] != value for key, value in after.items() if key != "goods_id"):
            goods_updates.append(after)
        for tab in product["tabs"]:
            kind = _section_kind(tab["section_id"])
            current_row = related[(current["goods_id"], kind)]
            after_row = {
                "id": current_row["id"], "goods_id": current["goods_id"],
                "section_name": "基地特色" if kind == "basic" else "入住须知",
                "content": tab["content"],
            }
            if current_row["section_name"] != after_row["section_name"] or current_row["content"] != after_row["content"]:
                related_updates.append(after_row)
    return {"goods_updates": goods_updates, "related_updates": related_updates,
            "room_updates": room_updates}


def _guard(column: str, value: Any) -> str:
    return f"BINARY `{column}` <=> BINARY {sql_literal(value)}"


def build_transaction_sql(payload: dict[str, Any]) -> str:
    snapshot, desired = payload["snapshot"], payload["desired"]
    goods = {row["goods_id"]: row for row in snapshot["goods"]}
    related = {row["id"]: row for row in snapshot["related"]}
    options = {row["option_id"]: row for row in snapshot["options"]}
    guards, statements = [], []
    for row in desired["goods_updates"]:
        before = goods[row["goods_id"]]
        guards.append("EXISTS(SELECT 1 FROM app_goods WHERE " +
                      f"goods_id={row['goods_id']} AND {_guard('description', before['description'])} " +
                      f"AND {_guard('goods_cover', before['goods_cover'])} AND {_guard('goods_images', before['goods_images'])})")
        statements.append("UPDATE app_goods SET " +
                          f"description={sql_literal(row['description'])},goods_cover={sql_literal(row['goods_cover'])}," +
                          f"goods_images={sql_literal(row['goods_images'])},update_time=NOW() WHERE goods_id={row['goods_id']};")
    for row in desired["related_updates"]:
        before = related[row["id"]]
        digest = hashlib.sha256((before["content"] or "").encode()).hexdigest()
        guards.append("EXISTS(SELECT 1 FROM app_goods_related WHERE " +
                      f"id={row['id']} AND {_guard('section_name', before['section_name'])} " +
                      f"AND SHA2(COALESCE(content,''),256)={sql_literal(digest)})")
        statements.append("UPDATE app_goods_related SET " +
                          f"section_name={sql_literal(row['section_name'])},content={sql_literal(row['content'])} WHERE id={row['id']};")
    for row in desired["room_updates"]:
        before = options[row["option_id"]]
        guards.append("EXISTS(SELECT 1 FROM app_goods_sku_option WHERE " +
                      f"option_id={row['option_id']} AND sku_id={row['sku_id']} AND {_guard('option_value', before['option_value'])})")
        statements.append("UPDATE app_goods_sku_option SET " +
                          f"option_value={sql_literal(row['option_value'])} WHERE option_id={row['option_id']};")
    guard = " AND ".join(f"({value})" for value in guards) or "TRUE"
    return "\n".join(["START TRANSACTION;", "CREATE TEMPORARY TABLE yxh_standard_guard (ok TINYINT NOT NULL);",
                      f"INSERT INTO yxh_standard_guard (ok) SELECT CASE WHEN {guard} THEN 1 ELSE NULL END;",
                      "DROP TEMPORARY TABLE yxh_standard_guard;", *statements, "COMMIT;"])


def _write_plan(document: dict[str, Any]) -> Path:
    directory = STATE_DIR / "audit/travel-standard-plans"
    directory.mkdir(parents=True, exist_ok=True)
    path = directory / f"{datetime.now(timezone.utc):%Y%m%d-%H%M%S}-{document['token']}.json"
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
    os.chmod(path, 0o600)
    return path


def create_plan(target: str, root: Path) -> tuple[Path, dict[str, Any]]:
    catalog = load_catalog(root)
    assets = prepare_assets(catalog)
    snapshot = capture_snapshot(target, catalog)
    rooms = resolve_room_updates(catalog, snapshot)
    desired = build_desired(catalog, snapshot, rooms)
    payload = {
        "version": PLAN_VERSION, "created_at": datetime.now(timezone.utc).isoformat(),
        "target": target, "catalog_meta": {"knowledge_root": catalog["knowledge_root"],
        "remote_asset_dir": catalog["remote_asset_dir"], "source_hashes": catalog["source_hashes"]},
        "assets": assets, "asset_state": _asset_state(target, catalog),
        "snapshot": snapshot, "desired": desired,
    }
    token = plan_token(payload)
    path = _write_plan({"token": token, "status": "planned", "payload": payload})
    room_counts = {kind: sum(row["source_type"] == kind for row in rooms)
                   for kind in sorted({row["source_type"] for row in rooms})}
    return path, {"plan": str(path), "token": token, "target": target,
                  "active_products": 56, "assets": len(assets),
                  "row_impact": {key: len(value) for key, value in desired.items()},
                  "changed_room_sources": room_counts,
                  "pricing": "preserved; strict all-product price validation is not yet complete"}


def verify_desired(target: str, payload: dict[str, Any]) -> None:
    catalog = load_catalog(Path(payload["catalog_meta"]["knowledge_root"]))
    actual = capture_snapshot(target, catalog)
    goods = {row["goods_id"]: row for row in actual["goods"]}
    related = {row["id"]: row for row in actual["related"]}
    options = {row["option_id"]: row for row in actual["options"]}
    for row in payload["desired"]["goods_updates"]:
        if any(goods[row["goods_id"]][key] != value for key, value in row.items() if key != "goods_id"):
            raise PolicyError(f"Goods postcondition failed: {row['goods_id']}")
    for row in payload["desired"]["related_updates"]:
        if any(related[row["id"]][key] != value for key, value in row.items() if key not in {"id", "goods_id"}):
            raise PolicyError(f"Detail postcondition failed: {row['id']}")
    for row in payload["desired"]["room_updates"]:
        if options[row["option_id"]]["option_value"] != row["option_value"]:
            raise PolicyError(f"Room image postcondition failed: {row['option_id']}")


def apply_plan(path: Path, confirmation: str | None) -> dict[str, Any]:
    document = _load_plan(path)
    payload, token = document["payload"], document["token"]
    if payload["target"] == "production" and confirmation != token:
        raise PolicyError("Production apply requires the exact plan token")
    _validate_files(payload)
    catalog = load_catalog(Path(payload["catalog_meta"]["knowledge_root"]))
    if capture_snapshot(payload["target"], catalog) != payload["snapshot"]:
        raise PolicyError("Database state or schema changed after preview")
    backup_path = STATE_DIR / "backups" / f"production-full-travel-standard-{datetime.now(timezone.utc):%Y%m%d-%H%M%S}.sql.gz"
    backup = create_backup(payload["target"], backup_path) if payload["target"] == "production" else None
    if backup:
        backup.update(verify_gzip(backup_path))
    sync_assets(payload["target"], payload["catalog_meta"], payload["assets"])
    run_mysql(payload["target"], build_transaction_sql(payload), headers=False, write=True)
    verify_desired(payload["target"], payload)
    document.update({"status": "applied", "applied_at": datetime.now(timezone.utc).isoformat(), "backup": backup})
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
    os.chmod(path, 0o600)
    return {"plan": str(path), "token": token, "status": "applied", "backup": backup,
            "row_impact": {key: len(value) for key, value in payload["desired"].items()}}


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
        _, result = create_plan(args.env, args.knowledge_root) if args.command == "preview" else (None, apply_plan(args.plan, args.confirm_production))
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return 0
    except (PolicyError, RuntimeFailure, OSError, ValueError) as error:
        print(str(error), file=__import__("sys").stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
