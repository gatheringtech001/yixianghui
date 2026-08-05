---
name: backup-yixianghui-db
description: Create and verify local or production Yixianghui MySQL backups with consistent snapshots, compression, checksums, restrictive permissions, and restore planning. Use before production data changes, for scheduled/manual 逸享荟 backups, backup validation, schema snapshots, incident preparation, or recovery planning.
---

# Backup Yixianghui Database

Use `/Users/kevin/.codex/skills/manage-yixianghui/scripts/yxh_db.py`. Backups default to `~/.codex/yixianghui/backups` with mode `0600` and are never stored in Git.

## Create and verify

```bash
python3 /Users/kevin/.codex/skills/manage-yixianghui/scripts/yxh_db.py backup --env local
python3 /Users/kevin/.codex/skills/manage-yixianghui/scripts/yxh_db.py backup --env production --schema-only
python3 /Users/kevin/.codex/skills/manage-yixianghui/scripts/yxh_db.py verify-backup --file /absolute/backup.sql.gz
```

Use full backup before a broad production change. A single-row content plan's stored preimage is the required rollback evidence for that exact-row mutation; use a full database backup when multiple tables or many rows are involved.

Report the absolute path, compressed and decompressed byte sizes, SHA-256, target, and whether the dump was schema-only or full.

## Restore boundary

Do not restore automatically. Verify the archive, identify the target database, compare schema version, list objects that will be overwritten, estimate downtime, and produce exact restore and post-restore validation commands. A restore requires a new explicit confirmation after that plan is shown.
