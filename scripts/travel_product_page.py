#!/usr/bin/env python3
"""Project canonical travel-product data onto the mini-program view model."""

from __future__ import annotations

import html
from decimal import Decimal, ROUND_HALF_UP
from typing import Any

from travel_asset_policy import REJECTED_ASSET_SHA256
from travel_catalog import REMOTE_ASSET_DIR


def _banner_images(document: dict[str, Any], cover_index: int | None) -> list[str]:
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
    return [f"{REMOTE_ASSET_DIR}/{document['slug']}-{index:03d}.jpg" for index in indices]


def _average(price: str, nights: int | None) -> str | None:
    if nights is None:
        return None
    value = Decimal(price) / Decimal(nights)
    return f"{value.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP):.2f}"


def _sku_groups(packages: list[dict[str, Any]], cover: str,
                meal_name: str) -> list[dict[str, Any]]:
    groups = []
    for package in packages:
        occupancy = package.get("occupancy")
        title = package["room_type"] + (f"（{occupancy}）" if occupancy else "")
        options = []
        for offer in package["offers"]:
            options.append({
                "name": offer["duration_label"],
                "day": offer["nights"],
                "combinationList": [{
                    "name": meal_name,
                    "price": offer["price"],
                    "average": _average(offer["price"], offer["nights"]),
                }],
            })
        groups.append({
            "title": title,
            "cover": cover,
            "descOne": "",
            "descTwo": "",
            "price": None,
            "skuDataList": options,
        })
    return groups


def _related_sections(sections: list[dict[str, Any]]) -> list[dict[str, str]]:
    return [{
        "id": f"id_{section['section_id']}",
        "name": section["section_name"],
        "content": "".join(f"<p>{html.escape(fact['text'])}</p>"
                           for fact in section["facts"]),
    } for section in sections]


def build_page_display(document: dict[str, Any], product: dict[str, Any]) -> dict[str, Any]:
    display = product["display"]
    banners = _banner_images(document, product["media"]["cover_asset_index"])
    tag_labels = [row["label"] for row in display["tags"]]
    meal_name = "含三餐" if "含三餐" in tag_labels else "住宿套餐"
    return {
        "bannerImages": banners,
        "hotelData": {
            "type": "旅居基地",
            "name": display["title"],
            "desc": display["summary"],
            "tagList": tag_labels,
            "related": _related_sections(product["content_sections"]),
        },
        "skuGroupList": _sku_groups(
            product["pricing"]["room_packages"], banners[0] if banners else "", meal_name
        ),
    }


def validate_page_display(product: dict[str, Any]) -> None:
    page = product.get("page_display") or {}
    hotel = page.get("hotelData") or {}
    if hotel.get("type") != product["identity"]["category"]:
        raise ValueError("Page product type does not match canonical identity")
    if hotel.get("name") != product["display"]["title"]:
        raise ValueError("Page product name does not match canonical display title")
    if hotel.get("desc") != product["display"]["summary"]:
        raise ValueError("Page description does not match canonical display summary")
    expected_tags = [row["label"] for row in product["display"]["tags"]]
    if hotel.get("tagList") != expected_tags:
        raise ValueError("Page tags do not match canonical display tags")
    related = hotel.get("related") or []
    expected_sections = product["content_sections"]
    if [row.get("name") for row in related] != [row["section_name"] for row in expected_sections]:
        raise ValueError("Page detail sections do not match canonical content sections")
    groups = page.get("skuGroupList") or []
    packages = product["pricing"]["room_packages"]
    if len(groups) != len(packages):
        raise ValueError("Page SKU groups do not match canonical room packages")
    for group, package in zip(groups, packages):
        occupancy = package.get("occupancy")
        expected_title = package["room_type"] + (f"（{occupancy}）" if occupancy else "")
        if group.get("title") != expected_title:
            raise ValueError("Page SKU title does not match canonical room package")
        options = group.get("skuDataList") or []
        offers = package["offers"]
        if len(options) != len(offers):
            raise ValueError("Page duration options do not match canonical room offers")
        for option, offer in zip(options, offers):
            if option.get("name") != offer["duration_label"] or option.get("day") != offer["nights"]:
                raise ValueError("Page duration does not match canonical room offer")
            combination = (option.get("combinationList") or [{}])[0]
            if combination.get("average") != _average(offer["price"], offer["nights"]):
                raise ValueError("Page average does not match canonical room offer")
    page_prices = [combination["price"] for group in groups
                   for option in group["skuDataList"]
                   for combination in option["combinationList"]]
    canonical_prices = [offer["price"] for package in product["pricing"]["room_packages"]
                        for offer in package["offers"]]
    if page_prices != canonical_prices:
        raise ValueError("Page prices do not match canonical room offers")
