---
name: audit-yixianghui-db
description: Audit Yixianghui MySQL data for relational integrity, commerce anomalies, missing education/content links, and asset-path problems without changing data. Use for 逸享荟 health checks, production readiness, local-versus-production drift investigation, orphan records, missing images, inconsistent orders/payments/refunds, and pre/post-change verification.
---

# Audit Yixianghui Database

Use `/Users/kevin/.codex/skills/manage-yixianghui/scripts/yxh_db.py`. Audits are read-only and return issue counts, not automatic repairs.

## Run checks

```bash
python3 /Users/kevin/.codex/skills/manage-yixianghui/scripts/yxh_db.py audit --env local --check all
python3 /Users/kevin/.codex/skills/manage-yixianghui/scripts/yxh_db.py audit --env production --check relations
python3 /Users/kevin/.codex/skills/manage-yixianghui/scripts/yxh_db.py audit --env production --check assets
```

Available groups:

- `relations`: orphan goods/SKU/options/education/order-detail links.
- `commerce`: pay logs without orders, refunds exceeding recorded payment, negative order amounts.
- `content`: education goods without extension rows and content with missing categories/positions.
- `assets`: enabled records with empty cover/image content and insecure HTTP asset URLs.

For every nonzero result, run a narrow read-only query to identify exact primary keys before recommending a fix. State whether the invariant is certain or only a warning. Never infer a repair from a count alone, and never apply a repair through the audit skill.
