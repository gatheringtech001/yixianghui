import fs from 'node:fs/promises';
import { join } from 'node:path';
import { gzipSync } from 'node:zlib';
import { isDeepStrictEqual } from 'node:util';
import { pathToFileURL } from 'node:url';
import { prepareImageIndex, currentImageIndex, needsImageIndex } from './image-index.mjs';
import { mediaInventory } from './enrich-media-tags.mjs';
import { applyMediaUsagePolicy } from './media-usage-policy.mjs';
import { sparseVector } from './lib.mjs';
import { QdrantStore, AzureModels, FeishuSource } from './service.mjs';
import { LunaReranker } from './luna.mjs';
import { setTimeout as wait } from 'node:timers/promises';
import { currentVideoIndex } from './video-index.mjs';

export async function backfillImages({ store, models, points, indexer, directory, apply = false, concurrency = 1, delay = wait, kind = 'image' }) {
  if (!['image', 'video'].includes(kind)) throw new Error('Invalid backfill kind');
  const currentIndex = kind === 'video' ? currentVideoIndex : currentImageIndex;
  if (!Number.isInteger(concurrency) || concurrency < 1 || concurrency > 6) throw new Error('Invalid backfill concurrency');
  const pending = points.filter(p => p.payload.media?.kind === kind && (kind === 'image' ? needsImageIndex(p.payload) : !currentIndex(p.payload)));
  const report = { total: points.length, pending: pending.length, written: [], failed: [], startedAt: new Date().toISOString() };
  if (!apply) return report;
  await fs.mkdir(directory, { recursive: true, mode: 0o700 });
  const route = '/collections/' + encodeURIComponent(store.config.collection) + '/points';
  let consecutiveFailures = 0, unsafe = false;
  for (let offset = 0; offset < pending.length; offset += concurrency) {
    await Promise.all(pending.slice(offset, offset + concurrency).map(async point => {
    try {
      if (!/^[A-Za-z0-9_-]+$/.test(String(point.id))) throw new Error('Invalid image point identity');
      let prepared;
      for (let attempt = 0; ; attempt++) {
        try { prepared = await indexer(point); break; }
        catch (error) {
          const seconds = Number(error.retryAfter || 60);
          const status = error.statusCode ?? error.status;
          if (![429, 502, 503, 504].includes(status) || attempt >= 2 || !Number.isFinite(seconds) || seconds < 1 || seconds > 120) throw error;
          // Offline maintenance only; honor the provider cooldown without
          // turning rate limits into repeated immediate requests.
          await delay(status === 429 ? seconds * 1000 : 5000 * (attempt + 1));
        }
      }
      const index = currentIndex(prepared.payload);
      if (!index) throw new Error('Media ingestion returned no current index');
      prepared.payload = applyMediaUsagePolicy(prepared.payload, prepared.fileUsage);
      const [dense] = await models.embed([index.description]);
      if (!Array.isArray(dense) || dense.length !== store.config.dimensions || !dense.every(Number.isFinite) || !dense.some(v => v !== 0)) throw new Error('Invalid image embedding');
      const current = (await store.api(route, { method: 'POST', body: JSON.stringify({ ids: [point.id], with_payload: true, with_vector: true }) })).result?.[0];
      if (!current || !isDeepStrictEqual(current.payload, point.payload)) throw new Error('Concurrent image source change');
      await fs.writeFile(join(directory, point.id + '.json.gz'), gzipSync(JSON.stringify(current)), { flag: 'wx', mode: 0o600 });
      await store.upsert([{ id: point.id, payload: prepared.payload, vector: { dense, lexical: sparseVector(index.description) } }]);
      const saved = (await store.api(route, { method: 'POST', body: JSON.stringify({ ids: [point.id], with_payload: true, with_vector: true }) })).result?.[0];
      const actual = saved?.vector?.dense;
      // This collection uses Cosine: Qdrant normalizes dense vectors on write.
      const norm = Math.hypot(...dense);
      if (!isDeepStrictEqual(saved?.payload, prepared.payload) || !Array.isArray(actual) || actual.length !== dense.length
        || actual.some((v, i) => !Number.isFinite(v) || Math.abs(v - dense[i] / norm) > 1e-7)) throw new Error('Image index readback mismatch');
      report.written.push(point.id);
      consecutiveFailures = 0;
    } catch (error) {
      report.failed.push({ id: point.id, reason: error.message }); consecutiveFailures++;
      // Never continue after a source race or unverifiable write.
      if (/Concurrent|readback mismatch|Invalid image embedding/.test(error.message)) unsafe = true;
    }
    }));
    report.updatedAt = new Date().toISOString();
    report.stoppedReason = unsafe ? 'write-safety-check' : consecutiveFailures >= 10 ? '10-consecutive-failures' : null;
    await fs.writeFile(join(directory, 'report.json.next'), JSON.stringify(report), { mode: 0o600 });
    await fs.rename(join(directory, 'report.json.next'), join(directory, 'report.json'));
    if (report.stoppedReason) break;
  }
  report.finishedAt = new Date().toISOString();
  await fs.writeFile(join(directory, 'report.json'), JSON.stringify(report), { mode: 0o600 });
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
