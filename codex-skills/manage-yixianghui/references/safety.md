# Production safety contract

1. Default production operations to read-only.
2. Read credentials only at runtime from the existing Obsidian/Bitwarden export note. Never print, copy, log, commit, or store secret values in plans or memory.
3. Use one validated read statement, explicit columns, and a maximum of 200 rows for ad hoc production queries.
4. Apply content writes only through `plan -> preview -> explicit token confirmation -> row/schema recheck -> transaction -> audit record`.
5. Require a second explicit token for deletion. Do not automate DDL or restore.
6. Reject writes to commerce, balances, PII, permissions, scheduler, and schema tables.
7. For multi-table changes, take a full consistent backup and prepare a bespoke transaction with rollback and postcondition queries.
8. Keep plan and backup artifacts under `~/.codex/yixianghui` with mode `0600`; keep them out of Git.
9. Stop on drift, missing primary keys, multiple primary keys, affected-row mismatch, validation failure, or credential/config discovery failure. Never silently fall back to another environment.
