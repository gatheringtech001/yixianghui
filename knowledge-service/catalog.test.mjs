import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { buildCatalog, catalogPoints, CATALOG_TABLES } from "./catalog.mjs";
import { syncCatalog } from "./catalog-sync.mjs";
import { contentHash } from "./lib.mjs";

const KEYS = ["goods_id", "category_id", "attr_id", "sku_id", "option_id", "data_id",
  "id", "ext_id", "activity_id", "category_id", "business_id", "business_id"];
function snapshot(rows = {}) {
  const tables = Object.fromEntries(CATALOG_TABLES.map((table) => [table, rows[table] ?? []]));
  return { version: 1, source: "production_mysql", exportedAt: "2026-09-05T01:00:00.000Z", tables,
    fingerprint: contentHash(JSON.stringify(tables)),
    schemas: Object.fromEntries(CATALOG_TABLES.map((table, i) => [table, { primaryKey: KEYS[i] }])),
  };
}

test("all catalog rows are represented including disabled goods and orphan children", () => {
  const source = snapshot({
    app_goods: [{ goods_id: "1", goods_name: "建水测试基地", category_id: "10", status: "0",
      content: '<p>包三餐</p><img src="photo.jpg">', goods_images: "photo.jpg", price: "0.01" }],
    app_goods_category: [{ category_id: "10", parent_id: "11", category_name: "建水" },
      { category_id: "11", parent_id: "0", category_name: "旅居" },
      { category_id: "12", parent_id: "0", category_name: "未使用分类" }],
    app_goods_sku: [{ sku_id: "20", goods_id: "1", sku_type: "200", sku_name: "标间" },
      { sku_id: "21", goods_id: "1", par_sku_id: "20", sku_type: "202", sku_name: "30天" },
      { sku_id: "22", goods_id: "999", sku_name: "孤立房型" }],
    app_goods_sku_option: [{ option_id: "30", goods_id: "1", sku_id: "21", option_type: "302", option_value: "1000" }],
    app_goods_education_ext: [{ ext_id: "1", goods_id: "1", course_time: "周一" }],
    app_activity: [{ activity_id: "2", activity_name: "书法活动", status: "1" }],
    app_activity_plan_feishu: [{ business_id: "3", canonical_table: "app_activity", canonical_id: "2" }],
  });
  const catalog = buildCatalog(source);
  assert.equal(catalog.represented, 11);
  assert.equal(catalog.orphans, 1);
  const goods = catalog.documents.find((document) => document.table === "app_goods");
  assert.match(goods.heading, /下架/);
  assert.match(goods.raw, /套餐总价/);
  assert.match(goods.raw, /父房型SKU_ID: 20/);
  const point = catalogPoints(goods, source.exportedAt)[0];
  assert.equal(point.payload.structured_data.app_goods[0].content, '<p>包三餐</p><img src="photo.jpg">');
  assert.equal(point.payload.structured_data.app_goods_sku[1].par_sku_id, "20");
  assert.equal(point.payload.structured_data.app_goods_category.length, 2);
  assert.equal(point.payload.product_status, "0");
  assert.doesNotMatch(point.text, /<p>|<img/);
  const activity = catalog.documents.find((document) => document.table === "app_activity");
  assert.equal(activity.groups.app_activity_plan_feishu[0].canonical_id, "2");
});

test("incomplete snapshots, hash corruption and duplicate IDs are rejected", () => {
  const missing = snapshot();
  delete missing.tables.app_goods_sku;
  missing.fingerprint = contentHash(JSON.stringify(missing.tables));
  assert.throws(() => buildCatalog(missing), /Incomplete/);
  const corrupt = snapshot();
  corrupt.tables.app_goods.push({ goods_id: "1" });
  assert.throws(() => buildCatalog(corrupt), /hash/);
  assert.throws(() => buildCatalog(snapshot({ app_goods: [{ goods_id: "1" }, { goods_id: "1" }] })), /duplicate/);
});

async function setup(context) {
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), "catalog-test-"));
  context.after(() => fs.rm(directory, { recursive: true, force: true }));
  const points = new Map([["existing-feishu", { payload: { source_type: "feishu_docx" } }]]);
  const store = {
    async ensureCollection() {},
    async upsert(rows) { for (const row of rows) points.set(row.id, row); },
    async deleteIds(ids) { for (const id of ids) points.delete(id); },
  };
  let embedCalls = 0;
  const models = { async embed(texts) { embedCalls += 1; return texts.map(() => [0.1, 0.2]); } };
  return { store, models, manifestFile: path.join(directory, "catalog.json"), points,
    calls: () => embedCalls };
}

test("catalog sync is incremental and never deletes Feishu vectors", async (context) => {
  const fixture = await setup(context);
  const input = snapshot({ app_goods: [{ goods_id: "1", goods_name: "建水", status: "1", price: "0.01" }] });
  const first = await syncCatalog({ ...fixture, snapshot: input });
  const second = await syncCatalog({ ...fixture, snapshot: input });
  assert.equal(first.changed, 1);
  assert.equal(second.changed, 0);
  assert.equal(fixture.calls(), 1);
  assert.equal(fixture.points.size, first.totalPoints + 1);
  const changed = snapshot({ app_goods: [{ ...input.tables.app_goods[0], price: "2000.00" }] });
  assert.equal((await syncCatalog({ ...fixture, snapshot: changed })).changed, 1);
  const empty = await syncCatalog({ ...fixture, snapshot: snapshot() });
  assert.equal(empty.totalPoints, 0);
  assert.equal(fixture.points.size, 1);
  assert.ok(fixture.points.has("existing-feishu"));
});

test("failed vector writes leave no completed manifest entry and retry safely", async (context) => {
  const fixture = await setup(context);
  const source = snapshot({ app_goods: [{ goods_id: "1", goods_name: "基地" }] });
  const failing = { ...fixture.store, async upsert() { throw new Error("unavailable"); } };
  await assert.rejects(syncCatalog({ ...fixture, store: failing, snapshot: source }), /unavailable/);
  await assert.rejects(fs.readFile(fixture.manifestFile), /ENOENT/);
  const result = await syncCatalog({ ...fixture, snapshot: source });
  assert.equal(result.changed, 1);
  assert.equal(fixture.points.size, 1 + result.totalPoints);
});

test("invalid embedding batches and older snapshots fail explicitly", async (context) => {
  const fixture = await setup(context);
  const input = snapshot({ app_goods: [{ goods_id: "1", goods_name: "基地" }] });
  await assert.rejects(syncCatalog({ ...fixture, snapshot: input, models: { async embed() { return []; } } }), /embedding/);
  await syncCatalog({ ...fixture, snapshot: input });
  const older = { ...input, exportedAt: "2026-09-04T01:00:00.000Z" };
  await assert.rejects(syncCatalog({ ...fixture, snapshot: older }), /Stale/);
});
