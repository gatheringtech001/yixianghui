import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { loadRetiredSources } from './retired-sources.mjs';
import { KnowledgeService } from './service.mjs';
import { syncMedia } from './media-sync.mjs';
import { syncCatalog } from './catalog-sync.mjs';
import { CATALOG_TABLES } from './catalog.mjs';
import { contentHash } from './lib.mjs';

test('retirement uses exact source identities and fails closed for invalid configured files', async t => {
  const dir = await fs.mkdtemp(path.join(os.tmpdir(), 'kb-retirement-'));
  t.after(() => fs.rm(dir, { recursive: true, force: true }));
  const file = path.join(dir, 'retired.json');
  await assert.rejects(loadRetiredSources(file), /ENOENT/);
  await fs.writeFile(file, JSON.stringify({ version: 1, sourceIds: ['catalog:app_goods:57', 'old-doc'] }));
  const ids = await loadRetiredSources(file);
  assert.equal(ids.has('catalog:app_goods:57'), true);
  assert.equal(ids.has('catalog:app_goods:570'), false);
  assert.equal(ids.has('new-doc'), false);
  await fs.writeFile(file, JSON.stringify({ version: 1, sourceIds: ['duplicate', 'duplicate'] }));
  await assert.rejects(loadRetiredSources(file), /Invalid/);
});

test('all sync paths exclude retired sources and preserve unrelated records', async t => {
  const dir = await fs.mkdtemp(path.join(os.tmpdir(), 'kb-sync-retirement-'));
  const previous = process.env.KNOWLEDGE_RETIRED_SOURCES_FILE;
  t.after(async () => {
    if (previous === undefined) delete process.env.KNOWLEDGE_RETIRED_SOURCES_FILE;
    else process.env.KNOWLEDGE_RETIRED_SOURCES_FILE = previous;
    await fs.rm(dir, { recursive: true, force: true });
  });
  const file = path.join(dir, 'retired.json');
  await fs.writeFile(file, JSON.stringify({ version: 1,
    sourceIds: ['old-doc', 'catalog:app_goods:57', 'media:old-image'] }));
  process.env.KNOWLEDGE_RETIRED_SOURCES_FILE = file;
  const indexed = [], removed = [];
  const store = { ensureCollection: async () => {},
    upsert: async rows => indexed.push(...rows), deleteIds: async ids => removed.push(...ids) };
  const models = { embed: async texts => texts.map(() => [0.1]) };
  const manifestFile = path.join(dir, 'docs.json');
  await fs.writeFile(manifestFile, JSON.stringify({ version: 1,
    documents: { 'old-doc': { pointIds: ['old-point'] } } }));
  const service = new KnowledgeService({ store, models, manifestFile, source: {
    listDocuments: async () => [{ token: 'old-doc', name: '旧资料' }, { token: 'new-doc', name: '新资料' }],
    readDocument: async id => { assert.equal(id, 'new-doc'); return '有效资料'; },
  } });
  assert.equal((await service.sync()).retiredDocuments, 1);
  assert.ok(removed.includes('old-point'));
  const mediaFile = path.join(dir, 'media.json');
  await fs.writeFile(mediaFile, JSON.stringify({ version: 1, records: {
    'old-image': { pointIds: ['old-media-point'] }, 'independent': { pointIds: ['keep-media'] },
  } }));
  const mediaResult = await syncMedia({ store, models, manifestFile: mediaFile,
    snapshot: { version: 1, createdAt: '2026-09-11T00:00:00Z', records: [{ id: 'old-image' }] } });
  assert.equal(mediaResult.retiredRecords, 1);
  assert.ok(removed.includes('old-media-point'));
  assert.ok(!removed.includes('keep-media'));
  const tables = Object.fromEntries(CATALOG_TABLES.map(name => [name, []]));
  tables.app_goods = [{ id: 57, goods_name: '旧商品资料' }, { id: 58, goods_name: '独立商品' }];
  const snapshot = { version: 1, source: 'production_mysql', tables,
    exportedAt: '2026-09-11T00:00:00Z', fingerprint: contentHash(JSON.stringify(tables)),
    schemas: Object.fromEntries(CATALOG_TABLES.map(name => [name, { primaryKey: 'id' }])) };
  const catalogResult = await syncCatalog({ store, models, snapshot, manifestFile: path.join(dir, 'catalog.json') });
  assert.equal(catalogResult.retiredDocuments, 1);
  assert.equal(catalogResult.documents, 1);
  assert.ok(indexed.some(p => p.payload.source_id === 'catalog:app_goods:58'));
  assert.ok(indexed.every(p => !['old-doc', 'catalog:app_goods:57', 'media:old-image'].includes(p.payload.source_id)));
});
