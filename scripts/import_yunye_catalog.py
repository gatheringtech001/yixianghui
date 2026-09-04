#!/usr/bin/env python3
"""Plan and apply the 云野集 storefront import to Yixianghui."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO_ROOT / "codex-skills/manage-yixianghui/scripts"))

from yxh_plans import STATE_DIR, _schema, _schema_signature  # noqa: E402
from yxh_policy import PolicyError, plan_token, sql_literal  # noqa: E402
from yxh_runtime import RuntimeFailure, create_backup, run_mysql, verify_gzip  # noqa: E402

PARENT_ID = 33
PLAN_TTL = timedelta(minutes=30)
NON_SALE_SOURCE_IDS = {16885, 16894}


def read_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def canonical_digest(value: Any) -> str:
    raw = json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()


def build_catalog(source: Path) -> dict[str, Any]:
    products = read_json(source / "products.json")
    categories = read_json(source / "categories.json")
    if not products.get("complete") or products.get("truncated"):
        raise PolicyError("云野集商品列表不完整")
    if categories.get("code") != 200:
        raise PolicyError("云野集分类响应无效")
    product_rows = products.get("items") or []
    ids = {int(row["id"]) for row in product_rows}
    if len(ids) != products.get("unique_count"):
        raise PolicyError("云野集商品 ID 计数不一致")

    class_rows = categories.get("data") or []
    memberships: dict[int, list[int]] = {goods_id: [] for goods_id in ids}
    class_counts: dict[int, int] = {}
    for category in class_rows:
        class_id = int(category["id"])
        listing = read_json(source / "classes" / f"{class_id}.json")
        if not listing.get("complete"):
            raise PolicyError(f"分类 {class_id} 抓取不完整")
        class_counts[class_id] = int(listing["unique_count"])
        for item in listing.get("items") or []:
            goods_id = int(item["id"])
            if goods_id in memberships:
                memberships[goods_id].append(class_id)

    active_classes = [row for row in class_rows if class_counts[int(row["id"])] > 0]
    class_names = {int(row["id"]): str(row["cate_name"]).strip() for row in active_classes}
    mapped = []
    for list_row in product_rows:
        goods_id = int(list_row["id"])
        detail = read_json(source / "details" / f"{goods_id}.json")
        if detail.get("code") != 200 or not detail.get("data", {}).get("goods"):
            raise PolicyError(f"商品 {goods_id} 详情无效")
        goods = detail["data"]["goods"]
        specs = detail["data"].get("spec_relate") or []
        class_ids = memberships[goods_id]
        if not class_ids:
            raise PolicyError(f"商品 {goods_id} 没有分类")
        images = json.loads(goods.get("pic_group") or "[]")
        instruction = str(goods.get("instruction") or goods.get("promote_word") or "").strip()
        spec_text = "、".join(str(row.get("spec_value_name") or "").strip() for row in specs)
        description = instruction or str(goods["goods_name"])
        if spec_text:
            description = f"{description}；规格：{spec_text}"
        is_retail_goods = int(goods.get("goods_type") or 0) == 0
        publishable = is_retail_goods and len(specs) <= 1 and goods_id not in NON_SALE_SOURCE_IDS
        mapped.append({
            "source_id": goods_id,
            "category_name": class_names[class_ids[0]],
            "goods_name": str(goods["goods_name"]).strip(),
            "goods_cover": str(goods.get("pic") or images[0]),
            "goods_images": ",".join(images),
            "description": description[:500],
            "tags": "云南好物,云野集",
            "price": str(goods["price"]),
            "vip_price": str(goods["price"]),
            "unit": "件",
            "specifications": f"YUNYE:{goods_id}",
            "stock": int(goods.get("stock") or 0),
            "goods_type": "online",
            "attr_values": json.dumps(specs, ensure_ascii=False, separators=(",", ":")),
            "content": str(goods.get("description") or ""),
            "express_fee": str(goods.get("shipping_fee") or "0.00"),
            "weight": int(goods.get("weight") or 0),
            "view_count": int(goods.get("click") or 0),
            "sale_count": int(goods.get("sales_volume") or 0),
            "status": "1" if publishable else "0",
            "draft_reason": None if publishable else (
                "云野集说明页，不可售卖" if goods_id in NON_SALE_SOURCE_IDS else (
                    "非普通零售商品" if not is_retail_goods else "多规格价格或库存待映射"
                )
            ),
        })
    if len(mapped) != len(ids):
        raise PolicyError("商品详情数量不完整")
    return {"classes": [class_names[int(row["id"])] for row in active_classes], "goods": mapped}


def fetch_json(target: str, sql: str) -> Any:
    raw = run_mysql(target, sql, headers=False).strip()
    return json.loads(raw) if raw else None


def snapshot(target: str) -> dict[str, Any]:
    state = fetch_json(target, f"""
SELECT JSON_OBJECT(
  'parent', (SELECT JSON_OBJECT('category_id',category_id,'category_name',category_name,
    'link_type',link_type,'link_id',link_id,'order_num',order_num,'status',status)
    FROM app_goods_category WHERE category_id={PARENT_ID}),
  'children', (SELECT COUNT(*) FROM app_goods_category WHERE parent_id={PARENT_ID}),
  'source_goods', (SELECT COUNT(*) FROM app_goods WHERE specifications LIKE 'YUNYE:%'),
  'department', (SELECT JSON_OBJECT('dept_id',dept_id,'dept_name',dept_name,'status',status)
    FROM sys_dept WHERE dept_id=100));
""")
    return {
        "state": state,
        "schema": {
            table: _schema_signature(_schema(target, table))
            for table in ("app_goods_category", "app_goods")
        },
    }


def assert_initial_state(value: dict[str, Any]) -> None:
    state = value.get("state") or {}
    parent = state.get("parent") or {}
    department = state.get("department") or {}
    if parent.get("category_id") != PARENT_ID or str(parent.get("category_name", "")).strip() != "云野好物":
        raise PolicyError("旧的云野好物占位栏目不存在或已漂移")
    if state.get("children") != 0 or state.get("source_goods") != 0:
        raise PolicyError("云南好物目标分类或云野集来源商品已存在")
    if department.get("dept_id") != 100 or department.get("status") != "0":
        raise PolicyError("根部门状态不符合预期")


def build_sql(catalog: dict[str, Any]) -> str:
    statements = [
        "SET NAMES utf8mb4",
        "START TRANSACTION",
        f"UPDATE app_goods_category SET category_name={sql_literal('云南好物')}, "
        f"link_type={sql_literal('goods')}, link_id=0, status='1' WHERE category_id={PARENT_ID}",
    ]
    for order_num, name in enumerate(catalog["classes"], start=1):
        statements.append(
            "INSERT INTO app_goods_category "
            "(parent_id,parent_ids,category_name,is_hot,link_type,link_id,remark,order_num,status) VALUES "
            f"({PARENT_ID},{sql_literal('0,33')},{sql_literal(name)},0,'goods',0,"
            f"{sql_literal('云野集前台分类')},{order_num},'1')"
        )
    columns = (
        "category_id,category_ids,dept_id,goods_name,goods_cover,goods_images,description,tags,"
        "price,vip_price,unit,specifications,stock,goods_type,is_top,is_hot,attr_values,is_sku,"
        "award_type,content,express_fee,weight,view_count,sale_count,create_time,update_time,status"
    )
    for row in catalog["goods"]:
        category = (
            f"(SELECT category_id FROM app_goods_category WHERE parent_id={PARENT_ID} "
            f"AND category_name={sql_literal(row['category_name'])} LIMIT 1)"
        )
        values = [category, sql_literal("0,33"), "100"]
        values.extend(sql_literal(row[key]) for key in (
            "goods_name", "goods_cover", "goods_images", "description", "tags", "price",
            "vip_price", "unit", "specifications", "stock", "goods_type",
        ))
        values.extend(["0", "0", sql_literal(row["attr_values"]), "0", "'0'"])
        values.extend(sql_literal(row[key]) for key in (
            "content", "express_fee", "weight", "view_count", "sale_count",
        ))
        values.extend(["CURRENT_TIMESTAMP", "CURRENT_TIMESTAMP", sql_literal(row["status"])])
        statements.append(f"INSERT INTO app_goods ({columns}) VALUES ({','.join(values)})")
    statements.extend(["COMMIT", ""])
    return ";\n".join(statements)


def write_plan(document: dict[str, Any]) -> Path:
    directory = STATE_DIR / "audit/yunye-plans"
    directory.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
    path = directory / f"{stamp}-{document['token']}.json"
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
    os.chmod(path, 0o600)
    return path


def create_plan(target: str, source: Path) -> dict[str, Any]:
    catalog = build_catalog(source)
    before = snapshot(target)
    assert_initial_state(before)
    payload = {
        "version": 1,
        "created_at": datetime.now(timezone.utc).isoformat(),
        "target": target,
        "source": str(source.resolve()),
        "catalog_digest": canonical_digest(catalog),
        "catalog": catalog,
        "snapshot": before,
    }
    document = {"token": plan_token(payload), "status": "planned", "payload": payload}
    path = write_plan(document)
    published = sum(row["status"] == "1" for row in catalog["goods"])
    return {
        "plan": str(path), "token": document["token"], "target": target,
        "row_impact": {"parent_updates": 1, "category_inserts": len(catalog["classes"]),
                       "goods_inserts": len(catalog["goods"])},
        "published": published, "drafts": len(catalog["goods"]) - published,
    }


def load_plan(path: Path) -> dict[str, Any]:
    document = read_json(path)
    payload = document.get("payload") or {}
    if document.get("status") != "planned" or document.get("token") != plan_token(payload):
        raise PolicyError("导入计划无效")
    if datetime.now(timezone.utc) - datetime.fromisoformat(payload["created_at"]) > PLAN_TTL:
        raise PolicyError("导入计划已过期")
    return document


def apply_plan(path: Path, confirmation: str | None) -> dict[str, Any]:
    document = load_plan(path)
    payload = document["payload"]
    if payload["target"] == "production" and confirmation != document["token"]:
        raise PolicyError("生产导入需要匹配计划 token")
    current_catalog = build_catalog(Path(payload["source"]))
    if canonical_digest(current_catalog) != payload["catalog_digest"]:
        raise PolicyError("抓取数据在预览后发生变化")
    if snapshot(payload["target"]) != payload["snapshot"]:
        raise PolicyError("数据库在预览后发生漂移")
    backup = None
    if payload["target"] == "production":
        stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
        backup_path = STATE_DIR / "backups" / f"production-full-before-yunye-{stamp}.sql.gz"
        backup = create_backup("production", backup_path)
        backup.update(verify_gzip(backup_path))
    run_mysql(payload["target"], build_sql(current_catalog), headers=False, write=True)
    after = fetch_json(payload["target"], f"""
SELECT JSON_OBJECT(
  'parent_name',(SELECT category_name FROM app_goods_category WHERE category_id={PARENT_ID}),
  'children',(SELECT COUNT(*) FROM app_goods_category WHERE parent_id={PARENT_ID}),
  'goods',(SELECT COUNT(*) FROM app_goods WHERE specifications LIKE 'YUNYE:%'),
  'published',(SELECT COUNT(*) FROM app_goods WHERE specifications LIKE 'YUNYE:%' AND status='1'),
  'drafts',(SELECT COUNT(*) FROM app_goods WHERE specifications LIKE 'YUNYE:%' AND status='0'));
""")
    expected = {"parent_name": "云南好物", "children": len(current_catalog["classes"]),
                "goods": len(current_catalog["goods"]),
                "published": sum(row["status"] == "1" for row in current_catalog["goods"])}
    expected["drafts"] = expected["goods"] - expected["published"]
    if after != expected:
        raise PolicyError(f"导入后校验失败: {after}")
    document.update({"status": "applied", "applied_at": datetime.now(timezone.utc).isoformat(),
                     "backup": backup, "postcondition": after})
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
    os.chmod(path, 0o600)
    return {"plan": str(path), "status": "applied", "backup": backup, "postcondition": after}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    preview = sub.add_parser("preview")
    preview.add_argument("--env", choices=("local", "production"), required=True)
    preview.add_argument("--source", type=Path, required=True)
    apply = sub.add_parser("apply")
    apply.add_argument("--plan", type=Path, required=True)
    apply.add_argument("--confirm-production")
    args = parser.parse_args()
    try:
        result = create_plan(args.env, args.source) if args.command == "preview" else apply_plan(
            args.plan, args.confirm_production)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return 0
    except (PolicyError, RuntimeFailure, OSError, ValueError, json.JSONDecodeError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
