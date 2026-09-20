import test from 'node:test';
import assert from 'node:assert/strict';
import { retrieveImages } from './image-retrieval.mjs';

const fixture = () => {
  const requests = [];
  const service = { models: { embed: async queries => queries.map(() => [1, 2]) }, store: { config: { collection: 'test' },
    api: async (url, options) => { requests.push({ url, body: JSON.parse(options.body) }); return { result: { points: [
      { id: 'pic1', payload: { source_id: 'baseA', media: { fileToken: 'picture1', kind: 'image' } } },
      { id: 'vid1', payload: { source_id: 'baseA', media: { fileToken: 'videofile', kind: 'video', startSeconds: 4, endSeconds: 10 } } },
      { id: 'foreign', payload: { media: { fileToken: 'notAllowed', kind: 'image' } } },
    ] } }; } } };
  return { service, requests };
};
test('legacy image API preserves token rankings and filters', async () => {
  const { service, requests } = fixture();
  const result = await retrieveImages(service, { mode: 'images', queries: ['房间'], tokens: ['picture1'] });
  assert.deepEqual(result.rankings, [['picture1']]);
  assert.equal(requests[0].body.using, 'dense');
  assert.deepEqual(requests[0].body.filter.must.find(x => x.key === 'media.kind').match, { value: 'image' });
});
test('mixed API uses existing dense+lexical vectors and preserves video point/range identity', async () => {
  const { service, requests } = fixture();
  const result = await retrieveImages(service, { mode: 'media', queries: ['六菜一汤', '卧室'], tokens: ['picture1', 'videofile'] });
  assert.equal(result.rankings.length, 2);
  assert.deepEqual(result.rankings[0][1], { pointId: 'vid1', sourceId: 'baseA', token: 'videofile', kind: 'video', startSeconds: 4, endSeconds: 10 });
  for (const { body } of requests) {
    assert.equal(body.query.fusion, 'rrf');
    assert.deepEqual(body.prefetch.map(p => p.using), ['dense', 'lexical']);
    for (const p of body.prefetch) {
      assert.deepEqual(p.filter.must.find(x => x.key === 'media.kind').match.any, ['image', 'video']);
      assert.deepEqual(p.filter.must.find(x => x.key === 'permission_scope').match, { value: 'internal' });
      assert.deepEqual(p.filter.must.find(x => x.key === 'media.fileToken').match.any, ['picture1', 'videofile']);
      assert.ok(p.filter.must_not.some(x => x.key === 'orphan_relation'));
    }
  }
});
test('scope bounds, embedding mismatch and invalid store results fail explicitly', async () => {
  const { service } = fixture();
  for (const body of [{}, { queries: ['q'], tokens: ['bad'] }, { queries: Array(41).fill('q'), tokens: ['picture1'] },
    { queries: ['q'], tokens: Array(501).fill('picture1') }]) await assert.rejects(retrieveImages(service, body), /scope/);
  service.models.embed = async () => [];
  await assert.rejects(retrieveImages(service, { queries: ['q'], tokens: ['picture1'] }), /embedding count/);
  service.models.embed = async () => [[1]]; service.store.api = async () => ({});
  await assert.rejects(retrieveImages(service, { queries: ['q'], tokens: ['picture1'] }), /retrieval result/);
});
