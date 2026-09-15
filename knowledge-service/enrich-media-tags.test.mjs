import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { contentHash } from './lib.mjs';
import { prepareTagRecords, applyTagRecords } from './enrich-media-tags.mjs';
import { visualEvidence } from './visual-tags.mjs';

const point = { id: '00000000-1111-2222-3333-444444444444', payload: { source_id: 'media:one', permission_scope: 'internal',
  content: '画面: 树木旁边有石板路。', media: { kind: 'video', fileToken: 'original', startSeconds: 30, endSeconds: 60 } }, vector: { dense: [1, 0] } };
const record = { id: point.id, version: 1, sourceHash: contentHash(point.payload.content), evidence: visualEvidence(point.payload.content), tags: [{ name: '树木', category: 'objects', evidence: 0 }] };

test('unfounded model tags are recorded as rejected, never written as visual facts', async () => {
  const records = await prepareTagRecords([point], { config: { model: 'test' }, async complete() { return { output: { items: [{ id: point.id,
    tags: [...record.tags, { name: '温泉', category: 'objects', evidence: 0 }] }] } }; } });
  assert.equal(records[0].tags.length, 1);
  assert.equal(records[0].rejected.length, 1);
});
test('backup precedes update and all written payload is read back', async context => {
  const directory = await fs.mkdtemp(join(tmpdir(), 'visual-tags-test-'));
  context.after(() => fs.rm(directory, { recursive: true, force: true }));
  let stored = structuredClone(point);
  const store = { config: { collection: 'test', dimensions: 2 }, async api() { return { result: [stored] }; }, async upsert(rows) {
    assert.equal((await fs.readdir(directory)).length, 1); stored = rows[0];
  } };
  const result = await applyTagRecords({ points: [point], records: [record], store, models: { async embed() { return [[0.2, 0.4]]; } }, backupDirectory: directory });
  assert.equal(result[0].verified, true);
  assert.equal(stored.payload.permission_scope, 'internal');
  assert.equal(stored.payload.media.startSeconds, 30);
});
test('concurrent source edit during embedding is not overwritten', async context => {
  const directory = await fs.mkdtemp(join(tmpdir(), 'visual-conflict-test-'));
  context.after(() => fs.rm(directory, { recursive: true, force: true }));
  let stored = point;
  const store = { config: { collection: 'test', dimensions: 2 }, async api() { return { result: [stored] }; }, async upsert() { assert.fail('Must not overwrite'); } };
  await assert.rejects(applyTagRecords({ points: [point], records: [record], store, models: { async embed() {
    stored = { ...point, payload: { ...point.payload, content: 'changed' } }; return [[0.2, 0.4]];
  } }, backupDirectory: directory }), /changed/);
});
