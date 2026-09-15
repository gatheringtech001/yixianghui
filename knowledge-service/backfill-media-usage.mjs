import fs from 'node:fs/promises';
import { join } from 'node:path';
import { gzipSync } from 'node:zlib';
import { isDeepStrictEqual } from 'node:util';
import { QdrantStore } from './service.mjs';
import { loadRetiredSources } from './retired-sources.mjs';
import { applyFileUsagePolicies, readUsageReview, usageSourceHash } from './media-usage-policy.mjs';

const directory = '/var/lib/yixianghui-knowledge/media-usage-v1';
const store = new QdrantStore({ url: process.env.QDRANT_URL || 'http://127.0.0.1:6333', apiKey: process.env.QDRANT_API_KEY,
  collection: process.env.QDRANT_COLLECTION || 'yixianghui_travel_kb' });
const route = `/collections/${encodeURIComponent(store.config.collection)}/points`;
const apply = process.argv.includes('--apply');
const preserveTextBans = process.argv.includes('--preserve-text-bans');
const runDirectory = join(directory, 'runs', new Date().toISOString().replace(/[:.]/g, '-') + (apply ? '-apply' : '-plan'));
await fs.mkdir(runDirectory, { recursive: true, mode: 0o700 });
const retired = await loadRetiredSources();
const points = []; let offset;
do {
  const { result } = await store.api(route + '/scroll', { method: 'POST', body: JSON.stringify({ limit: 128, offset,
    with_payload: true, with_vector: false, filter: { must: [
      { key: 'permission_scope', match: { value: 'internal' } },
      { key: 'source_type', match: { any: ['yuque_media', 'external_asset'] } },
      { key: 'media.kind', match: { any: ['image', 'video'] } },
    ] } }) });
  points.push(...result.points.filter(point => !retired.has(point.payload.source_id))); offset = result.next_page_offset;
} while (offset);
const reviews = new Map(await Promise.all(points.map(async point => [point.id, await readUsageReview(point)])));
const next = applyFileUsagePolicies(points, reviews);
if (preserveTextBans) for (const point of next) {
  const original = points.find(item => item.id === point.id), review = reviews.get(point.id);
  if (![2, 3].includes(review?.scopePolicyVersion) || review.sourceHash !== usageSourceHash(original.payload)) throw Error(`Missing current scope review: ${point.id}`);
  if ((point.payload.media.usage.hasVisibleText !== original.payload.media.usage?.hasVisibleText
    || point.payload.media.usage.usable !== original.payload.media.usage?.usable)
    && point.payload.media.usage.hasVisibleText !== true) throw Error(`Review weakened text status: ${point.id}`);
}
const changes = next.filter(point => !isDeepStrictEqual(point.payload.media, points.find(item => item.id === point.id).payload.media));
const report = { apply, preserveTextBans, runDirectory, startedAt: new Date().toISOString(), total: next.length, changed: changes.length, written: 0,
  exclusive: next.filter(point => point.payload.media.usage.exclusive).length,
  generic: next.filter(point => !point.payload.media.usage.exclusive).length,
  unusable: next.filter(point => point.payload.media.usage.usable === false).length,
  noText: next.filter(point => point.payload.media.usage.hasVisibleText === false).length,
  pendingTextReview: next.filter(point => point.payload.media.usage.hasVisibleText === null).length,
  sample: next.filter(point => !point.payload.media.usage.exclusive).slice(0, 15).map(point => ({ id: point.id, title: point.payload.title,
    evidence: point.payload.visual_tagging?.baseContent || point.payload.content, usage: point.payload.media.usage })),
};
const reportFile = join(runDirectory, apply ? 'apply-report.json' : 'plan.json');
await fs.writeFile(reportFile, JSON.stringify(report, null, 2), { mode: 0o600 });
for (let start = 0; apply && start < changes.length; start += 64) {
  const batch = changes.slice(start, start + 64);
  const current = (await store.api(route, { method: 'POST', body: JSON.stringify({ ids: batch.map(point => point.id), with_payload: true, with_vector: true }) })).result;
  for (const point of batch) {
    const original = points.find(item => item.id === point.id), stored = current.find(item => item.id === point.id);
    if (!stored || !isDeepStrictEqual(stored.payload, original.payload)) throw Error(`Concurrent source change: ${point.id}`);
    await fs.writeFile(join(runDirectory, `${point.id}.json.gz`), gzipSync(JSON.stringify(stored)), { mode: 0o600, flag: 'wx' });
    await store.api(route + '/payload?wait=true', { method: 'POST', body: JSON.stringify({ points: [point.id], payload: { media: point.payload.media } }) });
  }
  const readback = (await store.api(route, { method: 'POST', body: JSON.stringify({ ids: batch.map(point => point.id), with_payload: true, with_vector: false }) })).result;
  for (const point of batch) if (!isDeepStrictEqual(readback.find(item => item.id === point.id)?.payload, point.payload)) throw Error(`Readback mismatch: ${point.id}`);
  report.written += batch.length;
  await fs.writeFile(reportFile, JSON.stringify(report, null, 2), { mode: 0o600 });
}
report.finishedAt = new Date().toISOString();
await fs.writeFile(reportFile, JSON.stringify(report, null, 2), { mode: 0o600 });
console.log(JSON.stringify(report, null, 2));
