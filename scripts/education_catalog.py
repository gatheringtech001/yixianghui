#!/usr/bin/env python3
"""Build and validate the fixed 2026 autumn education catalog import."""

from __future__ import annotations

import hashlib
import json
import re
import sys
from decimal import Decimal
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parent.parent
SKILL_SCRIPTS = REPO_ROOT / "codex-skills/manage-yixianghui/scripts"
sys.path.insert(0, str(SKILL_SCRIPTS))

from yxh_policy import PolicyError, sql_literal  # noqa: E402

TABLE_KEYS = {
    "app_goods_category": "category_id",
    "app_goods": "goods_id",
    "app_goods_education_ext": "ext_id",
    "app_goods_related": "id",
}
SECTION_META = (
    ("course_content", "课程内容"),
    ("signup_info", "报名信息"),
    ("signup_notice", "报名须知"),
)


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        while chunk := source.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def load_catalog(path: Path) -> dict[str, Any]:
    catalog = json.loads(path.read_text(encoding="utf-8"))
    validate_catalog(catalog, path.parent / "covers")
    return catalog


def _unique(values: list[Any], label: str) -> None:
    if len(values) != len(set(values)):
        raise PolicyError(f"Duplicate {label}")


def validate_catalog(catalog: dict[str, Any], covers_dir: Path) -> None:
    categories = catalog.get("categories") or []
    courses = catalog.get("courses") or []
    instructors = catalog.get("instructors") or {}
    if len(categories) != 5 or len(courses) != 15:
        raise PolicyError("Catalog must contain 5 categories and 15 courses")
    _unique([row["category_id"] for row in categories], "category IDs")
    _unique([row["name"] for row in categories], "category names")
    _unique([row["goods_id"] for row in courses], "goods IDs")
    _unique([row["ext_id"] for row in courses], "education extension IDs")
    related_ids = [item for row in courses for item in row["related_ids"]]
    _unique(related_ids, "related content IDs")
    category_ids = {row["category_id"] for row in categories}
    remote_dir = str(catalog.get("remote_asset_dir", ""))
    if not re.fullmatch(r"/profile/upload/[0-9/]+/[a-z0-9-]+", remote_dir):
        raise PolicyError("Invalid remote asset directory")
    for course in courses:
        if course["category_id"] not in category_ids:
            raise PolicyError(f"Unknown category for {course['name']}")
        if course["instructor_id"] not in instructors:
            raise PolicyError(f"Unknown instructor for {course['name']}")
        if len(course["related_ids"]) != len(SECTION_META):
            raise PolicyError(f"Expected three content sections for {course['name']}")
        if len(course["description"]) > 500 or Decimal(course["price"]) <= 0:
            raise PolicyError(f"Invalid description or price for {course['name']}")
        cover = str(course["cover"])
        if not re.fullmatch(r"[a-z0-9-]+\.jpg", cover):
            raise PolicyError(f"Invalid cover name: {cover}")
        cover_path = covers_dir / cover
        if not cover_path.is_file() or cover_path.read_bytes()[:2] != b"\xff\xd8":
            raise PolicyError(f"Missing or invalid cover: {cover_path}")


def cover_assets(catalog: dict[str, Any], covers_dir: Path) -> list[dict[str, Any]]:
    assets = []
    for course in catalog["courses"]:
        path = covers_dir / course["cover"]
        assets.append({
            "file": course["cover"],
            "path": str(path.resolve()),
            "bytes": path.stat().st_size,
            "sha256": sha256_file(path),
            "remote_path": f"{catalog['remote_asset_dir']}/{course['cover']}",
        })
    return assets


def category_rows(catalog: dict[str, Any]) -> list[dict[str, Any]]:
    rows = []
    for category in catalog["categories"]:
        rows.append({
            "category_id": category["category_id"], "parent_id": catalog["parent_category_id"],
            "parent_ids": "0,58", "category_name": category["name"], "category_icon": "",
            "is_hot": 0, "link_type": "goods", "link_id": 0, "remark": "",
            "order_num": category["order_num"], "status": "1",
        })
    return rows


def _course_section_html(catalog: dict[str, Any], course: dict[str, Any]) -> str:
    instructor = catalog["instructors"][course["instructor_id"]]
    return (
        course["course_content_html"]
        + "<h3>师资介绍</h3><p><strong>授课老师："
        + instructor["name"] + "</strong></p>" + instructor["bio_html"]
    )


def desired_rows(catalog: dict[str, Any]) -> dict[str, list[dict[str, Any]]]:
    goods_rows, ext_rows, related_rows = [], [], []
    for course in catalog["courses"]:
        cover = f"{catalog['remote_asset_dir']}/{course['cover']}"
        teacher = catalog["instructors"][course["instructor_id"]]["name"]
        goods_rows.append({
            "goods_id": course["goods_id"], "category_id": course["category_id"],
            "category_ids": "0", "dept_id": catalog["dept_id"], "goods_name": course["name"],
            "goods_cover": cover, "goods_images": cover, "description": course["description"],
            "tags": course["tags"], "price": course["price"], "vip_price": course["price"],
            "unit": "10节/期", "specifications": "线下小班", "stock": course["stock"],
            "goods_type": "education", "is_top": 0, "is_hot": 0, "attr_ids": None,
            "attr_values": None, "is_sku": 0, "award_type": None,
            "award_parent_ratio": None, "award_grand_parent_ratio": None,
            "award_golden": None, "content": None, "express_fee": "0.00", "weight": None,
            "status": "1",
        })
        ext_rows.append({
            "ext_id": course["ext_id"], "goods_id": course["goods_id"],
            "course_time": course["time"], "course_place": course["place"],
            "teacher_name": teacher, "lesson_count": catalog["lesson_count"],
            "class_size_max": catalog["class_size_max"],
            "class_size_min": catalog["class_size_min"], "start_date": catalog["start_date"],
            "signup_start": catalog["signup_start"], "signup_end": catalog["signup_end"],
            "material_note": course["material_note"], "consult_phone": catalog["consult_phone"],
        })
        contents = (_course_section_html(catalog, course), catalog["signup_info_html"],
                    catalog["signup_notice_html"])
        for related_id, (section_id, section_name), content in zip(
                course["related_ids"], SECTION_META, contents):
            related_rows.append({
                "id": related_id, "goods_id": course["goods_id"], "section_id": section_id,
                "section_name": section_name, "content": content,
                "sort_order": len([r for r in related_rows if r["goods_id"] == course["goods_id"]]) + 1,
                "min_content_length": 250,
            })
    return {
        "app_goods_category": category_rows(catalog), "app_goods": goods_rows,
        "app_goods_education_ext": ext_rows, "app_goods_related": related_rows,
    }


def _insert(table: str, row: dict[str, Any]) -> str:
    values = dict(row)
    if table in {"app_goods", "app_goods_education_ext", "app_goods_related"}:
        values["create_time"] = {"now": True}
    if table in {"app_goods", "app_goods_education_ext"}:
        values["update_time"] = {"now": True}
    columns = ", ".join(f"`{key}`" for key in values)
    literals = ", ".join("NOW()" if value == {"now": True} else sql_literal(value)
                         for value in values.values())
    return f"INSERT INTO `{table}` ({columns}) VALUES ({literals});"


def _update(table: str, row: dict[str, Any]) -> str:
    pk = TABLE_KEYS[table]
    values = {key: value for key, value in row.items() if key != pk}
    assignments = [f"`{key}`={sql_literal(value)}" for key, value in values.items()]
    if table in {"app_goods", "app_goods_education_ext"}:
        assignments.append("`update_time`=NOW()")
    return f"UPDATE `{table}` SET {', '.join(assignments)} WHERE `{pk}`={sql_literal(row[pk])};"


def _row_predicate(table: str, row: dict[str, Any]) -> str:
    predicates = []
    for key, value in row.items():
        if key in {"create_time", "update_time"}:
            continue
        literal = sql_literal(value)
        if isinstance(value, str):
            predicates.append(f"BINARY `{key}` <=> BINARY {literal}")
        else:
            predicates.append(f"`{key}` <=> {literal}")
    return " AND ".join(predicates)


def build_transaction_sql(catalog: dict[str, Any], snapshot: dict[str, Any]) -> str:
    desired = desired_rows(catalog)
    guards, statements = [], []
    for table, rows in desired.items():
        pk = TABLE_KEYS[table]
        ids = ",".join(str(row[pk]) for row in rows)
        before = snapshot["tables"][table]["rows"]
        guards.append(f"(SELECT COUNT(*) FROM `{table}` WHERE `{pk}` IN ({ids}))={len(before)}")
        guards.extend(
            f"EXISTS(SELECT 1 FROM `{table}` WHERE {_row_predicate(table, row)})"
            for row in before
        )
        before_ids = {row[pk] for row in before}
        statements.extend(_update(table, row) if row[pk] in before_ids else _insert(table, row)
                          for row in rows)
    names = ",".join(sql_literal(row["name"]) for row in catalog["courses"])
    guards.append(f"(SELECT COUNT(*) FROM app_goods WHERE goods_type='education' OR goods_name IN ({names}))=1")
    guards.append("EXISTS(SELECT 1 FROM app_goods_category WHERE category_id=58 AND category_name='老年教育')")
    guards.append("EXISTS(SELECT 1 FROM sys_dept WHERE dept_id=100 AND dept_name='上海智享居')")
    guard_sql = " AND ".join(f"({item})" for item in guards)
    return "\n".join([
        "START TRANSACTION;",
        "CREATE TEMPORARY TABLE yxh_education_guard (ok TINYINT NOT NULL);",
        f"INSERT INTO yxh_education_guard (ok) SELECT CASE WHEN {guard_sql} THEN 1 ELSE NULL END;",
        "DROP TEMPORARY TABLE yxh_education_guard;",
        *statements,
        "COMMIT;",
    ])


def summarize(catalog: dict[str, Any]) -> dict[str, Any]:
    return {
        "categories": [row["name"] for row in catalog["categories"]],
        "courses": [{"goods_id": row["goods_id"], "name": row["name"],
                     "price": row["price"], "stock": row["stock"]}
                    for row in catalog["courses"]],
        "row_impact": {"category_updates": 3, "category_inserts": 2,
                       "goods_updates": 1, "goods_inserts": 14,
                       "education_ext_updates": 1, "education_ext_inserts": 14,
                       "related_updates": 3, "related_inserts": 42},
    }
