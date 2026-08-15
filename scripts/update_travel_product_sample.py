#!/usr/bin/env python3
"""Safely replace one production travel product with a reviewed page projection."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
from datetime import datetime, timedelta, timezone
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(REPO_ROOT / "codex-skills/manage-yixianghui/scripts"))

from yxh_plans import STATE_DIR, _schema, _schema_signature  # noqa: E402
from yxh_policy import PolicyError, plan_token, sql_literal  # noqa: E402
from yxh_runtime import RuntimeFailure, create_backup, run_mysql, verify_gzip  # noqa: E402

GOODS_ID = 77
PRODUCT_NAME = "弥勒二号温泉基地（绿宝基地）"
PLAN_TTL = timedelta(minutes=30)
TABLES = ("app_goods", "app_goods_sku", "app_goods_sku_option", "app_goods_related")
SECTION_IDS = {
    "基地概览": "overview", "餐饮": "dining", "交通接送": "transport",
    "温泉与设施": "facilities", "周边景点": "attractions", "入住须知": "policy",
}


def _json_query(target: str, sql: str) -> Any:
    raw = run_mysql(target, f"SELECT JSON_OBJECT('value',({sql}));", headers=False).strip()
    return json.loads(raw)["value"]


def capture_snapshot(target: str) -> dict[str, Any]:
    schemas = {table: _schema_signature(_schema(target, table)) for table in TABLES}
    goods = _json_query(target, "SELECT JSON_OBJECT('goods_id',goods_id,'goods_name',goods_name,"
        "'goods_cover',goods_cover,'goods_images',goods_images,'description',description,'tags',tags,"
        "'price',price,'goods_type',goods_type,'is_sku',is_sku,'status',status) "
        f"FROM app_goods WHERE goods_id={GOODS_ID}")
    if not goods or goods["goods_name"] != PRODUCT_NAME or goods["goods_type"] != "hotel":
        raise PolicyError("Target travel product identity drifted")
    selects = {
        "skus": ("app_goods_sku", "sku_id,goods_id,sku_name,status,sku_type,sku_code,par_sku_id,"
                 "sort_order,valid_time,invalid_time,stock,stock_unit,sale_num,price,sale_price", "sku_id"),
        "options": ("app_goods_sku_option", "option_id,goods_id,sku_id,option_name,option_param,status,"
                    "option_value,option_value_unit,option_sort,option_type,sku_seq_no", "option_id"),
        "related": ("app_goods_related", "id,goods_id,section_id,section_name,content,sort_order,"
                    "min_content_length", "id"),
    }
    rows = {}
    for key, (table, columns, order) in selects.items():
        rows[key] = _json_query(target, "SELECT COALESCE(JSON_ARRAYAGG(JSON_OBJECT(" +
            ",".join(f"'{col}',{col}" for col in columns.split(",")) + ")),JSON_ARRAY()) FROM " +
            f"(SELECT {columns} FROM {table} WHERE goods_id={GOODS_ID} ORDER BY {order}) q")
    deps = _json_query(target, "SELECT JSON_OBJECT("
        f"'order_details',(SELECT COUNT(*) FROM app_goods_order_detail WHERE goods_id={GOODS_ID}),"
        f"'orders',(SELECT COUNT(*) FROM app_goods_order WHERE goods_id={GOODS_ID}),"
        f"'sku_data',(SELECT COUNT(*) FROM app_goods_sku_data WHERE goods_id={GOODS_ID}))")
    if any(int(value) for value in deps.values()):
        raise PolicyError(f"Target product has commerce dependencies: {deps}")
    maxima = _json_query(target, "SELECT JSON_OBJECT("
        "'sku_id',(SELECT MAX(sku_id) FROM app_goods_sku),"
        "'option_id',(SELECT MAX(option_id) FROM app_goods_sku_option),"
        "'related_id',(SELECT MAX(id) FROM app_goods_related))")
    return {"schemas": schemas, "goods": goods, **rows, "dependencies": deps, "maxima": maxima}


def _sku(sku_id: int, name: str, kind: str, parent: int, order: int, price: str) -> dict[str, Any]:
    return {"sku_id": sku_id, "goods_id": GOODS_ID, "sku_name": name, "status": "1",
            "sku_type": kind, "sku_code": "", "par_sku_id": parent, "sort_order": order,
            "valid_time": None, "invalid_time": None, "stock": 999, "stock_unit": "间",
            "sale_num": 0, "price": price, "sale_price": None}


def _option(option_id: int, sku_id: int, name: str, kind: str, value: str,
            unit: str, order: int, sequence: int) -> dict[str, Any]:
    return {"option_id": option_id, "goods_id": GOODS_ID, "sku_id": sku_id,
            "option_name": name, "option_param": None, "status": "1", "option_value": value,
            "option_value_unit": unit, "option_sort": order, "option_type": kind,
            "sku_seq_no": sequence}


def build_desired(product: dict[str, Any], snapshot: dict[str, Any]) -> dict[str, Any]:
    page = product["page_display"]
    packages = page["roomPricePackages"]
    if len(packages) != 6 or sum(len(row["packages"]) for row in packages) != 18:
        raise PolicyError("Reviewed sample must contain exactly 6 room groups and 18 offers")
    if any(row.get("image") for row in page["roomImages"]):
        raise PolicyError("This sample must not claim unverified room images")
    tags = "|".join(row["label"] for row in product["display"]["tags"])
    goods = {"description": page["introduction"], "goods_cover": page["mainImages"][0],
             "goods_images": ",".join(page["mainImages"]), "tags": tags,
             "price": product["pricing"]["starting_price"]}
    next_sku = int(snapshot["maxima"]["sku_id"] or 0) + 1
    next_option = int(snapshot["maxima"]["option_id"] or 0) + 1
    skus, options = [], []
    sort_order = 0
    for room in packages:
        sort_order += 1
        parent_id = next_sku; next_sku += 1
        title = f"{room['roomType']}（{room['occupancy']}）"
        skus.append(_sku(parent_id, title, "200", 0, sort_order, "0.00"))
        options.append(_option(next_option, parent_id, "入住标准", "304", room["occupancy"], "", 1, 1)); next_option += 1
        options.append(_option(next_option, parent_id, "价格说明", "304", "含三餐，按人计价", "", 2, 1)); next_option += 1
        for offer in room["packages"]:
            sort_order += 1
            child_id = next_sku; next_sku += 1
            skus.append(_sku(child_id, offer["duration"], "202", parent_id, sort_order, offer["price"]))
            options.append(_option(next_option, child_id, "天数", "303", str(offer["days"]), "天", 1, 0)); next_option += 1
            options.append(_option(next_option, child_id, "套餐", "304", offer["mealPlan"], "", 2, 1)); next_option += 1
            options.append(_option(next_option, child_id, "总价", "302", offer["price"], "元", 3, 1)); next_option += 1
    current = sorted(snapshot["related"], key=lambda row: row["id"])
    if len(current) != 2:
        raise PolicyError("Expected exactly two existing detail rows")
    details = page["details"] + [page["checkInNotice"]]
    related = []
    next_related = int(snapshot["maxima"]["related_id"] or 0) + 1
    for index, section in enumerate(details, start=1):
        row_id = current[0]["id"] if index == 1 else current[1]["id"] if index == 6 else next_related
        if index not in (1, 6): next_related += 1
        related.append({"id": row_id, "goods_id": GOODS_ID,
                        "section_id": SECTION_IDS[section["title"]], "section_name": section["title"],
                        "content": section["content"], "sort_order": index, "min_content_length": 0})
    return {"goods": goods, "skus": skus, "options": options, "related": related,
            "old_sku_ids": [row["sku_id"] for row in snapshot["skus"]],
            "old_option_ids": [row["option_id"] for row in snapshot["options"]]}


def _insert(table: str, row: dict[str, Any]) -> str:
    values = {**row, "create_time": {"now": True}}
    columns = ",".join(f"`{key}`" for key in values)
    literals = ",".join("NOW()" if value == {"now": True} else sql_literal(value) for value in values.values())
    return f"INSERT INTO `{table}` ({columns}) VALUES ({literals});"


def build_transaction_sql(desired: dict[str, Any], snapshot: dict[str, Any]) -> str:
    goods = desired["goods"]
    assignments = ",".join(f"`{key}`={sql_literal(value)}" for key, value in goods.items())
    sku_ids = ",".join(map(str, desired["old_sku_ids"])); option_ids = ",".join(map(str, desired["old_option_ids"]))
    statements = [f"UPDATE app_goods SET {assignments},update_time=NOW() WHERE goods_id={GOODS_ID};",
                  f"UPDATE app_goods_sku SET status='0' WHERE sku_id IN ({sku_ids});",
                  f"UPDATE app_goods_sku_option SET status='0' WHERE option_id IN ({option_ids});"]
    statements += [_insert("app_goods_sku", row) for row in desired["skus"]]
    statements += [_insert("app_goods_sku_option", row) for row in desired["options"]]
    existing_ids = {row["id"] for row in snapshot["related"]}
    for row in desired["related"]:
        if row["id"] in existing_ids:
            values = {key: value for key, value in row.items() if key not in {"id", "goods_id"}}
            change = ",".join(f"`{key}`={sql_literal(value)}" for key, value in values.items())
            statements.append(f"UPDATE app_goods_related SET {change} WHERE id={row['id']} AND goods_id={GOODS_ID};")
        else:
            statements.append(_insert("app_goods_related", row))
    guard = (f"EXISTS(SELECT 1 FROM app_goods WHERE goods_id={GOODS_ID} AND BINARY goods_name=BINARY {sql_literal(PRODUCT_NAME)}) "
             f"AND (SELECT MAX(sku_id) FROM app_goods_sku)={snapshot['maxima']['sku_id']} "
             f"AND (SELECT MAX(option_id) FROM app_goods_sku_option)={snapshot['maxima']['option_id']} "
             f"AND (SELECT MAX(id) FROM app_goods_related)={snapshot['maxima']['related_id']}")
    return "\n".join(["START TRANSACTION;", "CREATE TEMPORARY TABLE yxh_sample_guard (ok TINYINT NOT NULL);",
        f"INSERT INTO yxh_sample_guard (ok) SELECT CASE WHEN {guard} THEN 1 ELSE NULL END;",
        *statements, "DROP TEMPORARY TABLE yxh_sample_guard;", "COMMIT;"])


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _write_plan(document: dict[str, Any]) -> Path:
    directory = STATE_DIR / "audit/travel-sample-plans"; directory.mkdir(parents=True, exist_ok=True)
    path = directory / f"{datetime.now(timezone.utc):%Y%m%d-%H%M%S}-{document['token']}.json"
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8"); os.chmod(path, 0o600)
    return path


def create_plan(target: str, product_path: Path) -> dict[str, Any]:
    product = json.loads(product_path.read_text(encoding="utf-8")); snapshot = capture_snapshot(target)
    desired = build_desired(product, snapshot)
    payload = {"version": 1, "created_at": datetime.now(timezone.utc).isoformat(), "target": target,
               "source": {"path": str(product_path), "sha256": _sha256(product_path)},
               "snapshot": snapshot, "desired": desired}
    token = plan_token(payload); path = _write_plan({"token": token, "status": "planned", "payload": payload})
    return {"plan": str(path), "token": token, "target": target, "goods_id": GOODS_ID,
            "row_impact": {"goods_updates": 1, "sku_disables": len(desired["old_sku_ids"]),
                "option_disables": len(desired["old_option_ids"]), "sku_inserts": len(desired["skus"]),
                "option_inserts": len(desired["options"]), "related_updates": 2, "related_inserts": 4},
            "display": {"starting_price": desired["goods"]["price"], "room_groups": 6,
                        "offers": 18, "room_images": 0, "detail_sections": 6}}


def _same(expected: Any, actual: Any) -> bool:
    if expected is None or actual is None:
        return expected is actual
    try:
        return Decimal(str(expected)) == Decimal(str(actual))
    except (InvalidOperation, ValueError):
        return str(expected) == str(actual)


def _rows_match(expected: list[dict[str, Any]], actual: list[dict[str, Any]], key: str) -> bool:
    actual_by_id = {row[key]: row for row in actual}
    if set(actual_by_id) != {row[key] for row in expected}:
        return False
    return all(all(_same(value, actual_by_id[row[key]].get(field)) for field, value in row.items())
               for row in expected)


def verify_applied(target: str, desired: dict[str, Any]) -> None:
    current = capture_snapshot(target)
    if any(not _same(value, current["goods"].get(key)) for key, value in desired["goods"].items()):
        raise PolicyError("Goods postcondition failed")
    active_skus = [row for row in current["skus"] if str(row["status"]) == "1"]
    active_options = [row for row in current["options"] if str(row["status"]) == "1"]
    if not _rows_match(desired["skus"], active_skus, "sku_id"):
        raise PolicyError("SKU postcondition failed")
    if not _rows_match(desired["options"], active_options, "option_id"):
        raise PolicyError("SKU option postcondition failed")
    if not _rows_match(desired["related"], current["related"], "id"):
        raise PolicyError("Detail section postcondition failed")


def apply_plan(path: Path, confirmation: str | None) -> dict[str, Any]:
    document = json.loads(path.read_text(encoding="utf-8")); payload = document.get("payload") or {}
    if document.get("status") != "planned" or document.get("token") != plan_token(payload):
        raise PolicyError("Plan is invalid or no longer pending")
    if datetime.now(timezone.utc) - datetime.fromisoformat(payload["created_at"]) > PLAN_TTL:
        raise PolicyError("Plan expired; create a fresh preview")
    if payload["target"] == "production" and confirmation != document["token"]:
        raise PolicyError("Production apply requires the exact plan token")
    source = Path(payload["source"]["path"])
    if _sha256(source) != payload["source"]["sha256"] or capture_snapshot(payload["target"]) != payload["snapshot"]:
        raise PolicyError("Source, schema, or database state changed after preview")
    backup = None
    if payload["target"] == "production":
        backup_path = STATE_DIR / "backups" / f"production-before-travel-sample-{datetime.now(timezone.utc):%Y%m%d-%H%M%S}.sql.gz"
        backup = create_backup("production", backup_path); backup.update(verify_gzip(backup_path))
    run_mysql(payload["target"], build_transaction_sql(payload["desired"], payload["snapshot"]), headers=False, write=True)
    verify_applied(payload["target"], payload["desired"])
    document.update({"status": "applied", "applied_at": datetime.now(timezone.utc).isoformat(), "backup": backup})
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8"); os.chmod(path, 0o600)
    return {"plan": str(path), "status": "applied", "token": document["token"], "backup": backup}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__); sub = parser.add_subparsers(dest="command", required=True)
    preview = sub.add_parser("preview"); preview.add_argument("--env", choices=("local", "production"), required=True); preview.add_argument("--product-json", type=Path, required=True)
    apply = sub.add_parser("apply"); apply.add_argument("--plan", type=Path, required=True); apply.add_argument("--confirm-production")
    args = parser.parse_args()
    try:
        result = create_plan(args.env, args.product_json) if args.command == "preview" else apply_plan(args.plan, args.confirm_production)
        print(json.dumps(result, ensure_ascii=False, indent=2)); return 0
    except (PolicyError, RuntimeFailure, OSError, ValueError, json.JSONDecodeError) as error:
        print(str(error), file=sys.stderr); return 2


if __name__ == "__main__":
    raise SystemExit(main())
