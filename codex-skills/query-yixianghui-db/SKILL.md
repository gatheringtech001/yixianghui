---
name: query-yixianghui-db
description: Safely query and summarize the Yixianghui local or production MySQL database with single-statement validation, row limits, sensitive-field guards, and read-only production transactions. Use for 逸享荟/Yixianghui counts, lists, status checks, reports, record lookup, schema inspection, production-versus-local comparison, and evidence-backed backend questions.
---

# Query Yixianghui Database

Use `/Users/kevin/.codex/skills/manage-yixianghui/scripts/yxh_db.py` for every query.

## Procedure

1. State `local` or `production`. Default to `local` unless the user asks for current/live/production data.
2. Use `catalog` or `schema` before guessing a table or column.
3. Generate one read-only SQL statement. Select explicit columns in production and add stable ordering where row order matters.
4. Run `query`. The CLI rejects write keywords, comments, stacked statements, dangerous functions, system schemas, production `SELECT *`, and unbounded results.
5. Summarize the result and distinguish exact counts from estimates.

```bash
python3 /Users/kevin/.codex/skills/manage-yixianghui/scripts/yxh_db.py catalog --env production
python3 /Users/kevin/.codex/skills/manage-yixianghui/scripts/yxh_db.py schema --env local --table app_goods
python3 /Users/kevin/.codex/skills/manage-yixianghui/scripts/yxh_db.py query --env production \
  --sql "SELECT goods_id, goods_name, status FROM app_goods ORDER BY goods_id DESC LIMIT 20"
```

Do not expose mobile numbers, ID cards, addresses, OpenIDs, bank data, tokens, passwords, or payment notification bodies. If a user explicitly requires a sensitive field, add `--include-sensitive --reason "..."`, minimize rows, and keep raw values out of the final response.
