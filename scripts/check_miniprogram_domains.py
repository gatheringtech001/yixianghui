#!/usr/bin/env python3
"""Fail preview delivery when WeChat server-domain policy is incomplete."""

from __future__ import annotations

import argparse
import json
from pathlib import Path


DOMAIN_TYPES = ("RequestDomain", "UploadDomain", "DownloadDomain")


class DomainPolicyError(RuntimeError):
    pass


def _matching_caches(root: Path, appid: str) -> list[tuple[float, Path, dict]]:
    matches = []
    for path in root.glob("*/WeappLocalData/localstorage_*.json"):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, UnicodeDecodeError, json.JSONDecodeError):
            continue
        if not isinstance(data, dict):
            continue
        cached_appid = data.get("appid") or data.get("attr", {}).get("appid")
        if cached_appid == appid:
            matches.append((path.stat().st_mtime, path, data))
    return sorted(matches, reverse=True)


def verify_domains(root: Path, appid: str, required_domain: str) -> dict:
    caches = _matching_caches(root, appid)
    if not caches:
        raise DomainPolicyError(f"No WeChat domain cache found for AppID {appid}")
    _, path, data = caches[0]
    network = data.get("runtimeAttr", {}).get("network")
    if not network:
        network = data.get("attr", {}).get("network", {})
    missing = [name for name in DOMAIN_TYPES
               if required_domain not in network.get(name, [])]
    if missing:
        names = ", ".join(missing)
        raise DomainPolicyError(
            f"WeChat domain policy missing {names}: {required_domain}. "
            "Update 微信公众平台 -> 开发管理 -> 开发设置 -> 服务器域名."
        )
    return {"appid": appid, "domain": required_domain,
            "checked": list(DOMAIN_TYPES), "cache": str(path)}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--appid", required=True)
    parser.add_argument("--required-domain", required=True)
    parser.add_argument(
        "--app-support-root",
        type=Path,
        default=Path.home() / "Library/Application Support/微信开发者工具",
    )
    args = parser.parse_args()
    try:
        result = verify_domains(
            args.app_support_root, args.appid, args.required_domain.rstrip("/"))
    except DomainPolicyError as error:
        print(f"DOMAIN_CHECK_FAILED: {error}")
        return 1
    print(json.dumps(result, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
