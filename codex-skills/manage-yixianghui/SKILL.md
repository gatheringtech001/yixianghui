---
name: manage-yixianghui
description: Route and execute natural-language administration for the Yixianghui system, including database queries, content changes, backups, consistency audits, and production operations. Use when the user asks to manage 逸享荟/Yixianghui backend data, products, activities, education, ads, articles, orders, members, or production database state and the task must be classified and handled with safe environment and approval boundaries.
---

# Manage Yixianghui

Use the shared CLI in `scripts/yxh_db.py`. Read `references/safety.md` before every production task. Read `references/schema.md` when mapping natural language to tables, and `references/architecture.md` when code or API behavior matters.

## Workflow

1. State the target environment (`local` or `production`), business object, requested action, and whether the action is read-only, content mutation, backup, restore, or structural change.
2. Refuse to infer a production write from a broad request such as “接管后台”. Treat that as authority for read-only discovery, not approval for a specific mutation.
3. Route the task:
   - Query/report: use `$query-yixianghui-db`.
   - Product, activity, education, ad, article, or page content change: use `$edit-yixianghui-content`.
   - Backup or restore planning: use `$backup-yixianghui-db`.
   - Integrity or drift check: use `$audit-yixianghui-db`.
4. Validate on `local` first when the same schema and representative row exist.
5. For a production mutation, create a plan, show its exact row and field impact, obtain explicit confirmation for that token, then apply. Never combine preview and apply in one step.
6. Report the command outcome, affected row count, audit-plan path, and any unverified boundary.

## Core commands

Run from this skill directory:

```bash
python3 scripts/yxh_db.py catalog --env local
python3 scripts/yxh_db.py audit --env production --check all
python3 scripts/yxh_db.py backup --env production --schema-only
```

Do not place credentials in commands, environment files, SQL, output, plans, Git, or memory. The runtime reads the existing Obsidian credential note only through the guarded SSH askpass helper and reads database credentials inside the remote process without returning them.

## Boundaries

- Default to read-only production access.
- Never write order, payment, refund, member balance, bank, customer PII, permission, scheduler, or schema tables through the generic content editor.
- Do not use arbitrary write SQL. Use the plan/apply workflow and allowlist.
- Treat product creation/deletion as a multi-table operation requiring a bespoke reviewed transaction; the generic policy only updates existing `app_goods` rows.
- Treat restore and DDL as destructive. Produce a plan and require a new explicit confirmation; do not automate them with this suite.
- Redact sensitive fields from user-facing output. Use `--include-sensitive` only when the user explicitly needs a specific field and supply a reason.
