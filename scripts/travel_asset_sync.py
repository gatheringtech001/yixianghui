#!/usr/bin/env python3
"""Resumable, checksum-verified travel asset upload helpers."""

from __future__ import annotations

import shlex
import shutil
import subprocess
from typing import Any

from import_travel_catalog import (
    LOCAL_UPLOAD_ROOT,
    SSH_HOST,
    PolicyError,
    RuntimeFailure,
    _asset_state,
    _remote_dir,
    _ssh_env,
    _ssh_script,
)


def _pending_assets(assets: list[dict[str, Any]], current: dict[str, str]) -> list[dict[str, Any]]:
    expected_names = {row["file"] for row in assets}
    unexpected = sorted(set(current) - expected_names)
    if unexpected:
        raise PolicyError(f"Target travel-v2 asset directory contains unexpected files: {unexpected[:5]}")
    return [row for row in assets if current.get(row["file"]) != row["sha256"]]


def sync_assets(target: str, catalog_meta: dict[str, Any],
                assets: list[dict[str, Any]], batch_size: int = 50) -> None:
    current = _asset_state(target, catalog_meta)
    pending = _pending_assets(assets, current)
    if not pending:
        return
    if target == "local":
        directory = LOCAL_UPLOAD_ROOT / catalog_meta["remote_asset_dir"].removeprefix("/profile/")
        directory.mkdir(parents=True, exist_ok=True)
        for asset in pending:
            shutil.copy2(asset["path"], directory / asset["file"])
        return
    directory = _remote_dir(catalog_meta)
    _ssh_script(f"install -d -m 0755 {shlex.quote(directory)}")
    for start in range(0, len(pending), batch_size):
        batch = pending[start:start + batch_size]
        command = [
            "scp", "-o", "ConnectTimeout=10", "-o", "NumberOfPasswordPrompts=1",
            "-o", "PreferredAuthentications=password", "-o", "StrictHostKeyChecking=yes",
            *(row["path"] for row in batch), f"{SSH_HOST}:{directory}/",
        ]
        result = subprocess.run(command, env=_ssh_env(), capture_output=True, check=False)
        if result.returncode:
            detail = result.stderr.decode(errors="replace").strip()
            raise RuntimeFailure(detail or f"Asset upload batch failed at offset {start}")
        uploaded = _asset_state(target, catalog_meta)
        corrupt = [row["file"] for row in batch if uploaded.get(row["file"]) != row["sha256"]]
        if corrupt:
            raise RuntimeFailure(f"Asset checksum failed after batch {start}: {corrupt[:5]}")
    _ssh_script(f"chmod 0644 {shlex.quote(directory)}/*.jpg")
