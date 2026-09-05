import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs/promises";
import path from "node:path";
import os from "node:os";
import { mediaPoints, syncMedia } from "./media-sync.mjs";

const record = { id: "base:image:hash", title: "基地餐厅", text: "画面中有餐桌和窗户。",
  sourceUrl: "https://vcnnjnb870d6.feishu.cn/docx/doc1", sourceUpdatedAt: "2026-09-05",
  media: { kind: "image", fileToken: "image-token" } };

test("media points keep file identity and video timestamps", () => {
  const point = mediaPoints({ ...record, media: { kind: "video", startSeconds: 30, endSeconds: 60 } }, "2026-09-05")[0];
  assert.equal(point.payload.media.startSeconds, 30);
  assert.equal(point.payload.source_type, "yuque_media");
  assert.match(point.payload.source_id, /^media:/);
  assert.throws(() => mediaPoints({ ...record, sourceUrl: "http://wrong.test" }, "2026-09-05"));
});

test("media sync is incremental and missing records are not automatically deleted", async (context) => {
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), "yxh-media-"));
  context.after(() => fs.rm(directory, { recursive: true, force: true }));
  const points = new Map([["existing-product", {}]]);
  let embeds = 0;
  const options = { manifestFile: path.join(directory, "manifest.json"),
    store: { config: { collection: "test" }, async ensureCollection() {},
      async upsert(rows) { for (const row of rows) points.set(row.id, row); },
      async deleteIds(ids) { for (const id of ids) points.delete(id); }, async api() {} },
    models: { async embed(texts) { embeds++; return texts.map(() => [0.1]); } } };
  const snapshot = { version: 1, createdAt: "2026-09-05T00:00:00Z", records: [record], failures: [] };
  assert.equal((await syncMedia({ ...options, snapshot })).changedRecords, 1);
  assert.equal((await syncMedia({ ...options, snapshot })).changedRecords, 0);
  assert.equal(embeds, 1);
  const moved = { ...snapshot, records: [{ ...record, sourceUrl: "https://vcnnjnb870d6.feishu.cn/docx/doc2" }] };
  assert.equal((await syncMedia({ ...options, snapshot: moved })).payloadUpdates, 1);
  assert.equal(embeds, 1);
  const missing = await syncMedia({ ...options, snapshot: { ...snapshot, records: [] } });
  assert.equal(missing.missingCandidates, 1);
  assert.equal(points.size, 2);
});
