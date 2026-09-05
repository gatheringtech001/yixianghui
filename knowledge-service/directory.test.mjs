import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import { directoryPage, createDirectoryHandler } from './directory.mjs';

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
