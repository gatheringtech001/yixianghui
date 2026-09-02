#!/usr/bin/env python3
"""Build the one-time, idempotent Feishu travel-order merge SQL."""

from __future__ import annotations

import argparse
import gzip
import json
from datetime import datetime
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any
from zoneinfo import ZoneInfo


GOODS_BY_BASE = {
    "大理一号基地": (37, "大理一号基地（洱海才村码头）"),
    "昆明一号基地": (53, "昆明一号基地"),
    "昆明二号基地": (56, "昆明二号市中心基地（四星级）"),
    "昆明四号基地": (61, "昆明四号基地"),
    "昆明五号基地": (57, "昆明五号基地"),
    "昆明六号温泉基地": (31, "昆明六号温泉基地"),
    "昆明七号古滇基地": (32, "昆明七号古滇基地"),
    "昆明八号基地": (58, "昆明八号新官渡基地"),
    "昆明九号基地": (55, "昆明九号世博基地（四钻）"),
    "昆明十号基地": (59, "昆明十号基地（暑期不涨价）"),
    "建水四号基地": (72, "建水四号基地（古城五钻酒店）"),
    "建水五号基地": (71, "建水五号基地"),
    "弥勒一号基地": (73, "弥勒一号温泉基地"),
    "弥勒二号基地": (77, "弥勒二号温泉基地（绿宝基地）"),
    "芒市广场基地": (94, "芒市广场基地"),
    "腾冲四号基地": (103, "腾冲四号基地（四钻）"),
}

TRAVEL_STATUS = {
    "待确认": "0", "已确认": "1", "已取消": "2", "已入住": "3",
    "已离店": "4", "已结算": "5",
}


def sql_text(value: Any) -> str:
    if value is None or value == "":
        return "NULL"
    text = str(value)
    return f"CONVERT(0x{text.encode('utf-8').hex()} USING utf8mb4)"


def relation(value: Any) -> tuple[str | None, str | None]:
    if not isinstance(value, list) or not value:
        return None, None
    item = value[0] if isinstance(value[0], dict) else {}
    record_ids = item.get("record_ids") or []
    return (record_ids[0] if record_ids else None), item.get("text")


def people(value: Any) -> str | None:
    if not isinstance(value, list):
        return None
    names = [item.get("name") for item in value if isinstance(item, dict) and item.get("name")]
    return ",".join(names) or None


def list_text(value: Any) -> str | None:
    if isinstance(value, list):
        return ",".join(str(item) for item in value) or None
    return str(value) if value not in (None, "") else None


def clipped(value: Any, limit: int) -> str | None:
    text = list_text(value)
    return text[:limit] if text else None


def number(value: Any) -> str:
    try:
        return str(Decimal(str(value)))
    except (InvalidOperation, ValueError):
        return "NULL"


def integer(value: Any) -> str:
    try:
        return str(max(0, int(Decimal(str(value)))))
    except (InvalidOperation, ValueError):
        return "NULL"


def mysql_time(value: Any) -> str | None:
    if not isinstance(value, (int, float)):
        return None
    return datetime.fromtimestamp(value / 1000, ZoneInfo("Asia/Shanghai")).strftime("%Y-%m-%d %H:%M:%S")


def build(input_path: Path) -> tuple[str, dict[str, int]]:
    with gzip.open(input_path, "rt", encoding="utf-8") as source:
        export = json.load(source)
    table = next(
        table for base in export["bases"] if base["key"] == "travel"
        for table in base["tables"] if table["name"] == "预订订单表"
    )
    records = table["records"]
    order_nos = [str(record["fields"].get("订单编号") or "").strip() for record in records]
    if any(not value for value in order_nos) or len(order_nos) != len(set(order_nos)):
        raise ValueError("Feishu order numbers must be non-empty and unique")

    statements = ["SET NAMES utf8mb4;", "START TRANSACTION;"]
    matched = 0
    for index, record in enumerate(records):
        fields = record["fields"]
        base_record_id, base_name = relation(fields.get("关联基地"))
        customer_record_id, _ = relation(fields.get("关联客户"))
        goods = GOODS_BY_BASE.get(base_name or "")
        matched += int(goods is not None)
        goods_id = goods[0] if goods else 0
        goods_name = goods[1] if goods else None
        verified_goods = (
            f"IFNULL((SELECT goods_id FROM app_goods WHERE goods_id={goods_id} AND goods_name={sql_text(goods_name)} LIMIT 1),0)"
            if goods else "0"
        )
        verified_dept = (
            f"IFNULL((SELECT dept_id FROM app_goods WHERE goods_id={goods_id} AND goods_name={sql_text(goods_name)} LIMIT 1),0)"
            if goods else "0"
        )
        travel_status = TRAVEL_STATUS.get(fields.get("订单状态"))
        amount = number(fields.get("消费金额"))
        source_json = json.dumps(fields, ensure_ascii=False, separators=(",", ":"), sort_keys=True)
        values = [
            "0", verified_goods, verified_dept, sql_text(order_nos[index]),
            sql_text("feishu_history"), sql_text(record["record_id"]), sql_text(order_nos[index]),
            sql_text(fields.get("渠道")), sql_text(customer_record_id), sql_text(base_record_id), sql_text(base_name),
            amount, amount, "NULL", sql_text("0"), sql_text(travel_status),
            sql_text(mysql_time(fields.get("入住日期"))), sql_text(mysql_time(fields.get("离店日期"))),
            sql_text(fields.get("客户名称")), sql_text(fields.get("联系方式")),
            sql_text(list_text(fields.get("房型"))), integer(fields.get("房间数")), integer(fields.get("同行人数")),
            sql_text(people(fields.get("客服负责人"))), sql_text(fields.get("服务备注")), sql_text(clipped(fields.get("备注"), 255)),
            sql_text(mysql_time(fields.get("创建时间"))), sql_text(mysql_time(fields.get("最后更新时间"))), sql_text(source_json),
        ]
        columns = (
            "user_id,goods_id,dept_id,order_no,order_origin,feishu_record_id,feishu_order_no,channel,"
            "travel_customer_record_id,travel_base_record_id,travel_base_name,money_total,money_payable,goods_count,"
            "status,travel_status,check_in_date,check_out_date,contact_name,contact_phone,room_type,room_count,"
            "traveler_count,service_owner,service_remark,remark,create_time,update_time,source_fields_json"
        )
        statements.append(
            f"INSERT INTO app_goods_order ({columns}) VALUES ({','.join(values)}) "
            "ON DUPLICATE KEY UPDATE feishu_order_no=VALUES(feishu_order_no),channel=VALUES(channel),"
            "travel_customer_record_id=VALUES(travel_customer_record_id),travel_base_record_id=VALUES(travel_base_record_id),"
            "travel_base_name=VALUES(travel_base_name),room_type=VALUES(room_type),room_count=VALUES(room_count),"
            "traveler_count=VALUES(traveler_count),service_owner=VALUES(service_owner),service_remark=VALUES(service_remark),"
            "source_fields_json=VALUES(source_fields_json),update_time=VALUES(update_time);"
        )
    statements.extend([
        "UPDATE app_feishu_migration_record m JOIN app_goods_order o ON BINARY o.feishu_record_id=BINARY m.source_record_id "
        f"SET m.merge_status='merged',m.target_table='app_goods_order',m.target_id=o.order_id,m.merge_message=NULL "
        f"WHERE m.source_table_id={sql_text(table['table_id'])};",
        "COMMIT;",
        "SELECT COUNT(*) AS feishu_history_orders FROM app_goods_order WHERE order_origin='feishu_history';",
        "SELECT COUNT(*) AS unmatched_goods FROM app_goods_order WHERE order_origin='feishu_history' AND goods_id=0;",
    ])
    return "\n".join(statements) + "\n", {
        "orders": len(records), "matched_goods": matched, "unmatched_goods": len(records) - matched,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    sql, report = build(args.input)
    args.output.write_text(sql, encoding="utf-8")
    args.output.chmod(0o600)
    print(json.dumps(report, ensure_ascii=False, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
