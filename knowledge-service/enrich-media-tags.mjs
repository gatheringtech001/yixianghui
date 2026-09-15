import fs from 'node:fs/promises';
import { join, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';
import { gzipSync } from 'node:zlib';
import { contentHash, sparseVector } from './lib.mjs';
import { AzureModels, QdrantStore } from './service.mjs';
import { LunaReranker } from './luna.mjs';
import { loadRetiredSources } from './retired-sources.mjs';
import { TAG_DIRECTORY, enrichPayload, readVisualTagRecord, taggingRequest, validateVisualTags, visualEvidence } from './visual-tags.mjs';

const canonical = value => JSON.stringify(value, (_, item) => item && typeof item === 'object' && !Array.isArray(item)
  ? Object.fromEntries(Object.entries(item).sort(([a], [b]) => a.localeCompare(b))) : item);
const fingerprint = value => contentHash(canonical(value));
async function save(file, value) {
  await fs.writeFile(`${file}.next`, JSON.stringify(value, null, 2), { mode: 0o600 });
  await fs.rename(`${file}.next`, file);
}

export async function mediaInventory(store) {
  const retired = await loadRetiredSources();
  const points = []; let offset;
  do {
    const { result } = await store.api(`/collections/${store.config.collection}/points/scroll`, { method: 'POST',
      body: JSON.stringify({ limit: 256, offset, with_payload: true, with_vector: false,
        filter: { must: [{ key: 'source_type', match: { value: 'yuque_media' } }, { key: 'media.kind', match: { any: ['video', 'image'] } }] } }),
    });
    points.push(...result.points.filter(point => !retired.has(point.payload.source_id)));
    offset = result.next_page_offset;
  } while (offset);
  return points;
}

export async function prepareTagRecords(points, labeler) {
  const result = await labeler.complete(taggingRequest(points, labeler.config.model), 'visual_tagging');
  const items = result.output.items;
  if (!Array.isArray(items) || items.length !== points.length || new Set(items.map(item => item.id)).size !== points.length
    || items.some(item => !points.some(point => point.id === item.id))) throw new Error(`Incomplete tagging response: expected ${points.length}, received ${items?.length}`);
  return points.map(point => {
    const evidence = visualEvidence(point.payload.visual_tagging?.baseContent ?? point.payload.content);
    const proposed = items.find(item => item.id === point.id).tags;
    if (!Array.isArray(proposed) || proposed.length > 80) throw new Error('Invalid tag count');
    const tags = [], rejected = [];
    for (const tag of proposed) {
      const index = typeof tag?.name === 'string' ? evidence.findIndex(text => text.includes(tag.name.trim())) : -1;
      try { tags.push(...validateVisualTags([{ ...tag, evidence: index }], evidence)); }
      catch (error) { rejected.push({ name: tag?.name, reason: error.message }); }
    }
    if (!tags.length) throw new Error(`No grounded tags for ${point.id}`);
    return { id: point.id, version: 1, promptVersion: 2, sourceHash: contentHash(point.payload.visual_tagging?.baseContent ?? point.payload.content),
      tags: validateVisualTags(tags, evidence), rejected, evidence, annotatedAt: new Date().toISOString(),
      model: result.model || labeler.config.model, responseId: result.id || '' };
  });
}

export async function applyTagRecords({ points, records, store, models, backupDirectory }) {
  const ids = points.map(point => point.id);
  const route = `/collections/${store.config.collection}/points`;
  const current = (await store.api(route, { method: 'POST', body: JSON.stringify({ ids, with_payload: true, with_vector: true }) })).result;
  const next = [];
  for (const point of points) {
    const original = current.find(item => item.id === point.id);
    if (!original || fingerprint(original.payload) !== fingerprint(point.payload)) throw new Error(`Concurrent source change: ${point.id}`);
    const file = join(backupDirectory, `${point.id}.json.gz`);
    try { await fs.writeFile(file, gzipSync(JSON.stringify(original)), { flag: 'wx', mode: 0o600 }); }
    catch (error) { if (error.code !== 'EEXIST') throw error; }
    next.push({ id: point.id, payload: enrichPayload(original.payload, records.find(record => record.id === point.id)) });
  }
  const vectors = await models.embed(next.map(point => point.payload.content));
  if (vectors.length !== next.length || vectors.some(vector => !Array.isArray(vector) || vector.length !== store.config.dimensions
    || vector.some(value => !Number.isFinite(value)) || vector.every(value => value === 0))) throw new Error('Invalid enrichment embeddings');
  const checked = (await store.api(route, { method: 'POST', body: JSON.stringify({ ids, with_payload: true, with_vector: false }) })).result;
  if (points.some(point => fingerprint(checked.find(item => item.id === point.id)?.payload) !== fingerprint(point.payload))) throw new Error('Source changed during embedding');
  await store.upsert(next.map((point, i) => ({ ...point, vector: { dense: vectors[i], lexical: sparseVector(point.payload.content) } })));
  const readback = (await store.api(route, { method: 'POST', body: JSON.stringify({ ids, with_payload: true, with_vector: false }) })).result;
  if (next.some(point => fingerprint(readback.find(item => item.id === point.id)?.payload) !== fingerprint(point.payload))) throw new Error('Enrichment readback mismatch');
  return next.map(point => ({ id: point.id, tags: point.payload.media.tags.length, verified: true }));
}

async function main() {
  const args = process.argv.slice(2); const apply = args.includes('--apply');
  const limit = Number(args.find(arg => arg.startsWith('--limit='))?.split('=')[1] ?? 12);
  if (!Number.isInteger(limit) || limit < 0) throw new Error('Invalid limit');
  const directory = resolve(args.find(arg => arg.startsWith('--directory='))?.slice(12) || TAG_DIRECTORY);
  const backups = join(directory, 'backups'); await fs.mkdir(backups, { recursive: true, mode: 0o700 });
  const store = new QdrantStore({ url: process.env.QDRANT_URL || 'http://127.0.0.1:6333', apiKey: process.env.QDRANT_API_KEY,
    collection: process.env.QDRANT_COLLECTION || 'yixianghui_travel_kb', dimensions: Number(process.env.EMBEDDING_DIMENSIONS || 3072) });
  const models = new AzureModels({ baseUrl: process.env.AZURE_OPENAI_BASE_URL, apiKey: process.env.AZURE_OPENAI_KEY,
    apiVersion: process.env.AZURE_OPENAI_API_VERSION, embeddingModel: process.env.AZURE_EMBEDDING_MODEL || 'text-embedding-3-large' });
  const labeler = new LunaReranker({ url: process.env.LUNA_RERANK_URL, apiKey: process.env.LUNA_RERANK_KEY, model: process.env.LUNA_RERANK_MODEL });
  const all = await mediaInventory(store);
  const report = { startedAt: new Date().toISOString(), apply, total: all.length, existing: 0, noEvidence: [], prepared: [], written: [], failed: [] };
  const eligible = all.filter(point => {
    if (point.payload.visual_tagging?.version === 1) { report.existing++; return false; }
    if (!visualEvidence(point.payload.content).length) { report.noEvidence.push({ id: point.id, kind: point.payload.media.kind, title: point.payload.title }); return false; }
    return true;
  }).sort((a, b) => Number(b.payload.media.kind === 'video') - Number(a.payload.media.kind === 'video'));
  const selected = limit ? eligible.slice(0, limit) : eligible;
  report.selected = selected.length; await save(join(directory, 'run-report.json'), report);
  let cursor = 0, failures = 0;
  let saving = Promise.resolve();
  const checkpoint = () => { const snapshot = structuredClone(report); saving = saving.then(() => save(join(directory, 'run-report.json'), snapshot)); return saving; };
  const worker = async () => {
    while (cursor < selected.length && failures < 3) {
      const batch = selected.slice(cursor, cursor + 6); cursor += batch.length;
      try {
        const records = [], pending = [];
        for (const point of batch) {
          const saved = await readVisualTagRecord(point.id, directory);
          if (saved?.promptVersion === 2 && saved.sourceHash === contentHash(point.payload.content)) records.push(saved); else pending.push(point);
        }
        if (pending.length) records.push(...await prepareTagRecords(pending, labeler));
        for (const record of records) { await save(join(directory, `${record.id}.json`), record); report.prepared.push({ id: record.id, tags: record.tags.length, rejected: record.rejected.length }); }
        if (apply) report.written.push(...await applyTagRecords({ points: batch, records, store, models, backupDirectory: backups }));
        failures = 0;
      } catch (error) {
        failures++; report.failed.push({ ids: batch.map(point => point.id), error: String(error.message).slice(0, 400) });
      }
      await checkpoint();
      console.log(JSON.stringify({ prepared: report.prepared.length, written: report.written.length, failedBatches: report.failed.length, total: selected.length }));
    }
  };
  await Promise.all(Array.from({ length: 4 }, worker));
  report.finishedAt = new Date().toISOString(); report.unprocessed = selected.length - report.written.length - (apply ? 0 : report.prepared.length);
  await save(join(directory, 'run-report.json'), report);
  console.log(JSON.stringify({ ...report, noEvidence: report.noEvidence.length, prepared: report.prepared.length, written: report.written.length }));
  if (report.failed.length || report.unprocessed) process.exitCode = 1;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) main().catch(error => { console.error(error.message); process.exitCode = 1; });
