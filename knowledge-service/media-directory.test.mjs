import test from 'node:test';
import assert from 'node:assert/strict';
import { mediaDirectoryPage } from './media-directory.mjs';

test('media catalog uses pagination without a semantic top-ten cutoff and preserves tags and time ranges', async () => {
  let request;
  const next = '00000000-1111-2222-3333-444444444444';
  const store = { config: { collection: 'test' }, async api(route, options) {
    if (route.endsWith('/count')) return { result: { count: 2 } };
    request = JSON.parse(options.body);
    return { result: { points: [{ id: 'one', payload: { source_id: 'media:one', title: '庭院', content: '有树木',
      media: { kind: 'video', tags: ['树木'], startSeconds: 30, endSeconds: 60 } } }], next_page_offset: next } };
  } };
  const result = await mediaDirectoryPage(store, new URLSearchParams({ kind: 'video', limit: '200' }));
  assert.equal(request.limit, 200);
  assert.deepEqual(request.filter.must[0], { key: 'permission_scope', match: { value: 'internal' } });
  assert.deepEqual(result.items[0].tags, ['树木']);
  assert.equal(result.items[0].media.startSeconds, 30);
  assert.equal(result.nextCursor, next);
  await assert.rejects(mediaDirectoryPage(store, new URLSearchParams({ limit: '201' })), /invalid/);
  await assert.rejects(mediaDirectoryPage(store, new URLSearchParams({ cursor: '../wrong' })), /invalid/);
});
