import { loadRetiredSources } from './retired-sources.mjs';

export async function retrieveImages(service, body) {
  const { queries, tokens } = body || {};
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
    { key: 'media.kind', match: { value: 'image' } },
    { key: 'media.fileToken', match: { any: tokens } },
    { should: [{ is_empty: { key: 'product_status' } }, { key: 'product_status', match: { value: '1' } }, { key: 'product_status', match: { value: 1 } }] },
  ], must_not: [
    { key: 'orphan_relation', match: { value: true } },
    ...(retired.length ? [{ key: 'source_id', match: { any: retired } }] : []),
  ] };
  return { rankings: await Promise.all(vectors.map(async vector => {
    const { result } = await service.store.api('/collections/' + encodeURIComponent(service.store.config.collection) + '/points/query', {
      method: 'POST', body: JSON.stringify({ query: vector, using: 'dense', filter, limit: 1000, with_payload: ['media.fileToken'], params: { exact: true } }),
    });
    if (!Array.isArray(result?.points)) throw new Error('Invalid image retrieval result');
    return [...new Set(result.points.map(p => p.payload?.media?.fileToken).filter(t => tokens.includes(t)))];
  })) };
}
