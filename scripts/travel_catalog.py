#!/usr/bin/env python3
"""Build a safe, deterministic travel catalog from the exported knowledge base."""

from __future__ import annotations

import hashlib
import html as html_lib
import importlib.util
import json
import re
import sys
from decimal import Decimal
from pathlib import Path
from typing import Any

from travel_asset_policy import COVER_ASSET_INDEX, REJECTED_ASSET_SHA256
from travel_price_parser import extract_quotes
from travel_quote_overrides import quote_overrides

TARGET_CITIES = ("昆明", "建水", "弥勒", "普洱", "西双版纳", "芒市", "腾冲")
DISPLAY_CITY = {"西双版纳": "西双版纳"}
EXISTING_ALIASES = {"昆明六号温泉基地": 31, "昆明七号古滇基地": 32}
CONTACT_RE = re.compile(
    r"(?<!\d)1[3-9]\d{9}(?!\d)|(?:二维码|扫码|联系(?:电话|手机|方式|号码)|手机号码|手机号|"
    r"微信号|加微信|V信|vx\s*[:：])", re.I
)
PRICE_RE = re.compile(r"(?:价格|套餐|\d+\s*(?:元|/人|／人|/间|／间))")
POLICY_RE = re.compile(
    r"退改|退订|退款|取消|房损|定金|押金|提前退房|入住前|入住后|办理入住|"
    r"退房时间|不予退|不做任何退费|费用包含|服务包含|价格(?:表|详情)?|收费标准|"
    r"套餐提示|预订须知|重要须知|注意事项|儿童(?:政策|收费|餐费)|"
    r"(?:节假日|春节|暑期|国庆|五一).{0,16}(?:加价|涨价|政策)"
)
MONEY_RE = re.compile(r"\d+(?:\.\d+)?\s*元(?:\b|/|／|每)")
REMOTE_ASSET_DIR = "/profile/upload/2026/08/13/travel-catalog-v2"
MAX_SECTION_BYTES = 54_000


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        while chunk := source.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def _load_extractor(knowledge_root: Path):
    module_path = knowledge_root / "yixiangkb_migration.py"
    spec = importlib.util.spec_from_file_location("yixiangkb_migration", module_path)
    if not spec or not spec.loader:
        raise RuntimeError(f"Cannot load knowledge extractor: {module_path}")
    module = importlib.util.module_from_spec(spec)
    sys.path.insert(0, str(knowledge_root))
    try:
        spec.loader.exec_module(module)
    finally:
        sys.path.pop(0)
    return module.extract_content_items


def _remote_name(slug: str, asset: dict[str, Any]) -> str:
    return f"{slug}-{int(asset['index']):03d}.jpg"


def _remote_path(slug: str, asset: dict[str, Any]) -> str:
    return f"{REMOTE_ASSET_DIR}/{_remote_name(slug, asset)}"


def _asset_map(document: dict[str, Any]) -> dict[str, dict[str, Any]]:
    return {
        row["url"]: row for row in document["assets"]
        if row.get("status") == "downloaded" and row.get("sha256") not in REJECTED_ASSET_SHA256
    }


def _text_fragment(item: dict[str, Any]) -> str:
    text = html_lib.escape(str(item.get("text") or "")).replace("\n", "<br>")
    if item["kind"] == "heading":
        level = min(max(int(item.get("level") or 2), 2), 4)
        return f"<h{level}>{text}</h{level}>"
    if item["kind"] == "bullet":
        return f"<p>• {text}</p>"
    return f"<p>{text}</p>"


def _table_fragment(rows: list[list[str]]) -> str:
    body = []
    for row in rows:
        cells = "".join(
            f"<td style=\"border:1px solid #ddd;padding:6px;\">{html_lib.escape(str(cell))}</td>"
            for cell in row
        )
        body.append(f"<tr>{cells}</tr>")
    return "<table style=\"width:100%;border-collapse:collapse;\"><tbody>" + "".join(body) + "</tbody></table>"


def _item_text(item: dict[str, Any]) -> str:
    if item.get("kind") == "table":
        return " ".join(str(cell) for row in item.get("rows") or [] for cell in row)
    return str(item.get("text") or item.get("alt") or "")


def _policy_flag(item: dict[str, Any]) -> bool | None:
    text = re.sub(r"\s+", " ", _item_text(item)).strip()
    if item.get("kind") == "image" and not text:
        return None
    return bool(POLICY_RE.search(text) or MONEY_RE.search(text))


def _content_fragments(items: list[dict[str, Any]], document: dict[str, Any]) \
        -> tuple[list[dict[str, Any]], set[int]]:
    assets = _asset_map(document)
    fragments: list[dict[str, Any]] = []
    used_indices: set[int] = set()
    seen_images: set[int] = set()
    suppress_images = 0
    for item in items:
        kind = item.get("kind")
        text = str(item.get("text") or "")
        if text and CONTACT_RE.search(text):
            if fragments and fragments[-1]["kind"] == "image":
                removed = fragments.pop()["index"]
                if removed is not None:
                    used_indices.discard(removed)
            suppress_images = 2
            continue
        if kind == "image":
            asset = assets.get(item.get("src"))
            if not asset or suppress_images:
                suppress_images = max(suppress_images - 1, 0)
                continue
            index = int(asset["index"])
            if index in seen_images:
                continue
            seen_images.add(index)
            used_indices.add(index)
            src = _remote_path(document["slug"], asset)
            alt = html_lib.escape(str(item.get("alt") or document["title"]), quote=True)
            fragments.append({
                "html": f'<p><img src="{src}" alt="{alt}" '
                        'style="width:100%;height:auto;display:block;"></p>',
                "kind": "image", "index": index, "policy": _policy_flag(item),
            })
        elif kind == "table":
            fragments.append({
                "html": _table_fragment(item.get("rows") or []),
                "kind": "table", "index": None, "policy": _policy_flag(item),
            })
        elif kind in {"heading", "paragraph", "bullet"} and text.strip():
            fragments.append({
                "html": _text_fragment(item), "kind": "text", "index": None,
                "policy": _policy_flag(item),
            })
        elif kind == "video" and str(item.get("src") or "").startswith("https://"):
            url = html_lib.escape(item["src"], quote=True)
            fragments.append({
                "html": f'<p><a href="{url}">查看知识库视频</a></p>',
                "kind": "video", "index": None, "policy": False,
            })
    for position, fragment in enumerate(fragments):
        if fragment["kind"] != "image" or fragment["policy"] is not None:
            continue
        previous = next((row["policy"] for row in reversed(fragments[:position])
                         if row["kind"] != "image"), None)
        following = next((row["policy"] for row in fragments[position + 1:]
                          if row["kind"] != "image"), False)
        fragment["policy"] = previous if previous is not None else following
    return fragments, used_indices


def build_content(items: list[dict[str, Any]], document: dict[str, Any]) -> tuple[list[str], set[int]]:
    fragments, used_indices = _content_fragments(items, document)
    sections: list[str] = []
    current: list[str] = []
    current_bytes = 0
    for row in fragments:
        fragment = row["html"]
        size = len(fragment.encode("utf-8"))
        if current and current_bytes + size > MAX_SECTION_BYTES:
            sections.append("".join(current))
            current, current_bytes = [], 0
        current.append(fragment)
        current_bytes += size
    if current:
        sections.append("".join(current))
    return sections, used_indices


def build_tabs(items: list[dict[str, Any]], document: dict[str, Any]) \
        -> tuple[list[dict[str, Any]], set[int]]:
    fragments, used_indices = _content_fragments(items, document)
    grouped = {
        "basic": [row["html"] for row in fragments if not row["policy"]],
        "policy": [row["html"] for row in fragments if row["policy"]],
    }
    if not grouped["basic"] or not grouped["policy"]:
        missing = "基本特色" if not grouped["basic"] else "政策"
        raise ValueError(f"无法从知识库识别{missing}: {document['title']}")
    tabs = []
    for section_id, section_name, sort_order in (
        ("basic", "基本特色", 1), ("policy", "政策", 2),
    ):
        content = "".join(grouped[section_id])
        if len(content.encode("utf-8")) > MAX_SECTION_BYTES:
            raise ValueError(f"详情标签内容超过数据库限制: {document['title']} {section_name}")
        tabs.append({
            "section_id": section_id, "section_name": section_name,
            "content": content, "sort_order": sort_order, "min_content_length": 250,
        })
    return tabs, used_indices


def _description(items: list[dict[str, Any]], title: str) -> str:
    values: list[str] = []
    for item in items:
        text = re.sub(r"\s+", " ", str(item.get("text") or "")).strip()
        if item.get("kind") != "paragraph" or len(text) < 24:
            continue
        if CONTACT_RE.search(text) or PRICE_RE.search(text) or text == title:
            continue
        values.append(text)
        if len(" ".join(values)) >= 180:
            break
    return " ".join(values)[:500] or f"{title}旅居基地资料与服务介绍。"


def _tags(title: str, items: list[dict[str, Any]]) -> str:
    text = title + " " + " ".join(str(row.get("text") or "") for row in items[:40])
    candidates = (
        ("温泉", "温泉"), ("含三餐", "含餐"), ("只含早", "含早"),
        ("不含餐", "不含餐"), ("四钻", "四钻"), ("五钻", "五钻"),
        ("市区", "市区"), ("古城", "古城"), ("康养", "康养"),
    )
    values = [tag for keyword, tag in candidates if keyword in text]
    return "|".join((values or ["旅居", "适老"])[0:5])


def _gallery(document: dict[str, Any], excluded: set[int]) -> list[int]:
    cover_index = COVER_ASSET_INDEX.get(document["slug"])
    if cover_index is None:
        raise ValueError(f"缺少人工封面决策: {document['title']}")
    available = [
        row for row in document["assets"]
        if row.get("status") == "downloaded"
        and row.get("sha256") not in REJECTED_ASSET_SHA256
    ]
    cover = next((row for row in available if int(row["index"]) == cover_index), None)
    if cover is None:
        raise ValueError(f"人工封面不可用: {document['title']} #{cover_index}")
    assets = [row for row in available if int(row["index"]) not in excluded]
    landscapes = [
        row for row in assets[:16]
        if int(row.get("width") or 0) >= int(row.get("height") or 1) * 1.1
        and int(row.get("width") or 0) >= 800
    ]
    selected = [cover] + landscapes + assets
    return list(dict.fromkeys(int(row["index"]) for row in selected))[:6]


def _bookable_quotes(quotes: list[dict[str, Any]]) -> list[dict[str, Any]]:
    groups: dict[tuple[str, str], list[dict[str, Any]]] = {}
    for row in quotes:
        if row["unit"] != "人" or Decimal(row["price"]) < 300:
            continue
        if len(row["room"]) > 60 or re.fullmatch(r"\d+月", row["duration"]):
            continue
        groups.setdefault((row["room"], row["duration"]), []).append(row)
    result = []
    for rows in groups.values():
        prices = {row["price"] for row in rows}
        if len(prices) == 1:
            result.append(rows[0])
    return result[:40]


def _product(document: dict[str, Any], cache: dict[str, Any], extractor) -> dict[str, Any]:
    items = extractor(cache["body_html"])
    tabs, used_indices = build_tabs(items, document)
    all_indices = {int(row["index"]) for row in document["assets"]}
    gallery = _gallery(document, all_indices - used_indices)
    used_indices.update(gallery)
    assets_by_index = {int(row["index"]): row for row in document["assets"]}
    quotes = [row.to_dict() for row in extract_quotes(items)]
    verified = quote_overrides(document["title"])
    if verified:
        quotes = verified
    bookable = _bookable_quotes(quotes)
    return {
        "slug": document["slug"], "city": document["city"], "name": document["title"],
        "existing_goods_id": EXISTING_ALIASES.get(document["title"]),
        "source_url": document["source_url"], "source_updated_at": document["source_updated_at"],
        "description": _description(items, document["title"]),
        "tags": _tags(document["title"], items), "tabs": tabs,
        "gallery": [_remote_path(document["slug"], assets_by_index[index]) for index in gallery],
        "quotes": quotes, "bookable_quotes": bookable,
        "assets": [
            {**row, "remote_name": _remote_name(document["slug"], row),
             "remote_path": _remote_path(document["slug"], row)}
            for row in document["assets"] if int(row["index"]) in used_indices
        ],
    }


def load_catalog(knowledge_root: Path) -> dict[str, Any]:
    output = knowledge_root / "output"
    state_path = output / "migration-state.json"
    state = json.loads(state_path.read_text(encoding="utf-8"))
    extractor = _load_extractor(knowledge_root)
    documents = [
        row for row in state["documents"]
        if row.get("category") == "基地资料" and row.get("city") in TARGET_CITIES
    ]
    document_slugs = {row["slug"] for row in documents}
    if document_slugs != set(COVER_ASSET_INDEX):
        missing = sorted(document_slugs - set(COVER_ASSET_INDEX))
        obsolete = sorted(set(COVER_ASSET_INDEX) - document_slugs)
        raise ValueError(f"人工封面清单与知识库不一致: missing={missing}, obsolete={obsolete}")
    products = []
    for document in sorted(documents, key=lambda row: (TARGET_CITIES.index(row["city"]), row["title"])):
        if not document.get("image_complete"):
            raise ValueError(f"Incomplete knowledge images: {document['title']}")
        cache_path = output / "browser-cache" / f"{document['slug']}.json"
        cache = json.loads(cache_path.read_text(encoding="utf-8"))
        products.append(_product(document, cache, extractor))
    if len(products) != 56 or len({row["slug"] for row in products}) != 56:
        raise ValueError(f"Expected 56 unique products, got {len(products)}")
    source_hashes = {str(state_path): sha256_file(state_path)}
    for row in products:
        cache = output / "browser-cache" / f"{row['slug']}.json"
        source_hashes[str(cache)] = sha256_file(cache)
    return {
        "knowledge_root": str(knowledge_root.resolve()), "remote_asset_dir": REMOTE_ASSET_DIR,
        "source_hashes": source_hashes, "cities": list(TARGET_CITIES), "products": products,
    }
