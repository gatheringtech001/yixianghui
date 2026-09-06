import test from "node:test";
import assert from "node:assert/strict";
import { mkdtemp, writeFile, rm } from "node:fs/promises";
import path from "node:path";
import os from "node:os";
import { retrieveForAnswer, RetrievalInputError, queryStore } from "./answer-retrieval.mjs";

const point = (id, title, kind) => ({ id, payload: { title, content: title, media: kind ? { kind } : undefined } });
test("a base absent from global top-k is retrieved independently from known titles", async (t) => {
  const dir = await mkdtemp(path.join(os.tmpdir(), "rag-coverage-"));
  t.after(() => rm(dir, { recursive: true, force: true }));
  const manifestFile = path.join(dir, "manifest.json");
  await writeFile(manifestFile, JSON.stringify({ documents: { a: { title: "弥勒二号基地" }, b: { title: "普洱一号基地" } } }));
  const calls = [];
  const service = { manifestFile, models: { async embed(xs) { return xs.map(() => [1]); } },
    store: { async search() { return [point("p", "普洱一号基地")]; },
      async searchScoped(q, v, options) { calls.push(options); return options.titles.map((title) => point(title, title)); } } };
  const result = await retrieveForAnswer(service, "对比弥勒二号和普洱一号的午晚餐");
  assert.equal(calls.length, 2);
  assert.deepEqual(new Set(result.points.map((p) => p.payload.title)), new Set(["弥勒二号基地", "普洱一号基地"]));
});
test("mixed media uses independent image and video retrieval with a bounded merge", async () => {
  const calls = [];
  const service = { models: { async embed(xs) { return xs.map(() => [1]); } }, store: {
    async search() { return [point("v", "弥勒二号基地", "video")]; },
    async searchScoped(q, v, options) { calls.push(options); return Array.from({ length: 30 }, (_, i) => point(options.kind + i, "弥勒二号基地", options.kind)); },
  } };
  const result = await retrieveForAnswer(service, "弥勒二号的卧室图片和房型视频都找一下");
  assert.deepEqual(calls.map((c) => c.kind), ["image", "video"]);
  assert.equal(result.points.length, 30);
  assert.equal(result.points.filter((p) => p.payload.media.kind === "image").length, 15);
});

test("fanout limits fail explicitly and scoped Qdrant filters retain internal permissions", async () => {
  const names = ["弥勒一号", "弥勒二号", "弥勒三号", "弥勒四号", "弥勒五号"];
  const service = { models: { async embed() { return [[1]]; } }, store: {
    async search() { return names.map((name) => point(name, name + "基地")); },
  } };
  await assert.rejects(retrieveForAnswer(service, names.join("和")), RetrievalInputError);
  const store = { config: { collection: "test" }, async api(route, options) {
    const body = JSON.parse(options.body);
    for (const branch of body.prefetch) assert.deepEqual(branch.filter.must, [
      { key: "permission_scope", match: { value: "internal" } },
      { key: "title", match: { any: ["弥勒二号基地"] } },
      { key: "media.kind", match: { any: ["video"] } },
    ]);
    return { result: { points: [] } };
  } };
  await queryStore(store, { question: "图片和视频", dense: [1], titles: ["弥勒二号基地"], kind: "video" });
});
