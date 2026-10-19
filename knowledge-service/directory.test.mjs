import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import { directoryPage, createDirectoryHandler, selectedDocument } from './directory.mjs';

const manifest = { documents: Object.fromEntries(Array.from({ length: 217 }, (_, n) => [`doc-${String(n).padStart(3, '0')}`, { title: `基地 ${n}` }])) };
test('directory pagination includes every indexed document without media filtering', () => {
  const items = []; let cursor = '';
  do {
    const page = directoryPage(manifest, new URLSearchParams({ limit: '100', ...(cursor ? { cursor } : {}) }));
    assert.equal(page.total, 217); items.push(...page.items); cursor = page.nextCursor;
  } while (cursor);
  assert.equal(new Set(items.map(item => item.id)).size, 217);
  assert.equal(items[216].title, '基地 216');
});
test('changed snapshots and invalid pagination fail rather than silently omit records', () => {
  const page = directoryPage(manifest, new URLSearchParams({ limit: '100' }));
  assert.throws(() => directoryPage({ documents: {} }, new URLSearchParams({ cursor: page.nextCursor })), { status: 409 });
  assert.throws(() => directoryPage(manifest, new URLSearchParams({ limit: '500' })), { status: 400 });
  assert.throws(() => directoryPage(manifest, new URLSearchParams({ cursor: 'bad' })), { status: 400 });
});
test('directory requires the existing query token and remains read-only', async t => {
  const handler = createDirectoryHandler({ token: 'query-test', load: async () => manifest });
  const server = http.createServer(handler);
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  t.after(() => { server.closeAllConnections(); server.close(); });
  const url = `http://127.0.0.1:${server.address().port}/documents`;
  assert.equal((await fetch(url)).status, 401);
  assert.equal((await fetch(url, { method: 'PUT', headers: { Authorization: 'Bearer query-test' } })).status, 405);
  const response = await fetch(url, { headers: { Authorization: 'Bearer query-test' } });
  assert.equal(response.status, 200); assert.equal((await response.json()).total, 217);
});

test('selected document reads exactly one catalog ID and preserves full body order', async () => {
  const calls = [];
  const text = '基地 1\n\n第一段：住宿。\n末段：服务与注意事项。';
  const result = await selectedDocument(manifest, 'doc-001', async id => { calls.push(id); return text; });
  assert.deepEqual(calls, ['doc-001']);
  assert.equal(result.complete, true);
  assert.equal(result.document.id, 'doc-001');
  assert.equal(result.document.title, '基地 1');
  assert.equal(result.document.text, text);
});

test('unknown and invalid IDs cannot read arbitrary Feishu documents', async () => {
  for (const [id, status] of [['missing', 404], ['__proto__', 404], ['', 400], ['../doc-001', 400]]) {
    await assert.rejects(selectedDocument(manifest, id, () => { throw new Error('must not read'); }), { status });
  }
  await assert.rejects(selectedDocument(manifest, 'doc-001', async () => ''), /body unavailable/);
});

test('selected document endpoint keeps auth, directory and media routes independent', async t => {
  const calls = [];
  const handler = createDirectoryHandler({ token: 'query-test', load: async () => manifest,
    readDocument: async id => { calls.push(id); return '完整原文\n第二段。'; },
    loadMedia: async () => ({ items: [{ id: 'image-1' }], total: 1 }) });
  const server = http.createServer(handler);
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  t.after(() => { server.closeAllConnections(); server.close(); });
  const base = `http://127.0.0.1:${server.address().port}/documents`;
  const headers = { Authorization: 'Bearer query-test' };
  assert.equal((await fetch(base + '?id=doc-001')).status, 401);
  assert.equal((await fetch(base + '?id=doc-001', { headers, method: 'POST' })).status, 405);
  assert.equal((await fetch(base + '?id=missing', { headers })).status, 404);
  assert.deepEqual(calls, []);
  const response = await fetch(base + '?id=doc-001', { headers });
  assert.equal(response.status, 200);
  assert.match(response.headers.get('cache-control'), /no-store/);
  assert.equal((await response.json()).document.text, '完整原文\n第二段。');
  assert.equal((await (await fetch(base, { headers })).json()).total, 217);
  assert.equal((await (await fetch(base + '?type=media', { headers })).json()).total, 1);
  assert.deepEqual(calls, ['doc-001']);
});
