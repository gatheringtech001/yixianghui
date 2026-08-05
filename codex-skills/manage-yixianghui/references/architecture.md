# Architecture evidence

- Repository: `/Users/kevin/Documents/yixianghui`.
- Admin frontend: `ruoyi-ui/src/views/system` and `ruoyi-ui/src/api/system`.
- Backend entry and 44 business controllers: `lankong-admin/src/main/java/com/ruoyi/web/controller/app`.
- Domain, service, and mapper layer: `ruoyi-system/src/main/java/com/ruoyi/system` and `ruoyi-system/src/main/resources/mapper/system`.
- Mini-program: `shop-mnp`.
- Local E2E profile: `lankong-admin/src/main/resources/application-e2e.yml`.
- Local environment setup: `scripts/e2e/backend.sh`.

Observed 2026-08-05 inventory: 70 total REST controllers, 44 app controllers, 78 database tables, 51 `app_*` tables, 18 `sys_*` tables, 2 generator tables, and 7 Quartz tables. Production and local table-family counts matched at discovery time; re-run `catalog` because this fact can drift.

Production database access is SSH-mediated. The helper authenticates from the existing Obsidian credential note, extracts datasource configuration inside `/home/shzxj/lankong-admin.jar` on the server, and invokes local MySQL there. No database credential should cross back to the local process.
