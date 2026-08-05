#!/usr/bin/env python3
"""SSH_ASKPASS helper. Never invoke directly or log its stdout."""

from __future__ import annotations

import os
import re
import sys
from pathlib import Path

DEFAULT_NOTE = Path(
    "/Users/kevin/Documents/Obsidian Vault/Bitwarden/Items/Logins/"
    "SSH yxh (逸享荟 - 阿里云 ECS).md"
)


def main() -> int:
    if os.environ.get("YXH_ALLOW_ASKPASS") != "1":
        print("Refusing direct credential access", file=sys.stderr)
        return 2

    note = Path(os.environ.get("YXH_SSH_CREDENTIAL_NOTE", DEFAULT_NOTE))
    try:
        content = note.read_text(encoding="utf-8")
    except OSError as exc:
        print(f"Unable to read SSH credential note: {exc}", file=sys.stderr)
        return 2

    match = re.search(r"^- Password:\s*`([^`]+)`\s*$", content, re.MULTILINE)
    if not match:
        print("SSH password field is missing", file=sys.stderr)
        return 2

    sys.stdout.write(match.group(1))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
