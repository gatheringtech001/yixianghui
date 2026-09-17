import test from 'node:test';
import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { indexImage, currentImageIndex } from './image-index.mjs';
import { mediaUsagePolicy } from './media-usage-policy.mjs';
import { retrieveImages } from './image-retrieval.mjs';
const payload = { content: '图片画面: 客房和床头牌', source_url: 'https://vcnnjnb870d6.feishu.cn/docx/abc',
  media: { kind: 'image', fileToken: 'abcdefghijklmnop' } };
const bytes = Buffer.from([255, 216, 0, 128]);
const options = text => ({ open: async () => new Response(bytes, { headers: { 'content-type': 'image/jpeg' } }),
  labeler: { config: { model: 'test' }, complete: async () => ({ output: { description: '客房双床与床头牌', text, face: false } }) } });
test('ingestion binds binary hash, reuses unchanged labels and invalidates changed source', async () => {
  const next = await indexImage(payload, options('natural'));
  assert.equal(next.media.imageIndex.contentHash, createHash('sha256').update(bytes).digest('hex'));
  assert.equal(await indexImage(next, {}), next);
  assert.equal(currentImageIndex({ ...next, source_updated_at: 'new' }), null);
  assert.equal(mediaUsagePolicy(next).usable, true);
  assert.equal(mediaUsagePolicy(next).hasPostproductionText, false);
  assert.equal(mediaUsagePolicy(next).hasVisibleText, true);
});
test('edited images blocked, uncertain images pending, malformed response fails ingestion', async () => {
  assert.equal(mediaUsagePolicy(await indexImage(payload, options('edited'))).usable, false);
  assert.equal(mediaUsagePolicy(await indexImage(payload, options('uncertain'))).usable, null);
  await assert.rejects(indexImage(payload, options('whatever')), /Invalid image indexing/);
});
test('ingestion never clears explicit manual or rights bans', async () => {
  const next = await indexImage(payload, options('natural'));
  for (const fields of [{ tags: ['人工禁用'] }, { status: 'rejected' }, { usage: { usable: false, reason: '版权未授权' } }]) {
    assert.equal(mediaUsagePolicy({ ...next, media: { ...next.media, ...fields } }).usable, false);
  }
});
test('query only embeds narration and restricts Qdrant to permitted tokens', async () => {
  const service = { models: { embed: async queries => { assert.deepEqual(queries, ['客房']); return [[1, 0]]; } },
    store: { config: { collection: 'test' }, api: async (_, options) => {
      const body = JSON.parse(options.body);
      assert.ok(body.filter.must.some(f => f.key === 'media.fileToken' && f.match.any[0] === 'abcdefghijklmnop'));
      return { result: { points: [{ payload: { media: { fileToken: 'abcdefghijklmnop' } } }, { payload: { media: { fileToken: 'foreign' } } }] } };
    } } };
  assert.deepEqual(await retrieveImages(service, { queries: ['客房'], tokens: ['abcdefghijklmnop'] }), { rankings: [['abcdefghijklmnop']] });
  await assert.rejects(retrieveImages(service, { queries: ['客房'], tokens: [] }), /Invalid/);
});
