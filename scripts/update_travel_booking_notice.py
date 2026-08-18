#!/usr/bin/env python3
"""Safely publish the payment-step travel notice from the approved DOCX."""

from __future__ import annotations

import argparse
import hashlib
import html
import json
import os
import re
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any

from docx import Document

from import_travel_catalog import STATE_DIR, PolicyError, RuntimeFailure, _schema, _schema_signature
from yxh_policy import plan_token, sql_literal
from yxh_runtime import run_mysql

NOTICE_ID = 1
PUBLISH_DATE = "2026年8月20日"
SOURCE_DATE = "202X年X月XX日"
PLAN_TTL = timedelta(minutes=30)


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        while chunk := source.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def extract_notice(path: Path) -> dict[str, str]:
    document = Document(path)
    paragraphs = [row for row in document.paragraphs if row.text.strip()]
    if len(paragraphs) != 47 or paragraphs[-1].text.strip() != SOURCE_DATE:
        raise PolicyError("The approved travel notice DOCX structure or date placeholder changed")
    title = paragraphs[0].text.strip()
    if title != "逸享旅居平台预订及入住须知":
        raise PolicyError("The approved travel notice title changed")
    numbered = sum(bool(re.match(r"^\d+\.\s*", row.text.strip())) for row in paragraphs)
    if numbered != 17:
        raise PolicyError("The approved travel notice must contain exactly 17 numbered clauses")
    fragments = []
    for row in paragraphs[1:]:
        text = PUBLISH_DATE if row.text.strip() == SOURCE_DATE else row.text.strip()
        escaped = html.escape(text)
        if row.style.name == "Heading 1":
            fragments.append(f"<h3>{escaped}</h3>")
        elif re.match(r"^\d+\.\s*", text):
            fragments.append(f"<p><strong>{escaped}</strong></p>")
        elif row.alignment == 2:
            fragments.append(f'<p style="text-align:right;">{escaped}</p>')
        else:
            fragments.append(f"<p>{escaped}</p>")
    content = "".join(fragments)
    if SOURCE_DATE in content or content.count(PUBLISH_DATE) != 1:
        raise PolicyError("The publication date replacement is invalid")
    return {"notice_title": title, "notice_content": content}


def capture_notice(target: str) -> dict[str, Any]:
    raw = run_mysql(target, (
        "SELECT JSON_OBJECT('notice_id',notice_id,'notice_title',notice_title,"
        "'notice_type',notice_type,'notice_content',CAST(notice_content AS CHAR),"
        "'status',status) FROM sys_notice WHERE notice_id=1;"
    ), headers=False).strip()
    if not raw:
        raise PolicyError("Payment-step travel notice 1 does not exist")
    row = json.loads(raw)
    if row["notice_id"] != NOTICE_ID or row["status"] != "0":
        raise PolicyError("Payment-step travel notice 1 is not active")
    return row


def build_transaction_sql(snapshot: dict[str, Any], desired: dict[str, str]) -> str:
    digest = hashlib.sha256((snapshot["notice_content"] or "").encode()).hexdigest()
    guard = (
        "EXISTS(SELECT 1 FROM sys_notice WHERE notice_id=1 "
        f"AND BINARY notice_title=BINARY {sql_literal(snapshot['notice_title'])} "
        f"AND BINARY notice_type=BINARY {sql_literal(snapshot['notice_type'])} "
        f"AND BINARY status=BINARY {sql_literal(snapshot['status'])} "
        f"AND SHA2(COALESCE(CAST(notice_content AS CHAR),''),256)={sql_literal(digest)})"
    )
    return "\n".join([
        "START TRANSACTION;",
        "CREATE TEMPORARY TABLE yxh_notice_guard (ok TINYINT NOT NULL);",
        f"INSERT INTO yxh_notice_guard (ok) SELECT CASE WHEN {guard} THEN 1 ELSE NULL END;",
        "DROP TEMPORARY TABLE yxh_notice_guard;",
        "UPDATE sys_notice SET "
        f"notice_title={sql_literal(desired['notice_title'])},"
        f"notice_content={sql_literal(desired['notice_content'])},update_time=NOW() "
        "WHERE notice_id=1;",
        "COMMIT;",
    ])


def _write_plan(document: dict[str, Any]) -> Path:
    directory = STATE_DIR / "audit/travel-notice-plans"
    directory.mkdir(parents=True, exist_ok=True)
    path = directory / f"{datetime.now(timezone.utc):%Y%m%d-%H%M%S}-{document['token']}.json"
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
    os.chmod(path, 0o600)
    return path


def create_plan(target: str, source: Path) -> tuple[Path, dict[str, Any]]:
    desired = extract_notice(source)
    snapshot = capture_notice(target)
    payload = {
        "created_at": datetime.now(timezone.utc).isoformat(), "target": target,
        "source": str(source.resolve()), "source_sha256": sha256_file(source),
        "schema": _schema_signature(_schema(target, "sys_notice")),
        "snapshot": snapshot, "desired": desired,
    }
    token = plan_token(payload)
    path = _write_plan({"token": token, "status": "planned", "payload": payload})
    return path, {"plan": str(path), "token": token, "target": target,
                  "notice_id": NOTICE_ID, "title_before": snapshot["notice_title"],
                  "title_after": desired["notice_title"],
                  "content_length_before": len(snapshot["notice_content"] or ""),
                  "content_length_after": len(desired["notice_content"]),
                  "publish_date": PUBLISH_DATE}


def _load(path: Path) -> dict[str, Any]:
    document = json.loads(path.read_text(encoding="utf-8"))
    payload = document.get("payload") or {}
    if document.get("status") != "planned" or document.get("token") != plan_token(payload):
        raise PolicyError("Notice plan is invalid or no longer pending")
    if datetime.now(timezone.utc) - datetime.fromisoformat(payload["created_at"]) > PLAN_TTL:
        raise PolicyError("Notice plan expired; create a fresh preview")
    return document


def apply_plan(path: Path, confirmation: str | None) -> dict[str, Any]:
    document = _load(path)
    payload, token = document["payload"], document["token"]
    if payload["target"] == "production" and confirmation != token:
        raise PolicyError("Production apply requires the exact plan token")
    source = Path(payload["source"])
    if sha256_file(source) != payload["source_sha256"] or extract_notice(source) != payload["desired"]:
        raise PolicyError("The approved notice DOCX changed after preview")
    if _schema_signature(_schema(payload["target"], "sys_notice")) != payload["schema"]:
        raise PolicyError("sys_notice schema changed after preview")
    if capture_notice(payload["target"]) != payload["snapshot"]:
        raise PolicyError("Payment-step travel notice changed after preview")
    run_mysql(payload["target"], build_transaction_sql(payload["snapshot"], payload["desired"]),
              headers=False, write=True)
    actual = capture_notice(payload["target"])
    if any(actual[key] != value for key, value in payload["desired"].items()):
        raise PolicyError("Payment-step travel notice postcondition failed")
    document.update({"status": "applied", "applied_at": datetime.now(timezone.utc).isoformat()})
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
    os.chmod(path, 0o600)
    return {"plan": str(path), "token": token, "status": "applied", "notice_id": NOTICE_ID}


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
        _, result = create_plan(args.env, args.source) if args.command == "preview" else (None, apply_plan(args.plan, args.confirm_production))
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return 0
    except (PolicyError, RuntimeFailure, OSError, ValueError) as error:
        print(str(error), file=__import__("sys").stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
