---
name: edit-yixianghui-content
description: Plan, preview, back up, and apply allowlisted Yixianghui content changes by exact primary key with optimistic rechecks and audit records. Use when editing 逸享荟 products, SKUs, education extensions, activities, ad assets, articles, categories, or pages; also use for controlled inserts or deletes on supported leaf content tables.
---

# Edit Yixianghui Content

Use `/Users/kevin/.codex/skills/manage-yixianghui/scripts/yxh_db.py`. Never execute arbitrary write SQL.

## Plan and apply

1. Inspect the target row and schema. Verify the user-facing object maps to the expected primary key.
2. Reproduce the change on `local` first when representative data exists.
3. Create a plan with a non-empty JSON object. The plan stores the preimage with mode `0600`, shows only requested field differences, and expires after 30 minutes.
4. Show the environment, table, primary key, before/after values, and token. Stop for explicit confirmation before production apply.
5. Apply the exact plan token. The CLI rechecks schema and row state before the transaction and records the result.

```bash
python3 /Users/kevin/.codex/skills/manage-yixianghui/scripts/yxh_db.py plan --env local --action update \
  --table app_goods --id 12 --data '{"goods_name":"新标题","status":"0"}'

python3 /Users/kevin/.codex/skills/manage-yixianghui/scripts/yxh_db.py apply --plan /absolute/plan.json

python3 /Users/kevin/.codex/skills/manage-yixianghui/scripts/yxh_db.py apply --plan /absolute/plan.json \
  --confirm-production PLAN_TOKEN
```

For an allowed delete, also require `--confirm-delete PLAN_TOKEN` after the user confirms deletion explicitly.

## Refuse or escalate

- Refuse generic edits to orders, payments, refunds, balances, bank records, customer PII, system permissions, jobs, or schemas.
- Do not create or delete a product with the generic editor. Products span goods, SKU, options, related sections, education extension, and dependent commerce rows; prepare a bespoke transaction and backup instead.
- Do not edit primary keys, timestamps, counters, stable ad position codes, or single-page keys through this skill.
- If validation fails, report the exact rejected boundary. Do not bypass the guardrail.
