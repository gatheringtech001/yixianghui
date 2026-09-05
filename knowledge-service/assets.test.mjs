import assert from "node:assert/strict";
import { test } from "node:test";
import { normalizeAsset } from "./asset-schema.mjs";
import { AssetIndex } from "./assets.mjs";

const identity = { source: "external-materials", id: "room-01" };
const input = { kind: "image", title: "双床客房", content: "房间有两张床，窗外可以看到花园。", url: "https://example.com/room.jpg", tags: ["客房", "花园"], updatedAt: "2026-09-05T01:00:00Z", media: { access: "public" } };
function fixture() {
  const points = new Map(); const calls = { embed: 0, upsert: 0, payload: 0 };
  const store = { config: { collection: "test", dimensions: 3 },
    async api(route, options) {
      const body = JSON.parse(options.body);
      if (route.includes("/payload")) { calls.payload++; for (const id of body.points) points.get(id).payload = body.payload; return { status: "ok" }; }
      return { result: body.ids.map((id) => points.get(id)).filter(Boolean) };
    },
    async upsert(batch) { calls.upsert++; for (const point of batch) points.set(point.id, structuredClone(point)); },
  };
  const models = { async embed() { calls.embed++; return [[0.1, 0.2, 0.3]]; } };
  return { index: new AssetIndex({ store, models }), points, calls, models, store };
}
test("normalized indexes reject files, unknown fields and unsafe URLs", () => {
  assert.equal(normalizeAsset(identity, input).media.access, "public");
  for (const patch of [{ file: "base64" }, { url: "https://user:password@example.com/a" },
    { url: "https://example.com/a?token=secret" }, { url: "https://127.0.0.1/a" },
    { content: "x".repeat(4000) }, { kind: "binary" }, { updatedAt: "yesterday" }]) {
    assert.throws(() => normalizeAsset(identity, { ...input, ...patch }), { status: 400 });
  }
});
test("identities and video bounds are validated", () => {
  assert.throws(() => normalizeAsset({ source: "../yuque", id: "x" }, input), { status: 400 });
  assert.throws(() => normalizeAsset(identity, { ...input, kind: "video", media: { startSeconds: 20, endSeconds: 10 } }), { status: 400 });
});
test("upsert is idempotent and stores an isolated external source", async () => {
  const { index, calls, points } = fixture();
  const first = await index.put(identity, input);
  assert.equal(first.status, "created");
  assert.equal((await index.put(identity, input)).status, "unchanged");
  assert.equal(calls.embed, 1); assert.equal(points.size, 1);
  const payload = [...points.values()][0].payload;
  assert.equal(payload.source_type, "external_asset"); assert.equal(payload.source_id, "external:external-materials:room-01");
  assert.deepEqual((await index.get(identity)).asset.tags, input.tags.slice().sort());
});
test("metadata changes reuse the vector, content changes replace the same point", async () => {
  const { index, calls, points } = fixture();
  await index.put(identity, input);
  assert.equal((await index.put(identity, { ...input, url: "https://example.com/new.jpg", updatedAt: "2026-09-05T02:00:00Z" })).status, "updated");
  assert.equal(calls.embed, 1); assert.equal(calls.payload, 1);
  await index.put(identity, { ...input, content: "阳台面向花园，房内有两张床。", updatedAt: "2026-09-05T03:00:00Z" });
  assert.equal(calls.embed, 2); assert.equal(points.size, 1);
});
test("older revisions and conflicting same-time revisions cannot overwrite", async () => {
  const { index } = fixture(); await index.put(identity, input);
  await assert.rejects(index.put(identity, { ...input, updatedAt: "2026-09-04T00:00:00Z" }), { status: 409 });
  await assert.rejects(index.put(identity, { ...input, title: "Changed" }), { status: 409 });
});
test("embedding failure and malformed vectors leave the previous index unchanged", async () => {
  const { index, models } = fixture(); await index.put(identity, input);
  models.embed = async () => [[NaN, 0, 1]];
  await assert.rejects(index.put(identity, { ...input, content: "new content", updatedAt: "2026-09-05T04:00:00Z" }));
  assert.equal((await index.get(identity)).asset.content, input.content);
});
test("point identity is isolated across sources and collision checks fail closed", async () => {
  const { index, points } = fixture(); await index.put(identity, input);
  await index.put({ source: "another-tool", id: identity.id }, input); assert.equal(points.size, 2);
  [...points.values()][0].payload.source_type = "feishu_docx";
  await assert.rejects(index.put(identity, input), { status: 409 });
});
test("concurrent writes to one identity cannot race", async () => {
  const { index, models } = fixture(); let begin; let finish;
  const entered = new Promise((resolve) => { begin = resolve; });
  models.embed = async () => { begin(); return new Promise((resolve) => { finish = resolve; }); };
  const pending = index.put(identity, input); await entered;
  await assert.rejects(index.put(identity, input), { status: 409, code: "write_in_progress" });
  finish([[1, 2, 3]]); assert.equal((await pending).status, "created");
});
test("malformed storage reads and failed writes cannot be reported as successful", async () => {
  const { index, store } = fixture(); store.api = async () => ({});
  await assert.rejects(index.put(identity, input));
  store.api = async () => ({ result: [] }); store.upsert = async () => { throw new Error("unavailable"); };
  await assert.rejects(index.put(identity, input));
});
