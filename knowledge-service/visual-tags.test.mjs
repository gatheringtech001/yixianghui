import test from 'node:test';
import assert from 'node:assert/strict';
import { visualEvidence, validateVisualTags, enrichPayload } from './visual-tags.mjs';
import { contentHash } from './lib.mjs';

const payload = { title: '温泉基地', content: '温泉基地\n源文视频说明: 五星温泉疗养\n语音转写: 泳池\n画面: 庭院里有树木、石板路和木屋。水面升起水汽。\n画面文字: 全国第一\n不确定: 无法确认是温泉。\n画面: 室内可见床和窗帘，没有人物。', media: { kind: 'video', fileToken: 'original-file-token', startSeconds: 30, endSeconds: 60 }, permission_scope: 'internal', source_id: 'media:original' };
test('only direct visual descriptions become evidence, not captions, speech or guesses', () => {
  const evidence = visualEvidence(payload.content);
  assert.match(evidence.join(''), /树木.*石板路.*木屋/);
  assert.doesNotMatch(evidence.join(''), /五星|泳池|全国第一|温泉|人物/);
});
test('tags require exact visual support and known categories', () => {
  const evidence = visualEvidence(payload.content);
  const valid = validateVisualTags([{ name: '树木', category: 'objects', evidence: 0 }, { name: '木屋', category: 'objects', evidence: 0 }], evidence);
  assert.equal(valid.length, 2);
  assert.throws(() => validateVisualTags([{ name: '温泉', category: 'objects', evidence: 0 }], evidence), /evidence/);
  assert.throws(() => validateVisualTags([{ name: '树木', category: 'health', evidence: 0 }], evidence), /category/);
});
test('enrichment preserves source identity, permissions and exact time range; reapplying is stable', () => {
  const evidence = visualEvidence(payload.content);
  const record = { version: 1, sourceHash: contentHash(payload.content), evidence,
    tags: [{ name: '树木', category: 'objects', evidence: 0 }], annotatedAt: '2026-09-15T00:00:00Z' };
  const result = enrichPayload(payload, record);
  assert.equal(result.permission_scope, 'internal');
  assert.equal(result.source_id, payload.source_id);
  assert.equal(result.media.startSeconds, 30);
  assert.equal(result.media.endSeconds, 60);
  assert.equal(result.media.fileToken, payload.media.fileToken);
  assert.deepEqual(result.media.tags, ['树木']);
  assert.match(result.content, /视觉标签.*树木/);
  assert.deepEqual(enrichPayload(result, record), result);
  assert.throws(() => enrichPayload({ ...payload, content: 'changed' }, record), /changed/);
});
