import os from "node:os";
import path from "node:path";
import fs from "node:fs/promises";
import test from "node:test";
import assert from "node:assert/strict";
import { FeishuSource, KnowledgeService } from "./service.mjs";

test("Feishu refreshes an expired token before the next API request", async (context) => {
  const calls = [];
  context.mock.method(globalThis, "fetch", async (url, options) => {
    calls.push({ url, options });
    if (url.includes("tenant_access_token")) {
      return Response.json({ code: 0, tenant_access_token: "fresh-token", expire: 7200 });
    }
    return Response.json({ code: 0, data: { files: [] } });
  });
  const source = new FeishuSource({ appId: "test", appSecret: "test", folderToken: "root" });
  source.accessToken = "expired-token";
  source.tokenExpiresAt = Date.now() - 1;
  await source.listDocuments();
  assert.equal(calls.length, 2);
  assert.equal(calls[1].options.headers.authorization, "Bearer fresh-token");
});

test("an incomplete source listing cannot be mistaken for an empty knowledge base", async () => {
  const source = new FeishuSource({ folderToken: "root" });
  source.api = async () => ({ code: 0, data: {} });
  await assert.rejects(() => source.listDocuments(), /listing/);
  source.api = async () => ({ data: { files: [], has_more: true } });
  await assert.rejects(() => source.listDocuments(), /pagination/);
});

test("missing document content must fail instead of deleting its chunks", async () => {
  const source = new FeishuSource({});
  source.api = async () => ({ data: {} });
  await assert.rejects(() => source.readDocument("doc-1"), /content/);
});

test("renaming a source reindexes its title even when its body is unchanged", async (context) => {
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), "yxh-knowledge-rename-"));
  context.after(() => fs.rm(directory, { recursive: true, force: true }));
  let title = "旧基地名称";
  const points = [];
  const service = new KnowledgeService({
    source: {
      async listDocuments() { return [{ token: "doc", name: title, modified_time: "7" }]; },
      async readDocument() { return "相同正文"; },
    },
    models: { async embed(texts) { return texts.map(() => [0.1]); } },
    store: {
      async ensureCollection() {},
      async upsert(batch) { points.push(...batch); },
      async deleteIds() {},
    },
    manifestFile: path.join(directory, "manifest.json"),
  });
  await service.sync();
  title = "新基地名称";
  await service.sync();
  assert.equal(points.at(-1).payload.title, title);
});

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
  };
  const reranker = {
    async rerank(question, candidates, limit) {
      assert.equal(question, "哪里可以看梯田？");
      assert.equal(limit, 8);
      return candidates.map((item) => ({ ...item, rerankScore: 0.95 }));
    },
  };
  const service = new KnowledgeService({ source: {}, models, reranker, store, manifestFile: "unused" });
  const results = await service.search("哪里可以看梯田？");
  assert.equal(results.length, 1);
  assert.equal(results[0].title, "元阳基地");
  assert.equal(results[0].sourceUrl, "https://example.test/docx/1");
  assert.equal(results[0].score, 0.95);
  assert.equal(results[0].retrievalScore, 0.8);
  await assert.rejects(() => service.search("哪里可以看梯田？", 0), /limit/);
});
