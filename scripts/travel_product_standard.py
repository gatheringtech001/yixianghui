#!/usr/bin/env python3
"""Extract and validate knowledge-backed travel products using travel_product.v1."""

from __future__ import annotations

import json
import re
from datetime import datetime, timezone
from decimal import Decimal
from pathlib import Path
from typing import Any

from travel_asset_policy import COVER_ASSET_INDEX
from travel_catalog import _load_extractor
from travel_price_parser import Quote, extract_quotes

SCHEMA_VERSION = "travel_product.v1"
SECTION_NAMES = {
    "overview": "基地概览",
    "dining": "餐饮",
    "transport": "交通接送",
    "facilities": "温泉与设施",
    "attractions": "周边景点",
    "stay_notice": "入住须知",
}
CONTACT_RE = re.compile(
    r"(?<!\d)1[3-9]\d{9}(?!\d)|二维码|扫码|微信|联系(?:电话|方式)|咨询客服|客服"
)
PRICE_RE = re.compile(r"\d+\s*元\s*(?:/|／)?\s*(?:人|间)")
MEDICAL_RE = re.compile(
    r"疗效|糖尿病|心脑血管|皮肤病|痛风|三高|排除湿气|体内.*湿气|"
    r"清洁皮肤|缓解疼痛|促进睡眠"
)
RATING_RE = re.compile(r"[四五]钻|[四五]星(?:级)?")
BED_ANOMALY_RE = re.compile(r"双床房.{0,24}1张2米")
GARBLED_RE = re.compile(r"可麻烦使用")
MARKETING_RE = re.compile(
    r"经营理念|宾至如家|低调.*奢华|被誉为|传说|灵气|素有|医学界公认|"
    r"最适合|天然氧吧|显著|最具|最大的|点赞"
)
ATTRACTION_RE = re.compile(
    r"东风韵|太平湖|葡萄酒|锦屏寺|湖泉生态园|可邑小镇|景区|景点|古城"
)
CONFLICT_RE = re.compile(
    r"每日.*(?:打扫|清洁)|每周.*(?:清洗|清洁|清扫)|免费接(?:站|送)|"
    r"接(?:站|送).{0,12}\d+\s*元|不可退餐.{0,8}退差价"
)
SECTION_RULES = (
    ("stay_notice", re.compile(r"儿童|节假日|取消|退款|退订|入住前|入住当天|房损|押金")),
    ("dining", re.compile(r"一日三餐|三餐|餐食|自助餐|牛奶|餐厅")),
    ("transport", re.compile(r"公交|车站|机场|高铁|接站|接送")),
    ("attractions", ATTRACTION_RE),
    ("facilities", re.compile(r"温泉|健身房|茶室|棋牌室|会议室|泡池")),
    ("overview", re.compile(r"基地|酒店|位于|坐落|毗邻|客房|房间|大床房|双床房|商业圈")),
)

def _clean_text(value: str) -> str:
    value = re.sub(r"\s+", " ", value).strip(" ·•\t\r\n")
    value = re.sub(r"^[^\w\u4e00-\u9fff]+", "", value)
    return re.sub(r"^(?:\d+|[一二三四五六七八九十]+)[.、]\s*", "", value)

def _clauses(value: str) -> list[str]:
    parts = re.split(r"[。！？!?；;]|(?=[·•])", _clean_text(value))
    return [text for part in parts if len(text := _clean_text(part)) >= 4]

def _issue(code: str, field: str, message: str, refs: list[int],
           excerpts: list[str]) -> dict[str, Any]:
    return {
        "code": code,
        "severity": "warning",
        "field": field,
        "message": message,
        "source_refs": sorted(set(refs)),
        "source_excerpts": excerpts,
    }

def _matching(items: list[dict[str, Any]], pattern: re.Pattern[str]) -> list[tuple[int, str]]:
    return [
        (index, _clean_text(str(item.get("text") or "")))
        for index, item in enumerate(items)
        if pattern.search(str(item.get("text") or ""))
    ]

def _quality_issues(items: list[dict[str, Any]], quotes: list[Quote]) -> list[dict[str, Any]]:
    issues: list[dict[str, Any]] = []
    checks = (
        ("CONFLICT_HOUSEKEEPING", "content_sections.stay_notice",
         re.compile(r"每日.*(?:打扫|清洁)"), re.compile(r"每周.*(?:清洗|清洁|清扫)"),
         "清洁频次存在“每日”和“每周两次”两种口径，发布前需确认。"),
        ("CONFLICT_PICKUP_FEE", "content_sections.transport",
         re.compile(r"免费接(?:站|送)"), re.compile(r"接(?:站|送).{0,12}\d+\s*元"),
         "接站政策同时出现免费与收费口径，发布前需确认适用条件。"),
        ("CONFLICT_RATING", "display.tags",
         re.compile(r"四钻"), re.compile(r"四星(?:级)?"),
         "基地等级同时使用“四钻”和“四星级标准”，不得作为已确认标签。"),
    )
    for code, field, left, right, message in checks:
        matches = _matching(items, left) + _matching(items, right)
        if _matching(items, left) and _matching(items, right):
            issues.append(_issue(code, field, message, [row[0] for row in matches],
                                 [row[1] for row in matches]))
    meal = _matching(items, re.compile(r"不可退餐.{0,8}退差价"))
    if meal:
        issues.append(_issue("CONFLICT_MEAL_REFUND", "content_sections.dining",
                             "“不可退餐，退差价”语义矛盾，暂不展示退餐规则。",
                             [row[0] for row in meal], [row[1] for row in meal]))
    ambiguous = [quote for quote in quotes if re.fullmatch(r"\d+天", quote.duration)]
    if ambiguous:
        issues.append(_issue("AMBIGUOUS_NIGHTS", "pricing.room_packages",
                             "来源只写住宿天数，未明确对应晚数；不得自动改写为“天晚”。",
                             [ref for row in ambiguous for ref in row.source_refs],
                             sorted({row.duration for row in ambiguous})))
    beds = _matching(items, BED_ANOMALY_RE)
    if beds:
        issues.append(_issue("SUSPICIOUS_BED_TYPE", "content_sections.overview",
                             "双床房床型包含2米特大床，房型名称与床型需人工核对。",
                             [row[0] for row in beds], [row[1] for row in beds]))
    medical = _matching(items, MEDICAL_RE)
    if medical:
        issues.append(_issue("WITHHELD_MEDICAL_CLAIMS", "content_sections.facilities",
                             "医疗功效表述不进入商品详情，需合规审核后才可使用。",
                             [row[0] for row in medical], [row[1] for row in medical]))
    garbled = _matching(items, GARBLED_RE)
    if garbled:
        issues.append(_issue("GARBLED_SOURCE_TEXT", "content_sections.facilities",
                             "设施原文存在疑似错字，已从展示内容中排除。",
                             [row[0] for row in garbled], [row[1] for row in garbled]))
    return issues

def _content_sections(items: list[dict[str, Any]]) -> list[dict[str, Any]]:
    grouped: dict[str, list[dict[str, Any]]] = {key: [] for key in SECTION_NAMES}
    seen: dict[str, set[str]] = {key: set() for key in SECTION_NAMES}
    source_counts: dict[tuple[str, int], int] = {}
    for index, item in enumerate(items):
        if item.get("kind") not in {"heading", "paragraph", "bullet"}:
            continue
        text = str(item.get("text") or "")
        if MEDICAL_RE.search(text):
            continue
        item_match = next((key for key, pattern in SECTION_RULES if pattern.search(text)), None)
        for clause in _clauses(text):
            if re.fullmatch(r".{0,6}(?:规则|备注)[:：]?", clause):
                continue
            if (CONTACT_RE.search(clause) or PRICE_RE.search(clause) or MEDICAL_RE.search(clause)
                    or RATING_RE.search(clause) or CONFLICT_RE.search(clause)
                    or BED_ANOMALY_RE.search(clause) or GARBLED_RE.search(clause)
                    or MARKETING_RE.search(clause)):
                continue
            match = next((key for key, pattern in SECTION_RULES if pattern.search(clause)), item_match)
            if item_match == "attractions":
                if not ATTRACTION_RE.search(clause):
                    continue
                match = item_match
            if not match or clause in seen[match] or (match == "overview" and len(clause) < 12):
                continue
            limit = 1 if match == "attractions" else 2
            if source_counts.get((match, index), 0) >= limit:
                continue
            seen[match].add(clause)
            source_counts[(match, index)] = source_counts.get((match, index), 0) + 1
            grouped[match].append({"text": clause[:240], "source_refs": [index]})
    return [
        {"section_id": key, "section_name": name, "facts": grouped[key][:8]}
        for key, name in SECTION_NAMES.items()
    ]

def _room_packages(quotes: list[Quote]) -> tuple[list[dict[str, Any]], Decimal]:
    grouped: dict[tuple[str, str | None], list[Quote]] = {}
    valid = [row for row in quotes if row.unit == "人" and row.price >= Decimal("300")]
    for quote in valid:
        grouped.setdefault((quote.room_type, quote.occupancy), []).append(quote)
    packages = []
    for (room_type, occupancy), rows in grouped.items():
        offers = []
        for quote in sorted(rows, key=lambda row: (row.days, row.price)):
            offers.append({
                "duration_label": quote.duration,
                "days": quote.days,
                "nights": quote.nights if "晚" in quote.duration else None,
                "price": f"{quote.price:.2f}",
                "unit": quote.unit,
                "source_refs": list(quote.source_refs),
                "source_type": quote.source,
            })
        packages.append({"room_type": room_type, "occupancy": occupancy, "offers": offers})
    packages.sort(key=lambda row: min(Decimal(offer["price"]) for offer in row["offers"]))
    return packages, min(row.price for row in valid)

def _identity(title: str) -> tuple[str, str | None]:
    match = re.fullmatch(r"(.+?)[（(]([^）)]+)[）)]", title)
    return (match.group(1), match.group(2)) if match else (title, None)

def _summary(items: list[dict[str, Any]], title: str) -> tuple[str, list[int]]:
    pattern = re.compile(r"基地(?:位于|坐落)|酒店(?:位于|坐落)|毗邻")
    for index, item in enumerate(items):
        clauses = [row for row in _clauses(str(item.get("text") or "")) if pattern.search(row)]
        if clauses:
            return clauses[0][:300], [index]
    return f"{title}旅居基地。", []

def build_product(document: dict[str, Any], items: list[dict[str, Any]],
                  generated_at: str | None = None) -> dict[str, Any]:
    quotes = extract_quotes(items)
    packages, starting_price = _room_packages(quotes)
    sections = _content_sections(items)
    issues = _quality_issues(items, quotes)
    base_name, alias = _identity(document["title"])
    all_text = " ".join(str(row.get("text") or "") for row in items)
    tag_specs = (("温泉", "温泉"), ("三餐", "含三餐"), ("湖泉", "湖泉商圈"),
                 ("健身房", "健身房"), ("古城", "古城周边"))
    tags = [{"label": label, "source_refs": [index for index, row in enumerate(items)
             if keyword in str(row.get("text") or "")]} for keyword, label in tag_specs
            if keyword in all_text]
    summary, summary_refs = _summary(items, document["title"])
    content_complete = all(section["facts"] for section in sections)
    traceable = bool(summary_refs) and all(offer["source_refs"] for package in packages
                                           for offer in package["offers"]) and all(tag["source_refs"] for tag in tags)
    product = {
        "schema_version": SCHEMA_VERSION,
        "generated_at": generated_at or datetime.now(timezone.utc).isoformat(),
        "source": {
            "document_id": document["slug"], "title": document["title"],
            "url": document["source_url"], "updated_at": document["source_updated_at"],
            "docx_sha256": document.get("docx_sha256"),
            "reference_scheme": "content_items zero-based index",
        },
        "identity": {
            "product_name": document["title"], "base_name": base_name,
            "alias": alias, "city": document["city"], "category": "旅居基地",
        },
        "display": {"title": document["title"], "summary": summary,
                    "summary_source_refs": summary_refs, "tags": tags[:5]},
        "pricing": {
            "currency": "CNY", "starting_price": f"{starting_price:.2f}",
            "starting_price_unit": "人", "room_package_count": len(packages),
            "offer_count": sum(len(row["offers"]) for row in packages),
            "room_packages": packages,
        },
        "content_sections": sections,
        "media": {
            "cover_asset_index": COVER_ASSET_INDEX.get(document["slug"]),
            "image_count": sum(row.get("status") == "downloaded" for row in document.get("assets", [])),
            "video_count": len(document.get("video_urls", [])),
            "images": [{"asset_index": row["index"], "width": row.get("width"),
                        "height": row.get("height"), "sha256": row.get("sha256")}
                       for row in document.get("assets", []) if row.get("status") == "downloaded"],
        },
        "quality": {
            "status": "review_required" if issues or not content_complete or not traceable else "ready",
            "issues": issues,
            "checks": {"source_traceability": traceable,
                       "content_sections_complete": content_complete,
                       "starting_price_matches_offers": True},
        },
    }
    validate_product(product)
    return product

def validate_product(product: dict[str, Any]) -> None:
    if product.get("schema_version") != SCHEMA_VERSION:
        raise ValueError("Unsupported travel product schema version")
    packages = product.get("pricing", {}).get("room_packages") or []
    offers = [offer for package in packages for offer in package.get("offers") or []]
    if not packages or not offers:
        raise ValueError("Travel product has no bookable room offers")
    pricing = product["pricing"]
    if pricing["room_package_count"] != len(packages) or pricing["offer_count"] != len(offers):
        raise ValueError("Declared pricing counts do not match room offers")
    prices = [Decimal(row["price"]) for row in offers]
    if Decimal(product["pricing"]["starting_price"]) != min(prices):
        raise ValueError("Starting price does not match the lowest room offer")
    keys = [(row["room_type"], row.get("occupancy"), offer["duration_label"])
            for row in packages for offer in row["offers"]]
    if len(keys) != len(set(keys)):
        raise ValueError("Duplicate room, occupancy, and duration offer")
    if not all(offer.get("source_refs") for offer in offers):
        raise ValueError("Every room offer must retain source references")
    expected_sections = list(SECTION_NAMES.items())
    actual_sections = [(row["section_id"], row["section_name"])
                       for row in product["content_sections"]]
    if actual_sections != expected_sections:
        raise ValueError("Travel product content sections are incomplete or out of order")
    content = json.dumps(product["content_sections"], ensure_ascii=False)
    if CONTACT_RE.search(content) or MEDICAL_RE.search(content):
        raise ValueError("Prohibited contact or medical claims leaked into display content")
    checks = product["quality"]["checks"]
    expected = "review_required" if (product["quality"]["issues"] or not checks["source_traceability"]
                                     or not checks["content_sections_complete"]) else "ready"
    if product["quality"]["status"] != expected:
        raise ValueError("Quality status does not match unresolved issues")

def extract_one(knowledge_root: Path, title: str) -> dict[str, Any]:
    output = knowledge_root / "output"
    state = json.loads((output / "migration-state.json").read_text(encoding="utf-8"))
    document = next((row for row in state["documents"] if row.get("title") == title), None)
    if document is None:
        raise ValueError(f"Knowledge document not found: {title}")
    cache = json.loads((output / "browser-cache" / f"{document['slug']}.json").read_text())
    items = _load_extractor(knowledge_root)(cache["body_html"])
    return build_product(document, items)
