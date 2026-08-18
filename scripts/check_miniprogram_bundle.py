#!/usr/bin/env python3
"""Reject mini-program preview bundles that contain a local API endpoint."""

from __future__ import annotations

import argparse
from pathlib import Path


class BundlePolicyError(RuntimeError):
    pass


def verify_bundle(bundle_root: Path, required_url: str,
                  forbidden_urls: list[str]) -> dict:
    files = sorted(bundle_root.rglob("*.js"))
    if not files:
        raise BundlePolicyError(f"No JavaScript bundle found under {bundle_root}")
    required_found = False
    violations = []
    for path in files:
        content = path.read_text(encoding="utf-8", errors="ignore")
        required_found = required_found or required_url in content
        violations.extend(url for url in forbidden_urls if url in content)
    if violations:
        raise BundlePolicyError(
            "Preview bundle contains forbidden local API URL: "
            + ", ".join(sorted(set(violations))))
    if not required_found:
        raise BundlePolicyError(
            f"Preview bundle does not contain required production API: {required_url}")
    return {"bundle": str(bundle_root), "required_url": required_url,
            "checked_files": len(files)}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--bundle", type=Path, required=True)
    parser.add_argument("--required-url", required=True)
    parser.add_argument("--forbidden-url", action="append", default=[])
    args = parser.parse_args()
    try:
        result = verify_bundle(
            args.bundle, args.required_url, args.forbidden_url)
    except BundlePolicyError as error:
        print(f"BUNDLE_CHECK_FAILED: {error}")
        return 1
    print(
        f"BUNDLE_CHECK_OK: {result['required_url']} "
        f"({result['checked_files']} JavaScript files)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
