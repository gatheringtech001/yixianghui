import os from "node:os";
import path from "node:path";
import fs from "node:fs/promises";
import test from "node:test";
import assert from "node:assert/strict";
import { KnowledgeService } from "./service.mjs";

test("sync is incremental and persists source metadata", async (context) => {
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), "yxh-knowledge-"));
  context.after(() => fs.rm(directory, { recursive: true, force: true }));
  const calls = { read: 0, embed: 0, upsert: [] };
  const source = {
    async listDocuments() {
      return [{ token: "doc-1", name: "建水三号基地", modified_time: "7" }];
    },
    async readDocument() {
      calls.read += 1;
      return "基地位于建水，提供住宿和餐饮。";
    },
  };
  const models = {
    async embed(texts) {
      calls.embed += 1;
      return texts.map(() => [0.1, 0.2]);
    },
  };
  const store = {
    async ensureCollection() {},
    async upsert(points) { calls.upsert.push(...points); },
    async deleteIds() {},
  };
  const service = new KnowledgeService({
    source,
    models,
    store,
    manifestFile: path.join(directory, "manifest.json"),
  });

  const first = await service.sync();
  const second = await service.sync();
  assert.deepEqual(first, { documents: 1, changed: 1, indexedChunks: 1 });
  assert.deepEqual(second, { documents: 1, changed: 0, indexedChunks: 0 });
  assert.equal(calls.read, 1);
  assert.equal(calls.embed, 1);
  assert.equal(calls.upsert.length, 1);
  assert.equal(calls.upsert[0].payload.permission_scope, "internal");
  assert.match(calls.upsert[0].payload.source_url, /doc-1$/);
});

test("search requires model reranking and returns source citations", async () => {
  const candidate = {
    id: "p1",
    score: 0.8,
    payload: {
      title: "元阳基地",
      content: "附近可观赏梯田。",
      source_url: "https://example.test/docx/1",
      source_id: "1",
      chunk_index: 0,
    },
  };
  const store = {
    async search(question, dense, limit) {
      assert.equal(question, "哪里可以看梯田？");
      assert.deepEqual(dense, [0.3, 0.4]);
      assert.equal(limit, 30);
      return [candidate];
    },
  };
  const models = {
    async embed() { return [[0.3, 0.4]]; },
    async rerank(question, candidates, limit) {
      assert.equal(question, "哪里可以看梯田？");
      assert.equal(limit, 8);
      return candidates;
    },
  };
  const service = new KnowledgeService({ source: {}, models, store, manifestFile: "unused" });
  const results = await service.search("哪里可以看梯田？");
  assert.equal(results.length, 1);
  assert.equal(results[0].title, "元阳基地");
  assert.equal(results[0].sourceUrl, "https://example.test/docx/1");
  await assert.rejects(() => service.search("哪里可以看梯田？", 0), /limit/);
});
