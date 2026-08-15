#!/usr/bin/env python3
"""Conservatively extract bookable lodging quotes from knowledge-base content."""

from __future__ import annotations

import re
from dataclasses import asdict, dataclass
from decimal import Decimal, ROUND_HALF_UP
from typing import Any


@dataclass(frozen=True)
class Quote:
    room: str
    duration: str
    days: int
    nights: int
    price: Decimal
    unit: str
    source: str
    room_type: str = ""
    occupancy: str | None = None
    source_refs: tuple[int, ...] = ()

    def to_dict(self) -> dict[str, Any]:
        value = asdict(self)
        value["price"] = str(self.price.quantize(Decimal("0.01")))
        value["source_refs"] = list(self.source_refs)
        value["average"] = str(
            (self.price / max(self.nights, 1)).quantize(Decimal("0.01"), ROUND_HALF_UP)
        )
        return value


_CN_DIGITS = {"零": 0, "一": 1, "二": 2, "两": 2, "三": 3, "四": 4,
              "五": 5, "六": 6, "七": 7, "八": 8, "九": 9}
_DURATION = r"(?:\d+|[零一二两三四五六七八九十]+)(?:天(?:\d+|[零一二两三四五六七八九十]+)晚|天|晚|个?月)"
_ROOM_HINT = re.compile(
    r"(?:普通|舒适|豪华|湖景|山景|江景|园景|阳台|飘窗|标间|双床|大床|套房|"
    r"单间|公寓|LOFT|一人|两人|三人|四人|单人|二居室|三居室|房型)", re.I
)
_OCCUPANCY_ONLY = re.compile(
    r"^(?:(?:一|1|单)人(?:一间|入住|包房)?|(?:二|2|两)人(?:一间|入住|拼房)?|"
    r"(?:三|3)人(?:一间|入住)?|(?:四|4)人(?:一间|入住)?|拼房)$"
)
_AMOUNT_FIRST = re.compile(
    rf"(?<!\d)(?P<amount>[1-9]\d{{2,5}}(?:\.\d{{1,2}})?)\s*元?\s*"
    rf"(?:[/／]\s*(?P<unit>人|间|套房))?\s*(?:[/／]\s*)?(?P<duration>{_DURATION})"
)
_DURATION_FIRST = re.compile(
    rf"(?P<duration>{_DURATION})\s*(?:[/／]\s*(?:人|间|套房))?\s*"
    rf"(?:[:：=，,]\s*)?(?P<amount>[1-9]\d{{2,5}}(?:\.\d{{1,2}})?)\s*元?"
    rf"(?:[/／]\s*(?P<unit>人|间|套房))?"
)
_DURATION_FIRST_LOOSE = re.compile(
    rf"(?P<duration>{_DURATION})[^\d]{{1,24}}?"
    rf"(?P<amount>[1-9]\d{{2,5}}(?:\.\d{{1,2}})?)\s*元?"
    rf"(?:[/／]\s*(?P<unit>人|间|套房))?"
)
_TABLE_PRICE = re.compile(
    r"(?<!\d)(?P<amount>[1-9]\d{2,5}(?:\.\d{1,2})?)\s*元?\s*"
    r"(?:[/／]\s*(?P<unit>人|间|套房))?\s*(?:（[^）]*）|\([^)]*\))?"
)


def _cn_number(value: str) -> int:
    if value.isdigit():
        return int(value)
    if "十" not in value:
        return _CN_DIGITS.get(value, 0)
    left, _, right = value.partition("十")
    tens = _CN_DIGITS.get(left, 1) if left else 1
    ones = _CN_DIGITS.get(right, 0) if right else 0
    return tens * 10 + ones


def duration_values(label: str) -> tuple[int, int]:
    numbers = re.findall(r"\d+|[零一二两三四五六七八九十]+", label)
    values = [_cn_number(item) for item in numbers]
    if "月" in label:
        days = max(values[0] * 30, 1)
        return days + 1, days
    if "天" in label and "晚" in label and len(values) >= 2:
        return values[0], values[1]
    if "晚" in label:
        return values[0] + 1, values[0]
    days = values[0]
    return days, max(days - 1, 1)


def _clean_room(value: str, fallback: str = "标准房型") -> str:
    value = re.sub(r"\s+", " ", value).strip(" ：:，,；;。-—")
    value = re.sub(r"^(?:\d{4}年)?(?:含三餐|只含早|不含餐|优惠|旅居)?价格(?:表)?", "", value)
    if len(value) > 80:
        value = value[-80:]
    return value or fallback


def _unit(explicit: str | None, room: str) -> str:
    if explicit:
        return "间" if explicit in {"间", "套房"} else "人"
    return "间" if re.search(r"(?:元/间|一间房|整间|包房)", room) else "人"


def _quote(room: str, duration: str, amount: str, unit: str | None,
           source: str, *, room_type: str | None = None,
           occupancy: str | None = None, source_refs: tuple[int, ...] = ()) -> Quote:
    days, nights = duration_values(duration)
    return Quote(_clean_room(room), duration, days, nights, Decimal(amount),
                 _unit(unit, room), source, _clean_room(room_type or room),
                 occupancy, source_refs)


def _occupancy(value: str) -> str | None:
    compact = re.sub(r"\s+", "", value).strip(" ：:，,；;。-—()（）")
    if not _OCCUPANCY_ONLY.fullmatch(compact):
        return None
    if compact == "拼房":
        return compact
    count = "1" if compact[0] in {"一", "1", "单"} else {
        "二": "2", "2": "2", "两": "2", "三": "3", "3": "3",
        "四": "4", "4": "4",
    }[compact[0]]
    suffix = "包房" if "包房" in compact else "拼房" if "拼房" in compact else "一间"
    return f"{count}人{suffix}"


def _room_label(room_type: str, occupancy: str | None) -> str:
    if not occupancy or room_type == "标准房型":
        return occupancy or room_type
    return f"{room_type}（{occupancy}）"


def parse_table_quotes(rows: list[list[str]], source_ref: int | None = None) -> list[Quote]:
    quotes: list[Quote] = []
    for index in range(len(rows) - 1):
        labels = [re.sub(r"\s+", "", str(cell)) for cell in rows[index]]
        prices = [str(cell).strip() for cell in rows[index + 1]]
        if len(labels) < 2 or len(prices) != len(labels) - 1:
            continue
        if not all(re.fullmatch(_DURATION, label) for label in labels[1:]):
            continue
        parsed = [_TABLE_PRICE.fullmatch(value) for value in prices]
        if not all(parsed):
            continue
        room = labels[0]
        for duration, match in zip(labels[1:], parsed):
            assert match is not None
            quotes.append(_quote(room, duration, match.group("amount"),
                                 match.group("unit"), "table", room_type=room,
                                 source_refs=(() if source_ref is None else (source_ref,))))
    return quotes


def _parse_text_quotes(text: str, current_room: str, room_source_ref: int | None,
                       item_ref: int | None) -> tuple[list[Quote], str, int | None]:
    quotes: list[Quote] = []
    normalized = re.sub(r"[\t\r\f\v ]+", " ", text)
    clauses = re.split(r"[；;\n]+", normalized)
    for clause in clauses:
        loose = list(_DURATION_FIRST_LOOSE.finditer(clause)) if re.search(
            r"(?:价格|套餐|旅居|房型)", clause
        ) else []
        matches = sorted(
            [*_AMOUNT_FIRST.finditer(clause), *_DURATION_FIRST.finditer(clause), *loose],
            key=lambda item: item.start(),
        )
        if not matches:
            if _ROOM_HINT.search(clause) and len(clause) <= 100:
                candidate = _clean_room(clause, current_room)
                if not _occupancy(candidate):
                    current_room = candidate
                    room_source_ref = item_ref
            continue
        prefix = clause[:matches[0].start()]
        occupancy = _occupancy(prefix)
        if _ROOM_HINT.search(prefix) and not occupancy:
            current_room = _clean_room(prefix, current_room)
            room_source_ref = item_ref
        room = _room_label(current_room, occupancy)
        refs = tuple(dict.fromkeys(
            ref for ref in (room_source_ref, item_ref) if ref is not None
        ))
        seen_spans: list[tuple[int, int]] = []
        for match in matches:
            if any(match.start() < end and match.end() > start for start, end in seen_spans):
                continue
            seen_spans.append(match.span())
            quotes.append(_quote(room, match.group("duration"), match.group("amount"),
                                 match.group("unit"), "text", room_type=current_room,
                                 occupancy=occupancy, source_refs=refs))
    return quotes, current_room, room_source_ref


def parse_text_quotes(text: str) -> list[Quote]:
    if not re.search(r"(?:天|晚|月)", text):
        return []
    quotes, _, _ = _parse_text_quotes(text, "标准房型", None, None)
    return quotes


def extract_quotes(items: list[dict[str, Any]]) -> list[Quote]:
    quotes: list[Quote] = []
    current_room = "标准房型"
    room_source_ref: int | None = None
    for index, item in enumerate(items):
        if item.get("kind") == "table":
            quotes.extend(parse_table_quotes(item.get("rows") or [], index))
            continue
        text = str(item.get("text") or "")
        if re.search(r"(?:天|晚|月)", text) or _ROOM_HINT.search(text):
            parsed, current_room, room_source_ref = _parse_text_quotes(
                text, current_room, room_source_ref, index
            )
            quotes.extend(parsed)
    unique: dict[tuple[str, str, Decimal, str], Quote] = {}
    for quote in quotes:
        if quote.days > 366 or quote.nights > 365 or quote.price > Decimal("50000"):
            continue
        if quote.price / max(quote.nights, 1) < Decimal("10"):
            continue
        key = (quote.room, quote.duration, quote.price, quote.unit)
        unique.setdefault(key, quote)
    return list(unique.values())
