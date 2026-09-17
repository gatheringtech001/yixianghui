import { pathToFileURL } from 'node:url';
import { backfillImages } from './backfill-image-index.mjs';
import { prepareVideoIndex, releaseVideoSource } from './video-index.mjs';
import { mediaInventory } from './enrich-media-tags.mjs';
import { QdrantStore, AzureModels, FeishuSource } from './service.mjs';
import { LunaReranker } from './luna.mjs';
import { applyFileUsagePolicies } from './media-usage-policy.mjs';

export const backfillVideos = options => {
  // Each source is prepared once per maintenance run, not once per sibling.
  const files = new Map();
  const groups = new Map();
  for (const point of options.points) {
    const key = point.payload.media?.fileToken || point.id;
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(point);
  }
  return backfillImages({ ...options, kind: 'video', concurrency: 1,
  indexer: async point => {
    const key = point.payload.media?.fileToken || point.id;
    if (!files.has(key)) files.set(key, (async () => {
      const prepared = [];
      for (const sibling of groups.get(key)) prepared.push(await options.indexer(sibling));
      if (options.releaseFile) await options.releaseFile(point.payload);
      return new Map(applyFileUsagePolicies(prepared).map(item => [item.id, item]));
    })().catch(error => {
      // Allow bounded provider retries; deterministic corrupt sources are not retried for every sibling.
      if ([429, 502, 503, 504].includes(error.statusCode ?? error.status)) files.delete(key);
      throw error;
    }));
    const next = (await files.get(key)).get(point.id);
    // Preserve file-level contamination in the shared writer's policy pass.
    return { ...next, fileUsage: next.payload.media.usage };
  },
  });
};

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const store = new QdrantStore({ url: process.env.QDRANT_URL || 'http://127.0.0.1:6333', apiKey: process.env.QDRANT_API_KEY,
    collection: process.env.QDRANT_COLLECTION || 'yixianghui_travel_kb', dimensions: Number(process.env.EMBEDDING_DIMENSIONS || 3072) });
  const models = new AzureModels({ baseUrl: process.env.AZURE_OPENAI_BASE_URL, apiKey: process.env.AZURE_OPENAI_KEY,
    apiVersion: process.env.AZURE_OPENAI_API_VERSION, embeddingModel: process.env.AZURE_EMBEDDING_MODEL || 'text-embedding-3-large' });
  const source = new FeishuSource({ appId: process.env.FEISHU_APP_ID, appSecret: process.env.FEISHU_APP_SECRET });
  const labeler = new LunaReranker({ url: process.env.LUNA_RERANK_URL, apiKey: process.env.LUNA_RERANK_KEY, model: process.env.LUNA_RERANK_MODEL });
  const limitArg = process.argv.find(arg => arg.startsWith('--limit='));
  const limit = limitArg ? Number(limitArg.slice(8)) : Infinity;
  if (limitArg && (!Number.isInteger(limit) || limit < 1)) throw new Error('Invalid limit');
  const points = (await mediaInventory(store)).filter(p => p.payload.media?.kind === 'video');
  const report = await backfillVideos({ store, models, points: points.slice(0, limit), apply: process.argv.includes('--apply'),
    indexer: point => prepareVideoIndex(point, { source, labeler }),
    releaseFile: payload => releaseVideoSource(payload),
    directory: '/var/lib/yixianghui-knowledge/video-index/runs/' + new Date().toISOString().replaceAll(':', '-'),
  });
  console.log(JSON.stringify(report));
  if (report.failed.length || (process.argv.includes('--apply') && report.written.length !== report.pending)) process.exitCode = 1;
}
