import { loadRetiredSources } from './retired-sources.mjs';
import { sparseVector } from './lib.mjs';

export async function retrieveImages(service, body) {
  const { queries, tokens } = body || {};
  const mixed = body?.mode === 'media';
  if (!Array.isArray(queries) || !queries.length || queries.length > 40
    || queries.some(q => typeof q !== 'string' || !q.trim() || q.length > 2000)
    || !Array.isArray(tokens) || !tokens.length || tokens.length > 500
    || tokens.some(t => typeof t !== 'string' || !/^[A-Za-z0-9_-]{8,256}$/.test(t))) {
    throw Object.assign(new Error('Invalid image retrieval scope'), { status: 400 });
  }
  const retired = [...await loadRetiredSources()];
  const vectors = await service.models.embed(queries);
  if (vectors.length !== queries.length) throw new Error('Invalid query embedding count');
  const filter = { must: [
    { key: 'permission_scope', match: { value: 'internal' } },
    { key: 'media.kind', match: mixed ? { any: ['image', 'video'] } : { value: 'image' } },
    { key: 'media.fileToken', match: { any: tokens } },
    { should: [{ is_empty: { key: 'product_status' } }, { key: 'product_status', match: { value: '1' } }, { key: 'product_status', match: { value: 1 } }] },
  ], must_not: [
    { key: 'orphan_relation', match: { value: true } },
    ...(retired.length ? [{ key: 'source_id', match: { any: retired } }] : []),
  ] };
  return { rankings: await Promise.all(vectors.map(async (vector, index) => {
    const { result } = await service.store.api('/collections/' + encodeURIComponent(service.store.config.collection) + '/points/query', {
      method: 'POST', body: JSON.stringify(mixed ? {
        prefetch: [
          { query: vector, using: 'dense', filter, limit: 1000, params: { exact: true } },
          { query: sparseVector(queries[index]), using: 'lexical', filter, limit: 1000 },
        ], query: { fusion: 'rrf' }, limit: 1000,
        with_payload: ['media.kind', 'media.fileToken', 'media.startSeconds', 'media.endSeconds', 'source_id'],
      } : { query: vector, using: 'dense', filter, limit: 1000, with_payload: ['media.fileToken'], params: { exact: true } }),
    });
    if (!Array.isArray(result?.points)) throw new Error('Invalid image retrieval result');
    if (mixed) return result.points.filter(p => tokens.includes(p.payload?.media?.fileToken)).map(p => ({
      pointId: String(p.id), sourceId: p.payload.source_id, kind: p.payload.media.kind,
      token: p.payload.media.fileToken, startSeconds: p.payload.media.startSeconds, endSeconds: p.payload.media.endSeconds,
    }));
    return [...new Set(result.points.map(p => p.payload?.media?.fileToken).filter(t => tokens.includes(t)))];
  })) };
}
