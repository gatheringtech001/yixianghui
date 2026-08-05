#!/usr/bin/env python3
"""Safe CLI for Yixianghui database administration."""

from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

from yxh_plans import STATE_DIR, apply_plan, create_plan
from yxh_policy import PolicyError, require_identifier, sql_literal, validate_read_sql
from yxh_runtime import RuntimeFailure, create_backup, run_mysql, verify_gzip

AUDITS = {
    "relations": """
SELECT 'sku_without_goods' AS check_name, COUNT(*) AS issue_count
FROM app_goods_sku s LEFT JOIN app_goods g ON g.goods_id=s.goods_id WHERE g.goods_id IS NULL
UNION ALL SELECT 'sku_option_without_goods', COUNT(*)
FROM app_goods_sku_option o LEFT JOIN app_goods g ON g.goods_id=o.goods_id WHERE g.goods_id IS NULL
UNION ALL SELECT 'related_without_goods', COUNT(*)
FROM app_goods_related r LEFT JOIN app_goods g ON g.goods_id=r.goods_id WHERE g.goods_id IS NULL
UNION ALL SELECT 'education_ext_without_goods', COUNT(*)
FROM app_goods_education_ext e LEFT JOIN app_goods g ON g.goods_id=e.goods_id WHERE g.goods_id IS NULL
UNION ALL SELECT 'order_detail_without_order', COUNT(*)
FROM app_goods_order_detail d LEFT JOIN app_goods_order o ON o.order_id=d.order_id WHERE o.order_id IS NULL;
""",
    "commerce": """
SELECT 'pay_log_without_order' AS check_name, COUNT(*) AS issue_count
FROM app_pay_log p LEFT JOIN app_goods_order o ON o.order_id=p.order_id
WHERE p.order_type='goods' AND o.order_id IS NULL
UNION ALL SELECT 'refund_exceeds_payment', COUNT(*) FROM (
  SELECT r.order_id FROM app_pay_refund_log r
  LEFT JOIN app_pay_log p ON p.order_id=r.order_id
  GROUP BY r.order_id HAVING COALESCE(SUM(r.refund_money),0) > COALESCE(MAX(p.pay_money),0)
) x
UNION ALL SELECT 'negative_order_money', COUNT(*) FROM app_goods_order
WHERE money_total < 0 OR money_payable < 0 OR pay_money < 0;
""",
    "content": """
SELECT 'education_goods_without_ext' AS check_name, COUNT(*) AS issue_count
FROM app_goods g LEFT JOIN app_goods_education_ext e ON e.goods_id=g.goods_id
WHERE g.goods_type='education' AND e.goods_id IS NULL
UNION ALL SELECT 'ad_without_position', COUNT(*)
FROM app_ad_content c LEFT JOIN app_ad_position p ON p.position_id=c.position_id
WHERE p.position_id IS NULL
UNION ALL SELECT 'article_without_category', COUNT(*)
FROM app_article a LEFT JOIN app_article_category c ON c.category_id=a.category_id
WHERE a.category_id IS NOT NULL AND c.category_id IS NULL;
""",
    "assets": """
SELECT 'goods_empty_cover' AS check_name, COUNT(*) AS issue_count
FROM app_goods WHERE status='0' AND (goods_cover IS NULL OR goods_cover='')
UNION ALL SELECT 'activity_empty_cover', COUNT(*) FROM app_activity
WHERE status='0' AND (activity_cover IS NULL OR activity_cover='')
UNION ALL SELECT 'ad_empty_image_and_content', COUNT(*) FROM app_ad_content
WHERE status='0' AND COALESCE(ad_image,'')='' AND COALESCE(ad_content,'')=''
UNION ALL SELECT 'insecure_http_asset', COUNT(*) FROM (
  SELECT goods_cover AS asset FROM app_goods UNION ALL
  SELECT activity_cover FROM app_activity UNION ALL SELECT ad_image FROM app_ad_content
) a WHERE asset LIKE 'http://%';
""",
}


def _json_data(args: argparse.Namespace) -> dict:
    if args.data_file:
        raw = Path(args.data_file).read_text(encoding="utf-8")
    elif args.data:
        raw = args.data
    else:
        raise PolicyError("Provide --data or --data-file")
    value = json.loads(raw)
    if not isinstance(value, dict) or not value:
        raise PolicyError("Change data must be a non-empty JSON object")
    return value


def _print_json(value: object) -> None:
    print(json.dumps(value, ensure_ascii=False, indent=2))


def command_catalog(args: argparse.Namespace) -> None:
    sql = """
SELECT COUNT(*) AS tables,
  SUM(table_name LIKE 'app\\_%') AS app_tables,
  SUM(table_name LIKE 'sys\\_%') AS sys_tables,
  SUM(table_name LIKE 'gen\\_%') AS gen_tables,
  SUM(table_name LIKE 'qrtz\\_%') AS qrtz_tables
FROM information_schema.tables WHERE table_schema=DATABASE();
SELECT table_name, table_rows FROM information_schema.tables
WHERE table_schema=DATABASE() ORDER BY table_name;
"""
    print(run_mysql(args.env, sql), end="")


def command_schema(args: argparse.Namespace) -> None:
    table = require_identifier(args.table)
    sql = f"""
SELECT column_name, column_type, is_nullable, column_default, column_key, extra
FROM information_schema.columns
WHERE table_schema=DATABASE() AND table_name={sql_literal(table)} ORDER BY ordinal_position;
"""
    print(run_mysql(args.env, sql), end="")


def command_query(args: argparse.Namespace) -> None:
    if args.sql_file:
        sql = Path(args.sql_file).read_text(encoding="utf-8")
    else:
        sql = args.sql or ""
    if args.include_sensitive and not args.reason:
        raise PolicyError("--include-sensitive requires --reason")
    checked = validate_read_sql(sql, production=args.env == "production",
                                include_sensitive=args.include_sensitive)
    print(run_mysql(args.env, checked), end="")


def command_plan(args: argparse.Namespace) -> None:
    data = {} if args.action == "delete" else _json_data(args)
    _, preview = create_plan(args.env, args.action, args.table, data, args.id)
    _print_json(preview)


def command_apply(args: argparse.Namespace) -> None:
    result = apply_plan(Path(args.plan), production_confirmation=args.confirm_production,
                        delete_confirmation=args.confirm_delete)
    _print_json(result)


def command_audit(args: argparse.Namespace) -> None:
    checks = AUDITS if args.check == "all" else {args.check: AUDITS[args.check]}
    for name, sql in checks.items():
        print(f"[{name}]")
        print(run_mysql(args.env, sql), end="")


def command_backup(args: argparse.Namespace) -> None:
    if args.output:
        output = Path(args.output)
    else:
        stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
        suffix = "schema" if args.schema_only else "full"
        output = STATE_DIR / "backups" / f"{args.env}-{suffix}-{stamp}.sql.gz"
    result = create_backup(args.env, output, schema_only=args.schema_only)
    result.update(verify_gzip(output))
    _print_json(result)


def command_verify_backup(args: argparse.Namespace) -> None:
    _print_json(verify_gzip(Path(args.file)))


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)

    def target(command: argparse.ArgumentParser) -> None:
        command.add_argument("--env", choices=("local", "production"), default="local")

    catalog = sub.add_parser("catalog"); target(catalog); catalog.set_defaults(func=command_catalog)
    schema = sub.add_parser("schema"); target(schema); schema.add_argument("--table", required=True); schema.set_defaults(func=command_schema)
    query = sub.add_parser("query"); target(query)
    query.add_argument("--sql"); query.add_argument("--sql-file")
    query.add_argument("--include-sensitive", action="store_true"); query.add_argument("--reason")
    query.set_defaults(func=command_query)
    plan = sub.add_parser("plan"); target(plan)
    plan.add_argument("--action", choices=("insert", "update", "delete"), required=True)
    plan.add_argument("--table", required=True); plan.add_argument("--id")
    plan.add_argument("--data"); plan.add_argument("--data-file"); plan.set_defaults(func=command_plan)
    apply = sub.add_parser("apply"); apply.add_argument("--plan", required=True)
    apply.add_argument("--confirm-production"); apply.add_argument("--confirm-delete"); apply.set_defaults(func=command_apply)
    audit = sub.add_parser("audit"); target(audit)
    audit.add_argument("--check", choices=("all", *AUDITS), default="all"); audit.set_defaults(func=command_audit)
    backup = sub.add_parser("backup"); target(backup)
    backup.add_argument("--schema-only", action="store_true"); backup.add_argument("--output"); backup.set_defaults(func=command_backup)
    verify = sub.add_parser("verify-backup"); verify.add_argument("--file", required=True); verify.set_defaults(func=command_verify_backup)
    return parser


def main() -> int:
    try:
        args = build_parser().parse_args()
        args.func(args)
        return 0
    except (PolicyError, RuntimeFailure, OSError, json.JSONDecodeError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
