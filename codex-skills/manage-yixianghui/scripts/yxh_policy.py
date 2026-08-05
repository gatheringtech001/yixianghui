#!/usr/bin/env python3
"""SQL and content-mutation guardrails for Yixianghui."""

from __future__ import annotations

import hashlib
import json
import math
import re
from typing import Any

CONTENT_POLICIES: dict[str, set[str]] = {
    "app_goods": {"update"},
    "app_goods_category": {"insert", "update"},
    "app_goods_sku": {"update"},
    "app_goods_sku_option": {"insert", "update", "delete"},
    "app_goods_related": {"insert", "update", "delete"},
    "app_goods_education_ext": {"insert", "update", "delete"},
    "app_activity": {"insert", "update"},
    "app_activity_category": {"insert", "update"},
    "app_ad_content": {"insert", "update", "delete"},
    "app_ad_position": {"insert", "update"},
    "app_article": {"insert", "update", "delete"},
    "app_article_category": {"insert", "update"},
    "app_single_page": {"update"},
    "app_site_nav": {"insert", "update", "delete"},
}

BLOCKED_COLUMNS = {
    "create_time", "update_time", "view_count", "sale_count", "sign_count",
    "pay_status", "pay_money", "money_total", "money_payable", "money_discount",
}
IMMUTABLE_BY_TABLE = {
    "app_ad_position": {"position_code"},
    "app_single_page": {"page_key"},
}
SENSITIVE = re.compile(
    r"(?:password|passwd|secret|token|openid|unionid|idcard|identity|mobile|phone|"
    r"bank|account|address|notify_content)", re.IGNORECASE
)
BLOCKED_READ = re.compile(
    r"\b(?:insert|update|delete|replace|create|alter|drop|truncate|grant|revoke|"
    r"call|do|set|lock|unlock|load_file|outfile|dumpfile|sleep|benchmark|for\s+update)\b",
    re.IGNORECASE,
)
BLOCKED_SCHEMA = re.compile(r"\b(?:mysql|performance_schema|sys)\s*\.", re.IGNORECASE)


class PolicyError(ValueError):
    pass


def require_identifier(value: str) -> str:
    if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_]*", value):
        raise PolicyError(f"Invalid SQL identifier: {value}")
    return value


def _inspection_text(sql: str) -> str:
    output: list[str] = []
    quote: str | None = None
    index = 0
    while index < len(sql):
        char = sql[index]
        if quote:
            if char == "\\":
                index += 2
                output.extend("  ")
                continue
            if char == quote:
                quote = None
            output.append(" ")
            index += 1
            continue
        if char in "'\"":
            quote = char
            output.append(" ")
            index += 1
            continue
        if sql.startswith("/*", index) or char == "#" or (
            sql.startswith("--", index) and index + 2 < len(sql) and sql[index + 2].isspace()
        ):
            raise PolicyError("SQL comments are not allowed")
        output.append(char)
        index += 1
    if quote:
        raise PolicyError("Unterminated SQL string")
    return "".join(output)


def validate_read_sql(sql: str, *, production: bool,
                      include_sensitive: bool = False) -> str:
    stripped = sql.strip()
    if not stripped:
        raise PolicyError("SQL is empty")
    inspect = _inspection_text(stripped)
    semicolons = [match.start() for match in re.finditer(";", inspect)]
    if len(semicolons) > 1 or (semicolons and inspect[semicolons[0] + 1:].strip()):
        raise PolicyError("Only one SQL statement is allowed")
    inspect = inspect.rstrip().rstrip(";").strip()
    first = re.match(r"([A-Za-z]+)", inspect)
    if not first or first.group(1).lower() not in {"select", "with", "show", "describe", "desc", "explain"}:
        raise PolicyError("Only read-only SELECT/SHOW/DESCRIBE/EXPLAIN is allowed")
    if BLOCKED_READ.search(inspect) or BLOCKED_SCHEMA.search(inspect):
        raise PolicyError("SQL contains a blocked operation or schema")
    if re.search(r"\binformation_schema\b", inspect, re.IGNORECASE):
        raise PolicyError("Use the catalog/schema command for information_schema")
    if production and re.search(r"\bselect\s+\*", inspect, re.IGNORECASE):
        raise PolicyError("SELECT * is blocked in production")
    if SENSITIVE.search(inspect) and not include_sensitive:
        raise PolicyError("Sensitive columns require --include-sensitive and a reason")
    statement = stripped.rstrip().rstrip(";")
    if first.group(1).lower() in {"select", "with"} and not re.search(r"\blimit\s+\d+", inspect, re.IGNORECASE):
        statement += " LIMIT 200"
    return statement + ";"


def check_content_change(table: str, action: str, columns: set[str], pk: str | None) -> None:
    require_identifier(table)
    allowed = CONTENT_POLICIES.get(table, set())
    if action not in allowed:
        raise PolicyError(f"{action} is not allowed for {table}")
    for column in columns:
        require_identifier(column)
    forbidden = BLOCKED_COLUMNS | IMMUTABLE_BY_TABLE.get(table, set())
    if pk:
        forbidden.add(pk)
    blocked = sorted(columns & forbidden)
    if blocked:
        raise PolicyError("Blocked columns: " + ", ".join(blocked))


def sql_literal(value: Any) -> str:
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "1" if value else "0"
    if isinstance(value, (int, float)) and not isinstance(value, bool):
        if isinstance(value, float) and not math.isfinite(value):
            raise PolicyError("Non-finite numbers are not allowed")
        return str(value)
    if value == "":
        return "''"
    encoded = str(value).encode("utf-8").hex()
    return f"CONVERT(0x{encoded} USING utf8mb4)"


def plan_token(payload: dict[str, Any]) -> str:
    canonical = json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()[:16]
