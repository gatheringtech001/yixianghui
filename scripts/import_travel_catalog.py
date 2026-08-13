#!/usr/bin/env python3
"""Plan, rehearse, and apply the guarded 56-base travel catalog import."""

from __future__ import annotations

import argparse
import json
import os
import re
import shlex
import shutil
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any

from PIL import Image, ImageOps

from travel_catalog import TARGET_CITIES, load_catalog, sha256_file

REPO_ROOT = Path(__file__).resolve().parent.parent
SKILL_SCRIPTS = REPO_ROOT / "codex-skills/manage-yixianghui/scripts"
sys.path.insert(0, str(SKILL_SCRIPTS))

from yxh_plans import STATE_DIR, _schema, _schema_signature  # noqa: E402
from yxh_policy import PolicyError, plan_token, sql_literal  # noqa: E402
from yxh_runtime import (  # noqa: E402
    SSH_HOST, RuntimeFailure, _ssh_env, _ssh_script, create_backup, run_mysql,
    verify_gzip,
)

TABLE_KEYS = {
    "app_goods_category": "category_id", "app_goods": "goods_id",
    "app_goods_sku": "sku_id", "app_goods_sku_option": "option_id",
    "app_goods_related": "id",
}
PLAN_TTL = timedelta(minutes=30)
PLAN_VERSION = 1
DEFAULT_KNOWLEDGE_ROOT = Path("/Users/kevin/Documents/ChatGPT/yixiangKB")
LOCAL_UPLOAD_ROOT = Path(os.environ.get("YXH_E2E_UPLOAD_DIR", "/tmp/yixianghui-e2e/uploads"))
PRODUCTION_PROFILE = "/home/lk-shzxj/uploadPath"
STAGING_DIR = STATE_DIR / "staging/travel-catalog-v1"


def _json_pairs(schema: list[dict[str, str]]) -> str:
    return ", ".join(f"{sql_literal(row['name'])}, `{row['name']}`" for row in schema)


def _fetch_json(target: str, sql: str) -> Any:
    raw = run_mysql(target, sql, headers=False).strip()
    return json.loads(raw) if raw else None


def _fetch_rows(target: str, table: str, schema: list[dict[str, str]],
                where: str) -> list[dict[str, Any]]:
    pk = TABLE_KEYS[table]
    rows = _fetch_json(target, (
        f"SELECT COALESCE(JSON_ARRAYAGG(JSON_OBJECT({_json_pairs(schema)})), JSON_ARRAY()) "
        f"FROM `{table}` WHERE {where};"
    )) or []
    return sorted(rows, key=lambda row: row[pk])


def capture_snapshot(target: str, catalog: dict[str, Any]) -> dict[str, Any]:
    schemas = {table: _schema(target, table) for table in TABLE_KEYS}
    category_rows = _fetch_rows(
        target, "app_goods_category", schemas["app_goods_category"],
        "category_id=25 OR parent_id=25",
    )
    category_ids = ",".join(str(row["category_id"]) for row in category_rows)
    travel_goods = _fetch_rows(
        target, "app_goods", schemas["app_goods"],
        f"category_id IN ({category_ids})",
    )
    titles = [row["name"] for row in catalog["products"]] + ["昆明古滇基地"]
    names = ",".join(sql_literal(name) for name in titles)
    conflicts = _fetch_rows(
        target, "app_goods", schemas["app_goods"], f"goods_name IN ({names})",
    )
    child_rows = {}
    for table in ("app_goods_sku", "app_goods_sku_option", "app_goods_related"):
        child_rows[table] = _fetch_rows(target, table, schemas[table], "goods_id IN (31,32)")
    maxima = _fetch_json(target, (
        "SELECT JSON_OBJECT("
        "'category_id',(SELECT MAX(category_id) FROM app_goods_category),"
        "'goods_id',(SELECT MAX(goods_id) FROM app_goods),"
        "'sku_id',(SELECT MAX(sku_id) FROM app_goods_sku),"
        "'option_id',(SELECT MAX(option_id) FROM app_goods_sku_option),"
        "'related_id',(SELECT MAX(id) FROM app_goods_related));"
    ))
    departments = _fetch_json(target, (
        "SELECT COALESCE(JSON_ARRAYAGG(JSON_OBJECT('dept_id',dept_id,'dept_name',dept_name,'status',status)),JSON_ARRAY()) "
        "FROM sys_dept WHERE dept_id IN (102,108);"
    ))
    return {
        "schemas": {table: _schema_signature(value) for table, value in schemas.items()},
        "category_rows": category_rows, "travel_goods": travel_goods,
        "conflicts": conflicts, "child_rows": child_rows,
        "maxima": maxima, "departments": sorted(departments or [], key=lambda row: row["dept_id"]),
    }


def _asset_source_rows(catalog: dict[str, Any]) -> list[dict[str, Any]]:
    unique = {}
    for product in catalog["products"]:
        for asset in product["assets"]:
            unique[asset["remote_name"]] = asset
    return [unique[name] for name in sorted(unique)]


def _optimize_asset(asset: dict[str, Any]) -> dict[str, Any]:
    source = Path(asset["file"])
    if sha256_file(source) != asset["sha256"]:
        raise PolicyError(f"Knowledge image changed: {source}")
    destination = STAGING_DIR / asset["remote_name"]
    if not destination.exists():
        with Image.open(source) as opened:
            image = ImageOps.exif_transpose(opened)
            image.thumbnail((1600, 1600), Image.Resampling.LANCZOS)
            if image.mode in {"RGBA", "LA"} or "transparency" in image.info:
                rgba = image.convert("RGBA")
                background = Image.new("RGB", rgba.size, "white")
                background.paste(rgba, mask=rgba.getchannel("A"))
                image = background
            image.convert("RGB").save(destination, "JPEG", quality=84, optimize=True, dpi=(96, 96))
        os.chmod(destination, 0o600)
    return {
        "file": asset["remote_name"], "path": str(destination),
        "remote_path": asset["remote_path"], "source_path": str(source),
        "source_sha256": asset["sha256"], "bytes": destination.stat().st_size,
        "sha256": sha256_file(destination),
    }


def prepare_assets(catalog: dict[str, Any]) -> list[dict[str, Any]]:
    STAGING_DIR.mkdir(parents=True, exist_ok=True)
    os.chmod(STAGING_DIR, 0o700)
    with ThreadPoolExecutor(max_workers=6) as pool:
        assets = list(pool.map(_optimize_asset, _asset_source_rows(catalog)))
    return sorted(assets, key=lambda row: row["file"])


def _remote_dir(catalog: dict[str, Any]) -> str:
    path = PRODUCTION_PROFILE + catalog["remote_asset_dir"].removeprefix("/profile")
    if not path.startswith(PRODUCTION_PROFILE + "/upload/") or ".." in path:
        raise PolicyError("Unsafe production asset path")
    return path


def _asset_state(target: str, catalog: dict[str, Any]) -> dict[str, str]:
    if target == "local":
        directory = LOCAL_UPLOAD_ROOT / catalog["remote_asset_dir"].removeprefix("/profile/")
        return {path.name: sha256_file(path) for path in directory.glob("*.jpg") if path.is_file()}
    directory = _remote_dir(catalog)
    command = (
        f"if test -d {shlex.quote(directory)}; then "
        f"find {shlex.quote(directory)} -maxdepth 1 -type f -name '*.jpg' -exec sha256sum -- {{}} +; fi"
    )
    output = _ssh_script(command).stdout.decode()
    state = {}
    for line in output.splitlines():
        digest, name = line.split(None, 1)
        state[Path(name.strip()).name] = digest
    return state


def _assert_prerequisites(snapshot: dict[str, Any]) -> None:
    categories = {row["category_id"]: row for row in snapshot["category_rows"]}
    if categories.get(25, {}).get("category_name") != "全国旅居":
        raise PolicyError("全国旅居 parent category is missing")
    city_names = {row["category_name"] for row in snapshot["category_rows"] if row["parent_id"] == 25}
    if not {"昆明", "建水", "弥勒", "芒市", "腾冲"}.issubset(city_names):
        raise PolicyError("Existing travel city categories are incomplete")
    departments = {row["dept_id"]: row for row in snapshot["departments"]}
    if set(departments) != {102, 108}:
        raise PolicyError("Required Yunnan departments are missing")
    conflicts = {row["goods_id"] for row in snapshot["conflicts"]}
    if conflicts != {31, 32}:
        raise PolicyError(f"Unexpected duplicate product names: {sorted(conflicts)}")
    names = {row["goods_id"]: row["goods_name"] for row in snapshot["travel_goods"]}
    if names.get(31) != "昆明六号温泉基地" or names.get(32) != "昆明古滇基地":
        raise PolicyError("Existing Kunming products drifted")


def _next(maxima: dict[str, Any], key: str):
    value = int(maxima.get(key) or 0)
    while True:
        value += 1
        yield value


def build_desired(catalog: dict[str, Any], snapshot: dict[str, Any]) -> dict[str, Any]:
    _assert_prerequisites(snapshot)
    maxima = snapshot["maxima"]
    category_ids = {row["category_name"]: row["category_id"] for row in snapshot["category_rows"]}
    category_seq = _next(maxima, "category_id")
    category_inserts = []
    for order_num, city in enumerate(TARGET_CITIES, start=9):
        if city in category_ids:
            continue
        category_id = next(category_seq)
        category_ids[city] = category_id
        category_inserts.append({
            "category_id": category_id, "parent_id": 25, "parent_ids": None,
            "category_name": city, "category_icon": "", "is_hot": 0,
            "link_type": "goods", "link_id": 0, "remark": "",
            "order_num": order_num, "status": "1",
        })
    goods_seq, sku_seq = _next(maxima, "goods_id"), _next(maxima, "sku_id")
    option_seq, related_seq = _next(maxima, "option_id"), _next(maxima, "related_id")
    goods_inserts, goods_updates, skus, options, related = [], [], [], [], []
    product_ids = {}
    existing_goods = {row["goods_id"]: row for row in snapshot["travel_goods"]}
    for product in catalog["products"]:
        goods_id = product["existing_goods_id"] or next(goods_seq)
        product_ids[product["slug"]] = goods_id
        all_quotes = product["bookable_quotes"] or product["quotes"]
        if not all_quotes and not product["existing_goods_id"]:
            raise PolicyError(f"No lodging price found for {product['name']}")
        price = (str(min(Decimal(row["price"]) for row in all_quotes)) if all_quotes
                 else str(existing_goods[goods_id]["price"]))
        gallery = product["gallery"]
        values = {
            "category_id": category_ids[product["city"]], "dept_id": 108 if product["city"] == "昆明" else 102,
            "goods_name": product["name"], "goods_cover": gallery[0], "goods_images": ",".join(gallery),
            "description": product["description"], "tags": product["tags"], "price": price,
            "goods_type": "hotel", "is_hot": 1, "status": "1",
        }
        if product["existing_goods_id"]:
            values["goods_id"] = goods_id
            goods_updates.append(values)
        else:
            goods_inserts.append({
                "goods_id": goods_id, "category_id": values["category_id"], "category_ids": "0",
                "dept_id": values["dept_id"], "goods_name": values["goods_name"],
                "goods_cover": values["goods_cover"], "goods_images": values["goods_images"],
                "description": values["description"], "tags": values["tags"], "price": values["price"],
                "vip_price": None, "unit": None, "specifications": None, "stock": 999,
                "goods_type": "hotel", "is_top": 0, "is_hot": 1, "attr_ids": None,
                "attr_values": None, "is_sku": 1 if product["bookable_quotes"] else 0,
                "award_type": None, "award_parent_ratio": None, "award_grand_parent_ratio": None,
                "award_golden": None, "content": None, "express_fee": "0.00", "weight": None,
                "view_count": 0, "sale_count": 0, "status": "1",
            })
            grouped = {}
            for quote in product["bookable_quotes"]:
                grouped.setdefault(quote["room"], []).append(quote)
            sort_order = 0
            for room, quotes in grouped.items():
                sort_order += 1
                parent_id = next(sku_seq)
                nightly = min(Decimal(row["average"]) for row in quotes)
                skus.append(_sku_row(parent_id, goods_id, room, "200", 0, sort_order, nightly))
                options.extend([
                    _option_row(next(option_seq), goods_id, parent_id, "套餐图片", "305", gallery[0], "", 1, 1),
                    _option_row(next(option_seq), goods_id, parent_id, "价格说明", "304", "知识库价格，按人计价", "", 2, 1),
                ])
                for quote in sorted(quotes, key=lambda row: (row["nights"], Decimal(row["price"]))):
                    sort_order += 1
                    child_id = next(sku_seq)
                    skus.append(_sku_row(child_id, goods_id, quote["duration"], "202", parent_id,
                                         sort_order, Decimal(quote["price"])))
                    options.extend([
                        _option_row(next(option_seq), goods_id, child_id, "天数", "303", str(quote["days"]), "天", 1, 0),
                        _option_row(next(option_seq), goods_id, child_id, "套餐", "304", room, "", 2, 1),
                        _option_row(next(option_seq), goods_id, child_id, "总价", "302", quote["price"], "元", 3, 1),
                        _option_row(next(option_seq), goods_id, child_id, "均价", "301", quote["average"], "元", 4, 1),
                    ])
        for tab in product["tabs"]:
            related.append({
                "id": next(related_seq), "goods_id": goods_id,
                "section_id": tab["section_id"], "section_name": tab["section_name"],
                "content": tab["content"], "sort_order": tab["sort_order"],
                "min_content_length": tab["min_content_length"],
            })
    return {
        "category_inserts": category_inserts, "goods_inserts": goods_inserts,
        "goods_updates": goods_updates, "skus": skus, "options": options,
        "related": related, "product_ids": product_ids,
    }


def _sku_row(sku_id: int, goods_id: int, name: str, sku_type: str, parent: int,
             order: int, price: Decimal) -> dict[str, Any]:
    return {
        "sku_id": sku_id, "goods_id": goods_id, "sku_name": name[:255], "sku_type": sku_type,
        "sku_code": "", "par_sku_id": parent, "sort_order": order, "status": "1",
        "valid_time": None, "invalid_time": None, "stock": 999, "stock_unit": "间",
        "sale_num": 0, "price": str(price.quantize(Decimal("0.01"))), "sale_price": None,
    }


def _option_row(option_id: int, goods_id: int, sku_id: int, name: str, kind: str,
                value: str, unit: str, order: int, sequence: int) -> dict[str, Any]:
    return {
        "option_id": option_id, "goods_id": goods_id, "sku_id": sku_id,
        "option_name": name, "option_param": None, "status": "1", "option_type": kind,
        "option_value": value, "option_value_unit": unit, "option_sort": order,
        "sku_seq_no": sequence,
    }


def _insert_sql(table: str, row: dict[str, Any]) -> str:
    values = dict(row)
    if table in {"app_goods", "app_goods_sku", "app_goods_sku_option", "app_goods_related"}:
        values["create_time"] = {"now": True}
    if table == "app_goods":
        values["update_time"] = {"now": True}
    columns = ",".join(f"`{key}`" for key in values)
    literals = ",".join("NOW()" if value == {"now": True} else sql_literal(value)
                        for value in values.values())
    return f"INSERT INTO `{table}` ({columns}) VALUES ({literals});"


def _update_goods_sql(row: dict[str, Any]) -> str:
    goods_id = row["goods_id"]
    values = {key: value for key, value in row.items() if key != "goods_id"}
    assignments = ",".join(f"`{key}`={sql_literal(value)}" for key, value in values.items())
    return f"UPDATE app_goods SET {assignments},`update_time`=NOW() WHERE goods_id={goods_id};"


def build_transaction_sql(desired: dict[str, Any], snapshot: dict[str, Any]) -> str:
    maxima = snapshot["maxima"]
    guards = [
        f"(SELECT MAX(category_id) FROM app_goods_category)={maxima['category_id']}",
        f"(SELECT MAX(goods_id) FROM app_goods)={maxima['goods_id']}",
        f"(SELECT MAX(sku_id) FROM app_goods_sku)={maxima['sku_id']}",
        f"(SELECT MAX(option_id) FROM app_goods_sku_option)={maxima['option_id']}",
        f"(SELECT MAX(id) FROM app_goods_related)={maxima['related_id']}",
        "EXISTS(SELECT 1 FROM app_goods WHERE goods_id=31 AND BINARY goods_name=BINARY '昆明六号温泉基地')",
        "EXISTS(SELECT 1 FROM app_goods WHERE goods_id=32 AND BINARY goods_name=BINARY '昆明古滇基地')",
    ]
    statements = []
    for row in desired["category_inserts"]:
        statements.append(_insert_sql("app_goods_category", row))
    for row in desired["goods_updates"]:
        statements.append(_update_goods_sql(row))
    for table, key in (("app_goods", "goods_inserts"), ("app_goods_sku", "skus"),
                       ("app_goods_sku_option", "options"), ("app_goods_related", "related")):
        statements.extend(_insert_sql(table, row) for row in desired[key])
    guard = " AND ".join(f"({item})" for item in guards)
    return "\n".join([
        "START TRANSACTION;",
        "CREATE TEMPORARY TABLE yxh_travel_guard (ok TINYINT NOT NULL);",
        f"INSERT INTO yxh_travel_guard (ok) SELECT CASE WHEN {guard} THEN 1 ELSE NULL END;",
        "DROP TEMPORARY TABLE yxh_travel_guard;", *statements, "COMMIT;",
    ])


def _same_value(expected: Any, actual: Any) -> bool:
    if expected is None:
        return actual is None
    try:
        return Decimal(str(expected)) == Decimal(str(actual))
    except (InvalidOperation, ValueError):
        return str(expected) == str(actual)


def verify_desired(target: str, desired: dict[str, Any]) -> None:
    groups = {
        "app_goods_category": desired["category_inserts"],
        "app_goods": desired["goods_inserts"] + desired["goods_updates"],
        "app_goods_sku": desired["skus"], "app_goods_sku_option": desired["options"],
        "app_goods_related": desired["related"],
    }
    for table, expected_rows in groups.items():
        if not expected_rows:
            continue
        schema = _schema(target, table)
        pk = TABLE_KEYS[table]
        ids = ",".join(sql_literal(row[pk]) for row in expected_rows)
        actual = {row[pk]: row for row in _fetch_rows(target, table, schema, f"`{pk}` IN ({ids})")}
        if set(actual) != {row[pk] for row in expected_rows}:
            raise PolicyError(f"Postcondition row set mismatch for {table}")
        for expected in expected_rows:
            if any(not _same_value(value, actual[expected[pk]].get(key)) for key, value in expected.items()):
                raise PolicyError(f"Postcondition value mismatch for {table} {expected[pk]}")


def _write_plan(document: dict[str, Any]) -> Path:
    directory = STATE_DIR / "audit/travel-plans"
    directory.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
    path = directory / f"{stamp}-{document['token']}.json"
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
    os.chmod(path, 0o600)
    return path


def _summary(catalog: dict[str, Any], desired: dict[str, Any], assets: list[dict[str, Any]]) -> dict[str, Any]:
    counts = {city: sum(row["city"] == city for row in catalog["products"]) for city in TARGET_CITIES}
    return {
        "city_product_counts": counts, "products": len(catalog["products"]),
        "bookable_products": sum(bool(row["bookable_quotes"]) or bool(row["existing_goods_id"])
                                    for row in catalog["products"]),
        "display_only_products": [row["name"] for row in catalog["products"]
                                  if not row["bookable_quotes"] and not row["existing_goods_id"]],
        "asset_count": len(assets), "asset_bytes": sum(row["bytes"] for row in assets),
        "row_impact": {
            "category_inserts": len(desired["category_inserts"]),
            "goods_updates": len(desired["goods_updates"]), "goods_inserts": len(desired["goods_inserts"]),
            "sku_inserts": len(desired["skus"]), "sku_option_inserts": len(desired["options"]),
            "related_inserts": len(desired["related"]), "deletes": 0,
        },
    }


def create_plan(target: str, knowledge_root: Path) -> tuple[Path, dict[str, Any]]:
    catalog = load_catalog(knowledge_root)
    assets = prepare_assets(catalog)
    snapshot = capture_snapshot(target, catalog)
    desired = build_desired(catalog, snapshot)
    current_assets = _asset_state(target, catalog)
    expected_assets = {row["file"]: row["sha256"] for row in assets}
    if current_assets and current_assets != expected_assets:
        raise PolicyError("Target travel asset directory contains unexpected files")
    payload = {
        "version": PLAN_VERSION, "created_at": datetime.now(timezone.utc).isoformat(),
        "target": target, "catalog_meta": {
            "knowledge_root": catalog["knowledge_root"], "remote_asset_dir": catalog["remote_asset_dir"],
            "source_hashes": catalog["source_hashes"],
        },
        "assets": assets, "asset_state": current_assets, "snapshot": snapshot, "desired": desired,
    }
    token = plan_token(payload)
    document = {"token": token, "status": "planned", "payload": payload}
    path = _write_plan(document)
    return path, {"plan": str(path), "token": token, "target": target,
                  **_summary(catalog, desired, assets),
                  "safety": {"full_backup_before_production": True, "existing_skus_preserved": [31, 32],
                             "private_contact_text_removed": True, "deletes": 0}}


def _load_plan(path: Path) -> dict[str, Any]:
    document = json.loads(path.read_text(encoding="utf-8"))
    payload = document.get("payload") or {}
    if document.get("status") != "planned" or document.get("token") != plan_token(payload):
        raise PolicyError("Plan is invalid or no longer pending")
    created = datetime.fromisoformat(payload["created_at"])
    if datetime.now(timezone.utc) - created > PLAN_TTL:
        raise PolicyError("Plan expired; create a fresh preview")
    return document


def _validate_files(payload: dict[str, Any]) -> None:
    for path, digest in payload["catalog_meta"]["source_hashes"].items():
        if sha256_file(Path(path)) != digest:
            raise PolicyError(f"Knowledge source changed after preview: {path}")
    for asset in payload["assets"]:
        if sha256_file(Path(asset["source_path"])) != asset["source_sha256"]:
            raise PolicyError(f"Knowledge image changed after preview: {asset['source_path']}")
        if sha256_file(Path(asset["path"])) != asset["sha256"]:
            raise PolicyError(f"Staged image changed after preview: {asset['path']}")


def _install_assets(target: str, catalog_meta: dict[str, Any], assets: list[dict[str, Any]]) -> None:
    if target == "local":
        directory = LOCAL_UPLOAD_ROOT / catalog_meta["remote_asset_dir"].removeprefix("/profile/")
        directory.mkdir(parents=True, exist_ok=True)
        for asset in assets:
            shutil.copy2(asset["path"], directory / asset["file"])
        return
    catalog = {"remote_asset_dir": catalog_meta["remote_asset_dir"]}
    directory = _remote_dir(catalog)
    _ssh_script(f"install -d -m 0755 {shlex.quote(directory)}")
    command = ["scp", "-o", "ConnectTimeout=10", "-o", "NumberOfPasswordPrompts=1",
               "-o", "PreferredAuthentications=password", "-o", "StrictHostKeyChecking=yes"]
    command.extend(asset["path"] for asset in assets)
    command.append(f"{SSH_HOST}:{directory}/")
    result = subprocess.run(command, env=_ssh_env(), capture_output=True, check=False)
    if result.returncode:
        raise RuntimeFailure(result.stderr.decode(errors="replace").strip() or "Asset upload failed")
    _ssh_script(f"chmod 0644 {shlex.quote(directory)}/*.jpg")


def apply_plan(path: Path, confirmation: str | None) -> dict[str, Any]:
    document = _load_plan(path)
    payload = document["payload"]
    target, token = payload["target"], document["token"]
    if target == "production" and confirmation != token:
        raise PolicyError("Production apply requires the exact plan token")
    _validate_files(payload)
    catalog_meta = payload["catalog_meta"]
    catalog_stub = {"remote_asset_dir": catalog_meta["remote_asset_dir"]}
    current_assets = _asset_state(target, catalog_stub)
    expected_assets = {row["file"]: row["sha256"] for row in payload["assets"]}
    if current_assets not in (payload["asset_state"], expected_assets):
        raise PolicyError("Target asset state changed after preview")
    knowledge_root = Path(catalog_meta["knowledge_root"])
    current_catalog = load_catalog(knowledge_root)
    current_snapshot = capture_snapshot(target, current_catalog)
    if current_snapshot != payload["snapshot"]:
        raise PolicyError("Database state or schema changed after preview")
    backup = None
    if target == "production":
        stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
        backup_path = STATE_DIR / "backups" / f"production-full-travel-{stamp}.sql.gz"
        backup = create_backup(target, backup_path)
        backup.update(verify_gzip(backup_path))
    _install_assets(target, catalog_meta, payload["assets"])
    if _asset_state(target, catalog_stub) != expected_assets:
        raise PolicyError("Uploaded asset checksum verification failed")
    run_mysql(target, build_transaction_sql(payload["desired"], payload["snapshot"]),
              headers=False, write=True)
    verify_desired(target, payload["desired"])
    document.update({"status": "applied", "applied_at": datetime.now(timezone.utc).isoformat(),
                     "backup": backup})
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
    os.chmod(path, 0o600)
    return {"plan": str(path), "token": token, "target": target, "status": "applied",
            "backup": backup, "row_impact": {
                "categories": len(payload["desired"]["category_inserts"]),
                "goods": len(payload["desired"]["goods_inserts"]) + len(payload["desired"]["goods_updates"]),
                "skus": len(payload["desired"]["skus"]),
                "options": len(payload["desired"]["options"]),
                "related": len(payload["desired"]["related"]),
            }}


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
        print(str(error), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
