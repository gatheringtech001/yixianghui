import assert from "node:assert/strict";
import { test } from "node:test";
import http from "node:http";
import fs from 'node:fs';
import { createAssetHandler, parseWriterTokens } from "./assets-http.mjs";

const token = "test-writer-" + "x".repeat(32);
test('production asset proxy permits scoped index deletion', () => {
  const nginx = fs.readFileSync(new URL('./nginx.conf', import.meta.url), 'utf8');
  assert.match(nginx, /location \^~ \/knowledge\/assets\/ \{\s*limit_except GET PUT DELETE/);
});
async function fixture(t) {
  const calls = [];
  const index = { async get(id) { calls.push(id); return { status: "indexed" }; },
    async remove(id, body) { calls.push({ id, body }); return { status: 'deleted' }; },
    async listWorkbook(id) { calls.push(id); return { items: [], total: 0 }; },
    async put(id, body) { calls.push({ id, body }); return { status: "created", ...id }; } };
  const handler = createAssetHandler({ tokens: { "external-materials": token }, index });
  const server = http.createServer(async (req, res) => { if (!await handler(req, res)) { res.writeHead(404); res.end(); } });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  t.after(() => { server.closeAllConnections(); server.close(); });
  const url = `http://127.0.0.1:${server.address().port}/knowledge/assets/`;
  return { calls, request: (path, options = {}) => fetch(url + path, options) };
}
test("write keys are scoped and must differ from query/admin keys", async (t) => {
  assert.throws(() => parseWriterTokens({ "external-materials": token }, [token]));
  assert.throws(() => parseWriterTokens({ "external-materials": token, another: token }));
  const { request, calls } = await fixture(t);
  for (const [path, key] of [["external-materials/a", "query-key"], ["other-tool/a", token], ["constructor/a", String(Object)]]) {
    assert.equal((await request(path, { method: "PUT", headers: { authorization: "Bearer " + key } })).status, 401);
  }
  assert.equal(calls.length, 0);
});
test("authenticated PUT and GET support index submission and readback only", async (t) => {
  const { request, calls } = await fixture(t); const headers = { authorization: "Bearer " + token, "content-type": "application/json" };
  assert.equal((await request("external-materials/a", { method: "PUT", headers, body: JSON.stringify({ title: "index" }) })).status, 201);
  assert.equal((await request("external-materials/a", { headers })).status, 200);
  assert.equal((await request("external-materials/a", { method: "DELETE", headers, body: '{}' })).status, 200);
  assert.equal((await request('external-materials/workbook-inventory', { headers })).status, 200);
  assert.equal((await request('external-materials/workbook-inventory', { method: 'PUT', headers, body: '{}' })).status, 405);
  assert.equal(calls.length, 4);
});
test("file uploads, oversized requests and broken JSON are rejected", async (t) => {
  const { request, calls } = await fixture(t);
  const headers = { authorization: "Bearer " + token, "content-type": "application/json" };
  assert.equal((await request("external-materials/a", { method: "PUT", headers, body: "{" })).status, 400);
  assert.equal((await request("external-materials/a", { method: "PUT", headers, body: "x".repeat(33000) })).status, 413);
  assert.equal((await request("external-materials/a", { method: "PUT", headers: { ...headers, "content-type": "multipart/form-data" }, body: "file" })).status, 415);
  assert.equal(calls.length, 0);
});
