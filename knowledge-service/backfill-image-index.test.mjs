import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import { join } from 'node:path';
import { gunzipSync } from 'node:zlib';
import { backfillImages } from './backfill-image-index.mjs';
import { imageRevision } from './image-index.mjs';

test('backfill is dry by default and verifies float32 vectors after backup', async t => {
  const directory = await fs.mkdtemp(join(os.tmpdir(), 'image-backfill-'));
  t.after(() => fs.rm(directory, { recursive: true, force: true }));
  const point = { id: 'image1', payload: { content: '客房', media: { kind: 'image', fileToken: 'token12345' } }, vector: { dense: [1] } };
  let current = structuredClone(point), writes = 0;
  const store = { config: { collection: 'test', dimensions: 1 }, api: async () => ({ result: [structuredClone(current)] }),
    upsert: async ([next]) => {
      assert.deepEqual(JSON.parse(gunzipSync(await fs.readFile(join(directory, 'image1.json.gz')))), point);
      writes++; current = structuredClone(next);
      const norm = Math.hypot(...current.vector.dense);
      current.vector.dense = current.vector.dense.map(v => Math.fround(v / norm));
    } };
  const options = { store, directory, points: [point], models: { embed: async () => [[.123456789]] },
    indexer: async p => ({ ...p, payload: { ...p.payload, media: { ...p.payload.media, imageIndex: { version: 1,
      sourceRevision: imageRevision(p.payload), contentHash: 'a'.repeat(64), description: '客房', text: 'natural', face: false } } } }) };
  assert.equal((await backfillImages(options)).pending, 1); assert.equal(writes, 0);
  const result = await backfillImages({ ...options, apply: true });
  assert.deepEqual(result.failed, []); assert.deepEqual(result.written, ['image1']);
  assert.equal((await backfillImages({ ...options, points: [current], apply: true })).pending, 0);
});

test('backfill stops immediately on concurrent source changes', async t => {
  const directory = await fs.mkdtemp(join(os.tmpdir(), 'image-backfill-'));
  t.after(() => fs.rm(directory, { recursive: true, force: true }));
  let calls = 0;
  const result = await backfillImages({ apply: true, directory,
    points: Array.from({ length: 10 }, (_, i) => ({ id: String(i), payload: { media: { kind: 'image' } } })),
    store: { config: { collection: 'test', dimensions: 1 }, api: async () => ({ result: [{ payload: { changed: true } }] }),
      upsert: async () => { throw new Error('must not write'); } }, models: { embed: async () => [[1]] },
    indexer: async point => { calls++; return { ...point, payload: { ...point.payload, media: { ...point.payload.media, imageIndex: {
      version: 1, sourceRevision: imageRevision(point.payload), contentHash: 'a'.repeat(64), description: '客房', text: 'clean', face: false,
    } } } }; } });
  assert.equal(calls, 1); assert.equal(result.failed.length, 1); assert.equal(result.written.length, 0);
  assert.match(result.failed[0].reason, /Concurrent/);
});

test('backfill rejects a changed vector direction despite matching payload', async t => {
  const directory = await fs.mkdtemp(join(os.tmpdir(), 'image-backfill-'));
  t.after(() => fs.rm(directory, { recursive: true, force: true }));
  const point = { id: 'image1', payload: { media: { kind: 'image', fileToken: 'token12345' } } };
  let current = structuredClone(point);
  const result = await backfillImages({ apply: true, directory, points: [point],
    store: { config: { collection: 'test', dimensions: 2 }, api: async () => ({ result: [structuredClone(current)] }),
      upsert: async ([next]) => { current = structuredClone(next); current.vector.dense = [0, 1]; } },
    models: { embed: async () => [[.3, .4]] },
    indexer: async p => ({ ...p, payload: { ...p.payload, media: { ...p.payload.media, imageIndex: {
      version: 1, sourceRevision: imageRevision(p.payload), contentHash: 'a'.repeat(64), description: '客房', text: 'clean', face: false,
    } } } }) });
  assert.equal(result.written.length, 0);
  assert.match(result.failed[0].reason, /readback mismatch/);
});

test('offline backfill honors bounded rate-limit cooldown and never retries validation errors', async t => {
  const directory = await fs.mkdtemp(join(os.tmpdir(), 'image-backfill-'));
  t.after(() => fs.rm(directory, { recursive: true, force: true }));
  const delays = []; let attempts = 0;
  const options = { apply: true, directory, points: [{ id: 'image1', payload: { media: { kind: 'image' } } }],
    store: { config: { collection: 'test' } }, delay: async ms => delays.push(ms),
    indexer: async () => { attempts++; throw Object.assign(new Error('rate limited'), { statusCode: 429, retryAfter: 61 }); } };
  const result = await backfillImages(options);
  assert.equal(attempts, 3); assert.deepEqual(delays, [61000, 61000]); assert.equal(result.failed.length, 1);
  attempts = 0;
  await backfillImages({ ...options, indexer: async () => { attempts++; throw new Error('invalid original'); } });
  assert.equal(attempts, 1);
});

test('isolated failures do not stop the library; persistent failures stop after ten items', async t => {
  const directory = await fs.mkdtemp(join(os.tmpdir(), 'backfill-failures-'));
  t.after(() => fs.rm(directory, { recursive: true, force: true }));
  const points = Array.from({ length: 15 }, (_, i) => ({ id: String(i), payload: { media: { kind: 'image' } } }));
  const stored = new Map(points.map(p => [p.id, p]));
  const options = { directory, points, apply: true, models: { embed: async () => [[1]] },
    store: { config: { collection: 'test', dimensions: 1 }, api: async (_, request) => ({ result: JSON.parse(request.body).ids.map(id => stored.get(id)) }),
      upsert: async rows => rows.forEach(p => stored.set(p.id, p)) },
    indexer: async p => {
      if (Number(p.id) % 2 === 0) throw new Error('bad source');
      return { ...p, payload: { ...p.payload, media: { ...p.payload.media, imageIndex: {
        version: 1, sourceRevision: imageRevision(p.payload), contentHash: 'a'.repeat(64), description: '客房', text: 'natural', face: false,
      } } } };
    } };
  const result = await backfillImages(options);
  assert.equal(result.written.length, 7); assert.equal(result.failed.length, 8); assert.equal(result.stoppedReason, null);
  let calls = 0; const delays = [];
  const failed = await backfillImages({ ...options, delay: async ms => delays.push(ms),
    indexer: async () => { calls++; throw Object.assign(new Error('timeout'), { statusCode: 502 }); } });
  assert.equal(failed.failed.length, 10); assert.equal(calls, 30); assert.equal(failed.stoppedReason, '10-consecutive-failures');
  assert.deepEqual(delays.slice(0, 2), [5000, 10000]);
});
