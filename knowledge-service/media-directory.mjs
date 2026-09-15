import { loadRetiredSources } from './retired-sources.mjs';

export async function mediaDirectoryPage(store, params) {
  const limit = Number(params.get('limit') || 200);
  const cursor = params.get('cursor') || undefined;
  const kind = params.get('kind');
  if (!Number.isInteger(limit) || limit < 1 || limit > 200 || (cursor && !/^[a-f0-9-]{36}$/.test(cursor))
    || (kind && !['image', 'video'].includes(kind))) throw Object.assign(new Error('invalid media pagination'), { status: 400 });
  const retired = [...await loadRetiredSources()];
  const filter = { must: [{ key: 'permission_scope', match: { value: 'internal' } },
    { key: 'media.kind', match: { any: kind ? [kind] : ['image', 'video'] } },
    { should: [{ is_empty: { key: 'product_status' } }, { key: 'product_status', match: { value: '1' } }, { key: 'product_status', match: { value: 1 } }] }],
    must_not: [{ key: 'orphan_relation', match: { value: true } },
      ...(retired.length ? [{ key: 'source_id', match: { any: retired } }] : [])] };
  const { result } = await store.api(`/collections/${encodeURIComponent(store.config.collection)}/points/scroll`, { method: 'POST',
    body: JSON.stringify({ limit, offset: cursor, with_payload: true, with_vector: false, filter }) });
  if (!Array.isArray(result?.points)) throw new Error('Invalid media directory response');
  const items = result.points.map(({ id, payload }) => ({ id, sourceId: payload.source_id, sourceType: payload.source_type,
    title: payload.title, content: payload.content, sourceUrl: payload.source_url, chunkIndex: payload.chunk_index,
    updatedAt: payload.source_updated_at, productStatus: payload.product_status, orphanRelation: payload.orphan_relation, media: payload.media, asset: payload.asset,
    tags: payload.media?.tags || payload.asset?.tags || [] }));
  const count = await store.api(`/collections/${encodeURIComponent(store.config.collection)}/points/count`, {
    method: 'POST', body: JSON.stringify({ filter, exact: true }) });
  if (!Number.isSafeInteger(count.result?.count)) throw new Error('Invalid media count');
  return { items, total: count.result.count, nextCursor: result.next_page_offset || null };
}
