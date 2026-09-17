import fs from 'node:fs/promises';
import { join } from 'node:path';
import { gzipSync } from 'node:zlib';
import { isDeepStrictEqual } from 'node:util';
import { pathToFileURL } from 'node:url';
import { prepareImageIndex, currentImageIndex } from './image-index.mjs';
import { mediaInventory } from './enrich-media-tags.mjs';
import { applyMediaUsagePolicy } from './media-usage-policy.mjs';
import { sparseVector } from './lib.mjs';
import { QdrantStore, AzureModels, FeishuSource } from './service.mjs';
import { LunaReranker } from './luna.mjs';
import { setTimeout as wait } from 'node:timers/promises';

export async function backfillImages({ store, models, points, indexer, directory, apply = false, concurrency = 1, delay = wait }) {
  if (!Number.isInteger(concurrency) || concurrency < 1 || concurrency > 6) throw new Error('Invalid backfill concurrency');
  const pending = points.filter(p => p.payload.media?.kind === 'image' && !currentImageIndex(p.payload));
  const report = { total: points.length, pending: pending.length, written: [], failed: [] };
  if (!apply) return report;
  await fs.mkdir(directory, { recursive: true, mode: 0o700 });
  const route = '/collections/' + encodeURIComponent(store.config.collection) + '/points';
  for (let offset = 0; offset < pending.length; offset += concurrency) {
    await Promise.all(pending.slice(offset, offset + concurrency).map(async point => {
    try {
      if (!/^[A-Za-z0-9_-]+$/.test(String(point.id))) throw new Error('Invalid image point identity');
      let prepared;
      for (let attempt = 0; ; attempt++) {
        try { prepared = await indexer(point); break; }
        catch (error) {
          const seconds = Number(error.retryAfter || 60);
          if (error.statusCode !== 429 || attempt >= 2 || !Number.isFinite(seconds) || seconds < 1 || seconds > 120) throw error;
          // Offline maintenance only; honor the provider cooldown without
          // turning rate limits into repeated immediate requests.
          await delay(seconds * 1000);
        }
      }
      if (!currentImageIndex(prepared.payload)) throw new Error('Image ingestion returned no current index');
      prepared.payload = applyMediaUsagePolicy(prepared.payload);
      const [dense] = await models.embed([prepared.payload.media.imageIndex.description]);
      if (!Array.isArray(dense) || dense.length !== store.config.dimensions || !dense.every(Number.isFinite) || !dense.some(v => v !== 0)) throw new Error('Invalid image embedding');
      const current = (await store.api(route, { method: 'POST', body: JSON.stringify({ ids: [point.id], with_payload: true, with_vector: true }) })).result?.[0];
      if (!current || !isDeepStrictEqual(current.payload, point.payload)) throw new Error('Concurrent image source change');
      await fs.writeFile(join(directory, point.id + '.json.gz'), gzipSync(JSON.stringify(current)), { flag: 'wx', mode: 0o600 });
      await store.upsert([{ id: point.id, payload: prepared.payload, vector: { dense, lexical: sparseVector(prepared.payload.media.imageIndex.description) } }]);
      const saved = (await store.api(route, { method: 'POST', body: JSON.stringify({ ids: [point.id], with_payload: true, with_vector: true }) })).result?.[0];
      const actual = saved?.vector?.dense;
      // This collection uses Cosine: Qdrant normalizes dense vectors on write.
      const norm = Math.hypot(...dense);
      if (!isDeepStrictEqual(saved?.payload, prepared.payload) || !Array.isArray(actual) || actual.length !== dense.length
        || actual.some((v, i) => !Number.isFinite(v) || Math.abs(v - dense[i] / norm) > 1e-7)) throw new Error('Image index readback mismatch');
      report.written.push(point.id);
    } catch (error) { report.failed.push({ id: point.id, reason: error.message }); }
    }));
    await fs.writeFile(join(directory, 'report.json'), JSON.stringify(report), { mode: 0o600 });
    if (report.failed.length >= 3) break;
  }
  return report;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const store = new QdrantStore({ url: process.env.QDRANT_URL || 'http://127.0.0.1:6333', apiKey: process.env.QDRANT_API_KEY,
    collection: process.env.QDRANT_COLLECTION || 'yixianghui_travel_kb', dimensions: Number(process.env.EMBEDDING_DIMENSIONS || 3072) });
  const models = new AzureModels({ baseUrl: process.env.AZURE_OPENAI_BASE_URL, apiKey: process.env.AZURE_OPENAI_KEY,
    apiVersion: process.env.AZURE_OPENAI_API_VERSION, embeddingModel: process.env.AZURE_EMBEDDING_MODEL || 'text-embedding-3-large' });
  const source = new FeishuSource({ appId: process.env.FEISHU_APP_ID, appSecret: process.env.FEISHU_APP_SECRET });
  const labeler = new LunaReranker({ url: process.env.LUNA_RERANK_URL, apiKey: process.env.LUNA_RERANK_KEY, model: process.env.LUNA_RERANK_MODEL });
  const points = await mediaInventory(store);
  const report = await backfillImages({ store, models, points, apply: process.argv.includes('--apply'),
    concurrency: 1,
    indexer: point => prepareImageIndex(point, { source, labeler }),
    directory: '/var/lib/yixianghui-knowledge/image-index/runs/' + new Date().toISOString().replaceAll(':', '-'),
  });
  console.log(JSON.stringify(report));
  if (report.failed.length || (process.argv.includes('--apply') && report.written.length !== report.pending)) process.exitCode = 1;
}
