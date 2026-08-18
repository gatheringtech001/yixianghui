#!/usr/bin/env python3
"""Project canonical travel data onto the six product-detail content slots."""

from __future__ import annotations

import html
import re
from decimal import Decimal, ROUND_HALF_UP
from typing import Any

from travel_asset_policy import REJECTED_ASSET_SHA256
from travel_catalog import REMOTE_ASSET_DIR

PAGE_FIELDS = (
    "introduction", "mainImages", "roomImages", "roomPricePackages",
    "details", "checkInNotice",
)
ROOM_ASSET_DIR = "/profile/upload/2026/08/17/travel-room-v1"
ROOM_IMAGE_OVERRIDES = {
    ("ptzlh413322upmyo", "豪华标间", "2人一间"): (
        f"{ROOM_ASSET_DIR}/mile-deluxe-twin-real.jpg", "real", None,
    ),
}
ROOM_PLACEHOLDERS = {
    "king": f"{ROOM_ASSET_DIR}/placeholder-king-clean-v2.jpg",
    "twin": f"{ROOM_ASSET_DIR}/placeholder-twin-clean-v2.jpg",
}


def _remote_path(document: dict[str, Any], index: int) -> str:
    return f"{REMOTE_ASSET_DIR}/{document['slug']}-{index:03d}.jpg"


def _main_images(document: dict[str, Any], cover_index: int | None) -> list[str]:
    assets = [
        row for row in document.get("assets", [])
        if row.get("status") == "downloaded"
        and row.get("sha256") not in REJECTED_ASSET_SHA256
    ]
    cover = next((row for row in assets if int(row["index"]) == cover_index), None)
    landscapes = [
        row for row in assets[:16]
        if int(row.get("width") or 0) >= int(row.get("height") or 1) * 1.1
        and int(row.get("width") or 0) >= 800
    ]
    ordered = ([cover] if cover else []) + landscapes + assets
    indices = list(dict.fromkeys(int(row["index"]) for row in ordered))[:6]
    return [_remote_path(document, index) for index in indices]


def _room_terms(room_type: str) -> list[str]:
    terms = [part.strip() for part in re.split(r"[/／]", room_type) if part.strip()]
    variants = []
    for term in terms:
        variants.extend((term, term.replace("标间", "双床房")))
    return list(dict.fromkeys(value for value in variants if len(value) >= 3))


def _room_image(document: dict[str, Any], items: list[dict[str, Any]],
                room_type: str) -> str | None:
    assets = {row.get("url"): row for row in document.get("assets", [])
              if row.get("status") == "downloaded"}
    terms = _room_terms(room_type)
    for index, item in enumerate(items[:-1]):
        text = str(item.get("text") or "")
        if item.get("kind") != "paragraph" or not any(term in text for term in terms):
            continue
        media = items[index + 1]
        asset = assets.get(media.get("src")) if media.get("kind") == "image" else None
        if asset and asset.get("sha256") not in REJECTED_ASSET_SHA256:
            return _remote_path(document, int(asset["index"]))
    return None


def _placeholder_type(room_type: str) -> str:
    if "大床" in room_type and not re.search(r"标间|双床", room_type):
        return "king"
    return "twin"


def _room_media(document: dict[str, Any], items: list[dict[str, Any]],
                room_type: str, occupancy: str | None) -> dict[str, str | None]:
    override = ROOM_IMAGE_OVERRIDES.get((document["slug"], room_type, occupancy))
    if override:
        image, source_type, placeholder_type = override
        return {"image": image, "sourceType": source_type,
                "placeholderType": placeholder_type}
    image = _room_image(document, items, room_type)
    if image:
        return {"image": image, "sourceType": "real", "placeholderType": None}
    placeholder_type = _placeholder_type(room_type)
    return {"image": ROOM_PLACEHOLDERS[placeholder_type], "sourceType": "placeholder",
            "placeholderType": placeholder_type}


def _average(price: str, nights: int | None) -> str | None:
    if nights is None:
        return None
    value = Decimal(price) / Decimal(nights)
    return f"{value.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP):.2f}"


def _room_rows(document: dict[str, Any], product: dict[str, Any],
               items: list[dict[str, Any]]) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    image_rows, price_rows = [], []
    meal_plan = "含三餐" if any(row["label"] == "含三餐"
                              for row in product["display"]["tags"]) else "住宿套餐"
    for room in product["pricing"]["room_packages"]:
        identity = {"roomType": room["room_type"], "occupancy": room.get("occupancy")}
        image_rows.append({**identity, **_room_media(
            document, items, room["room_type"], room.get("occupancy")
        )})
        price_rows.append({**identity, "packages": [{
            "duration": offer["duration_label"], "days": offer["days"],
            "nights": offer["nights"], "mealPlan": meal_plan,
            "price": offer["price"], "priceUnit": offer["unit"],
            "averagePerNight": _average(offer["price"], offer["nights"]),
        } for offer in room["offers"]]})
    return image_rows, price_rows


def _section(section: dict[str, Any]) -> dict[str, str]:
    content = "".join(f"<p>{html.escape(fact['text'])}</p>" for fact in section["facts"])
    for media in section.get("media", []):
        caption = html.escape(media["caption"])
        image = html.escape(media["image"], quote=True)
        content += (
            f"<p><strong>{caption}</strong></p>"
            f'<p><img src="{image}" alt="{html.escape(media["caption"], quote=True)}" '
            'style="width:100%;height:auto;display:block;"></p>'
        )
    return {"title": section["section_name"], "content": content}


def build_page_display(document: dict[str, Any], product: dict[str, Any],
                       items: list[dict[str, Any]]) -> dict[str, Any]:
    room_images, room_prices = _room_rows(document, product, items)
    sections = product["content_sections"]
    return {
        "introduction": product["display"]["summary"],
        "mainImages": _main_images(document, product["media"]["cover_asset_index"]),
        "roomImages": room_images,
        "roomPricePackages": room_prices,
        "details": [_section(next(section for section in sections
                                  if section["section_id"] == "base_features"))],
        "checkInNotice": _section(next(section for section in sections
                                        if section["section_id"] == "stay_notice")),
    }


def apply_page_quality(product: dict[str, Any]) -> None:
    if any(not row.get("image") for row in product["page_display"]["roomImages"]):
        raise ValueError("Every room type must resolve to a real or placeholder image")


def _validate_room_rows(product: dict[str, Any]) -> None:
    page = product["page_display"]
    packages = product["pricing"]["room_packages"]
    expected_keys = [(row["room_type"], row.get("occupancy")) for row in packages]
    image_keys = [(row.get("roomType"), row.get("occupancy")) for row in page["roomImages"]]
    price_keys = [(row.get("roomType"), row.get("occupancy"))
                  for row in page["roomPricePackages"]]
    if image_keys != expected_keys or price_keys != expected_keys:
        raise ValueError("Page room rows do not match canonical room packages")
    for row in page["roomImages"]:
        if row.get("sourceType") not in {"real", "placeholder"}:
            raise ValueError("Room image source type is invalid")
        expected_placeholder = None if row["sourceType"] == "real" else _placeholder_type(row["roomType"])
        if row.get("placeholderType") != expected_placeholder:
            raise ValueError("Room image placeholder type does not match the room type")
    for page_room, room in zip(page["roomPricePackages"], packages):
        if len(page_room["packages"]) != len(room["offers"]):
            raise ValueError("Page price packages do not match canonical room offers")
        for page_offer, offer in zip(page_room["packages"], room["offers"]):
            expected = (offer["duration_label"], offer["days"], offer["nights"],
                        offer["price"], offer["unit"], _average(offer["price"], offer["nights"]))
            actual = (page_offer.get("duration"), page_offer.get("days"), page_offer.get("nights"),
                      page_offer.get("price"), page_offer.get("priceUnit"),
                      page_offer.get("averagePerNight"))
            if actual != expected:
                raise ValueError("Page price package does not match canonical room offer")


def validate_page_display(product: dict[str, Any]) -> None:
    page = product.get("page_display") or {}
    if list(page) != list(PAGE_FIELDS):
        raise ValueError("Page display fields do not match the six content slots")
    if page["introduction"] != product["display"]["summary"]:
        raise ValueError("Page introduction does not match canonical summary")
    if not page["mainImages"] or len(page["mainImages"]) > 6:
        raise ValueError("Page main images are missing or exceed the display limit")
    _validate_room_rows(product)
    feature = next(row for row in product["content_sections"]
                   if row["section_id"] == "base_features")
    expected_details = [_section(feature)]
    if page["details"] != expected_details:
        raise ValueError("Page details do not match canonical content sections")
    notice = next(row for row in product["content_sections"] if row["section_id"] == "stay_notice")
    if page["checkInNotice"] != _section(notice):
        raise ValueError("Page check-in notice does not match canonical content")
