#!/usr/bin/env python3
"""Build typed business tables and lossless relationships from Feishu export."""

import argparse
import gzip
import json
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo

from feishu_canonical_sql import build_canonical_sql
from feishu_structured_model import TARGET_TABLES, column_name, sql_type, storage_kind


def quote(value):
    if value in (None, ""):
        return "NULL"
    text = str(value)
    return f"CONVERT(0x{text.encode('utf-8').hex()} USING utf8mb4)"


def comment(value):
    return str(value).replace("'", "''")[:240]


def compact(value):
    if isinstance(value, list):
        return ",".join(str(compact(v)) for v in value if compact(v) not in (None, ""))
    if isinstance(value, dict):
        text_arr = value.get("text_arr") or []
        return (value.get("text") or value.get("name") or value.get("full_address")
                or value.get("address") or compact(text_arr)
                or json.dumps(value, ensure_ascii=False, sort_keys=True))
    return value


def datetime_value(value):
    if not isinstance(value, (int, float)):
        return None
    return datetime.fromtimestamp(value / 1000, ZoneInfo("Asia/Shanghai")).strftime("%Y-%m-%d %H:%M:%S")


def scalar_sql(value, kind):
    if value in (None, ""):
        return "NULL"
    if kind == "tinyint(1)":
        return "1" if bool(value) else "0"
    if kind == "datetime":
        return quote(datetime_value(value))
    if kind.startswith("decimal"):
        try:
            return str(float(value))
        except (TypeError, ValueError):
            return "NULL"
    return quote(compact(value))


def relation_items(value):
    if not isinstance(value, list):
        return []
    result = []
    for item in value:
        if not isinstance(item, dict):
            continue
        ids = item.get("record_ids") or []
        for record_id in ids:
            display = str(compact(item) or "")[:500]
            result.append((item.get("table_id"), record_id or "", display))
    return result


def user_items(value):
    items = value if isinstance(value, list) else [value]
    return [item for item in items if isinstance(item, dict)]


def attachment_items(value):
    return [item for item in value or [] if isinstance(item, dict)] if isinstance(value, list) else []


def build(export):
    tables = [(base, table) for base in export["bases"] for table in base["tables"]]
    record_count = sum(len(table["records"]) for _, table in tables)
    if len(tables) != 15 or record_count != 3246:
        raise ValueError(f"expected exactly 15 tables and 3246 records, got {record_count}")
    field_count = sum(len(table["fields"]) for _, table in tables)
    if field_count != 310:
        raise ValueError(f"expected 310 fields, got {field_count}")

    out = ["SET NAMES utf8mb4;"]
    ddl = [
        "CREATE TABLE IF NOT EXISTS app_feishu_business_relation (relation_id bigint unsigned NOT NULL AUTO_INCREMENT,source_table_id varchar(64) NOT NULL,source_record_id varchar(64) NOT NULL,source_field_id varchar(64) NOT NULL,target_source_table_id varchar(64) DEFAULT NULL,target_source_record_id varchar(64) NOT NULL DEFAULT '',target_business_table varchar(64) DEFAULT NULL,target_business_id bigint unsigned DEFAULT NULL,display_text varchar(500) DEFAULT NULL,relation_status varchar(16) NOT NULL DEFAULT 'unresolved',relation_message varchar(500) DEFAULT NULL,relation_order int unsigned NOT NULL DEFAULT 0,PRIMARY KEY(relation_id),UNIQUE KEY uk_feishu_business_relation(source_table_id,source_record_id,source_field_id,target_source_record_id,relation_order),KEY idx_feishu_relation_target(target_source_table_id,target_source_record_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;",
        "CREATE TABLE IF NOT EXISTS app_feishu_business_user (business_user_id bigint unsigned NOT NULL AUTO_INCREMENT,source_table_id varchar(64) NOT NULL,source_record_id varchar(64) NOT NULL,source_field_id varchar(64) NOT NULL,feishu_user_id varchar(128) DEFAULT NULL,user_name varchar(255) DEFAULT NULL,user_email varchar(255) DEFAULT NULL,user_order int unsigned NOT NULL DEFAULT 0,PRIMARY KEY(business_user_id),UNIQUE KEY uk_feishu_business_user(source_table_id,source_record_id,source_field_id,feishu_user_id,user_order)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;",
        "CREATE TABLE IF NOT EXISTS app_feishu_business_attachment (business_attachment_id bigint unsigned NOT NULL AUTO_INCREMENT,source_table_id varchar(64) NOT NULL,source_record_id varchar(64) NOT NULL,source_field_id varchar(64) NOT NULL,file_token varchar(255) DEFAULT NULL,file_name varchar(500) DEFAULT NULL,file_type varchar(100) DEFAULT NULL,file_size bigint unsigned DEFAULT NULL,file_url text,attachment_order int unsigned NOT NULL DEFAULT 0,PRIMARY KEY(business_attachment_id),UNIQUE KEY uk_feishu_business_attachment(source_table_id,source_record_id,source_field_id,file_token,attachment_order)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;",
    ]
    dml = [
        "DELETE FROM app_feishu_business_relation;",
        "DELETE FROM app_feishu_business_user;",
        "DELETE FROM app_feishu_business_attachment;",
    ]

    target_by_source = {table["table_id"]: TARGET_TABLES[(base["key"], table["name"])] for base, table in tables}
    for base, table in tables:
        target = TARGET_TABLES[(base["key"], table["name"])]
        fields = table["fields"]
        columns = []
        for field in fields:
            values = [r["fields"].get(field["field_name"]) for r in table["records"]]
            kind = sql_type(field.get("ui_type"), values)
            columns.append(f"`{column_name(field['field_id'])}` {kind} NULL COMMENT '{comment(field['field_name'])}'")
        ddl_columns = ",".join(columns)
        ddl.append(f"CREATE TABLE IF NOT EXISTS `{target}` (business_id bigint unsigned NOT NULL AUTO_INCREMENT,source_table_id varchar(64) NOT NULL,feishu_record_id varchar(64) NOT NULL,canonical_table varchar(64) DEFAULT NULL,canonical_id bigint unsigned DEFAULT NULL,canonical_status varchar(16) NOT NULL DEFAULT 'unresolved',canonical_message varchar(500) DEFAULT NULL,{ddl_columns},created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,PRIMARY KEY(business_id),UNIQUE KEY uk_{target}_record(feishu_record_id),KEY idx_{target}_canonical(canonical_table,canonical_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;")
        dml.append(f"DELETE FROM `{target}`;")

        scalar_fields = fields
        for record in table["records"]:
            names = ["source_table_id", "feishu_record_id"] + [column_name(f["field_id"]) for f in scalar_fields]
            vals = [quote(table["table_id"]), quote(record["record_id"])]
            for field in scalar_fields:
                values = [r["fields"].get(field["field_name"]) for r in table["records"]]
                vals.append(scalar_sql(record["fields"].get(field["field_name"]), sql_type(field.get("ui_type"), values)))
            updates = ",".join(f"`{name}`=VALUES(`{name}`)" for name in names[2:])
            dml.append(f"INSERT INTO `{target}` (`{'`,`'.join(names)}`) VALUES ({','.join(vals)}) ON DUPLICATE KEY UPDATE {updates};")
            for field in fields:
                value = record["fields"].get(field["field_name"])
                kind = storage_kind(field.get("ui_type"))
                if kind == "relation":
                    for order, (target_table_id, target_record_id, text) in enumerate(relation_items(value)):
                        target_business = target_by_source.get(target_table_id)
                        target_record_sql = quote(target_record_id) if target_record_id else "''"
                        dml.append("INSERT INTO app_feishu_business_relation (source_table_id,source_record_id,source_field_id,target_source_table_id,target_source_record_id,target_business_table,target_business_id,display_text,relation_status,relation_message,relation_order) VALUES ("
                                   f"{quote(table['table_id'])},{quote(record['record_id'])},{quote(field['field_id'])},{quote(target_table_id)},{target_record_sql},{quote(target_business)},NULL,{quote(text)},'unresolved','target not resolved',{order}) ON DUPLICATE KEY UPDATE display_text=VALUES(display_text),target_business_table=VALUES(target_business_table);")
                elif kind == "user":
                    for order, item in enumerate(user_items(value)):
                        dml.append("INSERT INTO app_feishu_business_user (source_table_id,source_record_id,source_field_id,feishu_user_id,user_name,user_email,user_order) VALUES ("
                                   f"{quote(table['table_id'])},{quote(record['record_id'])},{quote(field['field_id'])},{quote(item.get('id'))},{quote(item.get('name'))},{quote(item.get('email'))},{order}) ON DUPLICATE KEY UPDATE user_name=VALUES(user_name),user_email=VALUES(user_email);")
                elif kind == "attachment":
                    for order, item in enumerate(attachment_items(value)):
                        dml.append("INSERT INTO app_feishu_business_attachment (source_table_id,source_record_id,source_field_id,file_token,file_name,file_type,file_size,file_url,attachment_order) VALUES ("
                                   f"{quote(table['table_id'])},{quote(record['record_id'])},{quote(field['field_id'])},{quote(item.get('file_token'))},{quote(item.get('name'))},{quote(item.get('type'))},{int(item.get('size') or 0)},{quote(item.get('url'))},{order}) ON DUPLICATE KEY UPDATE file_name=VALUES(file_name),file_type=VALUES(file_type),file_size=VALUES(file_size),file_url=VALUES(file_url);")
        dml.append(f"UPDATE app_feishu_migration_record r JOIN `{target}` b ON b.feishu_record_id=r.source_record_id SET r.merge_status='merged',r.target_table={quote(target)},r.target_id=b.business_id,r.merge_message=NULL WHERE r.source_table_id={quote(table['table_id'])};")

    canonical_ddl, canonical_dml = build_canonical_sql(tables)
    out.extend(ddl + canonical_ddl + ["START TRANSACTION;"] + dml + canonical_dml)
    out.extend(["COMMIT;", "SELECT COUNT(*) AS merged_records FROM app_feishu_migration_record WHERE merge_status='merged';", "SELECT COUNT(*) AS unresolved_relations FROM app_feishu_business_relation WHERE relation_status<>'resolved';"])
    return "\n".join(out) + "\n"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    with gzip.open(args.input, "rt", encoding="utf-8") as source:
        export = json.load(source)
    args.output.write_text(build(export), encoding="utf-8")
    args.output.chmod(0o600)


if __name__ == "__main__":
    main()
