#!/usr/bin/env python3
"""Build deterministic MySQL upserts from a lossless Feishu Base export."""

from __future__ import annotations

import argparse
import gzip
import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


def sql_text(value: str | None) -> str:
    if value is None:
        return "NULL"
    return f"CONVERT(0x{value.encode('utf-8').hex()} USING utf8mb4)"


def compact_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"), sort_keys=True)


def build(input_path: Path, schema_path: Path) -> str:
    with gzip.open(input_path, "rt", encoding="utf-8") as source:
        export = json.load(source)
    if export.get("formatVersion") != 1:
        raise ValueError("Unsupported Feishu export format")

    imported_at = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")
    statements = ["SET NAMES utf8mb4;", schema_path.read_text(encoding="utf-8"), "START TRANSACTION;"]
    table_count = field_count = record_count = 0

    for base in export["bases"]:
        base_key = base["key"]
        for table in base["tables"]:
            table_id = table["table_id"]
            fields = table["fields"]
            records = table["records"]
            statements.append(
                "INSERT INTO app_feishu_migration_table "
                "(base_key,source_table_id,source_table_name,source_revision,field_count,record_count,imported_at) VALUES ("
                f"{sql_text(base_key)},{sql_text(table_id)},{sql_text(table['name'])},{int(table.get('revision') or 0)},"
                f"{len(fields)},{len(records)},{sql_text(imported_at)}) "
                "ON DUPLICATE KEY UPDATE base_key=VALUES(base_key),source_table_name=VALUES(source_table_name),"
                "source_revision=VALUES(source_revision),field_count=VALUES(field_count),record_count=VALUES(record_count),"
                "imported_at=VALUES(imported_at);"
            )
            table_count += 1

            for field in fields:
                property_json = compact_json(field.get("property")) if field.get("property") is not None else None
                statements.append(
                    "INSERT INTO app_feishu_migration_field "
                    "(source_table_id,source_field_id,source_field_name,source_field_type,source_ui_type,is_primary,property_json,imported_at) VALUES ("
                    f"{sql_text(table_id)},{sql_text(field['field_id'])},{sql_text(field['field_name'])},{int(field['type'])},"
                    f"{sql_text(field.get('ui_type'))},{1 if field.get('is_primary') else 0},{sql_text(property_json)},{sql_text(imported_at)}) "
                    "ON DUPLICATE KEY UPDATE source_field_name=VALUES(source_field_name),source_field_type=VALUES(source_field_type),"
                    "source_ui_type=VALUES(source_ui_type),is_primary=VALUES(is_primary),property_json=VALUES(property_json),"
                    "imported_at=VALUES(imported_at);"
                )
                field_count += 1

            for record in records:
                fields_json = compact_json(record["fields"])
                record_hash = hashlib.sha256(fields_json.encode("utf-8")).hexdigest()
                statements.append(
                    "INSERT INTO app_feishu_migration_record "
                    "(source_table_id,source_record_id,fields_json,record_hash,merge_status,imported_at) VALUES ("
                    f"{sql_text(table_id)},{sql_text(record['record_id'])},{sql_text(fields_json)},{sql_text(record_hash)},"
                    f"'pending',{sql_text(imported_at)}) ON DUPLICATE KEY UPDATE fields_json=VALUES(fields_json),"
                    "record_hash=VALUES(record_hash),imported_at=VALUES(imported_at);"
                )
                record_count += 1

    statements.extend(
        [
            "COMMIT;",
            "SELECT COUNT(*) AS tables FROM app_feishu_migration_table;",
            "SELECT COUNT(*) AS fields FROM app_feishu_migration_field;",
            "SELECT COUNT(*) AS records FROM app_feishu_migration_record;",
        ]
    )
    if table_count != 15 or record_count != export.get("recordCount"):
        raise ValueError(f"Export count mismatch: tables={table_count}, records={record_count}")
    return "\n".join(statements) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--schema", default=Path("sql/app_feishu_migration_archive.sql"), type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    sql = build(args.input, args.schema)
    args.output.write_text(sql, encoding="utf-8")
    args.output.chmod(0o600)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
