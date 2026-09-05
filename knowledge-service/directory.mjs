import { readFile } from 'node:fs/promises';
import { createHash } from 'node:crypto';

export function directoryPage(manifest, params) {
  if (!manifest?.documents || typeof manifest.documents !== 'object' || Array.isArray(manifest.documents)) throw new Error('Knowledge manifest unavailable');
  const items = Object.entries(manifest.documents).map(([id, doc]) => ({
    id, title: String(doc.title || '').trim(), sourceId: id, sourceType: 'feishu_docx',
    sourceUrl: `https://vcnnjnb870d6.feishu.cn/docx/${encodeURIComponent(id)}`,
    updatedAt: String(doc.modifiedTime || ''),
  })).filter(item => item.title).sort((a, b) => a.id.localeCompare(b.id, 'en'));
  const snapshot = createHash('sha256').update(JSON.stringify(items)).digest('hex');
  const limit = Number(params.get('limit') || 100);
  if (!Number.isInteger(limit) || limit < 1 || limit > 200) throw Object.assign(new Error('limit must be 1–200'), { status: 400 });
  let offset = 0;
  if (params.get('cursor')) {
    let cursor;
    try { cursor = JSON.parse(Buffer.from(params.get('cursor'), 'base64url').toString()); }
    catch { throw Object.assign(new Error('invalid directory cursor'), { status: 400 }); }
    if (cursor.snapshot !== snapshot) throw Object.assign(new Error('directory changed; restart pagination'), { status: 409 });
    if (!Number.isSafeInteger(cursor.offset) || cursor.offset < 0 || cursor.offset > items.length) throw Object.assign(new Error('invalid directory offset'), { status: 400 });
    offset = cursor.offset;
  }
  const next = offset + limit;
  return { items: items.slice(offset, next), total: items.length, snapshot,
    nextCursor: next < items.length ? Buffer.from(JSON.stringify({ snapshot, offset: next })).toString('base64url') : null };
}

export function createDirectoryHandler({ token, manifestFile, load = async () => JSON.parse(await readFile(manifestFile, 'utf8')) }) {
  return async (request, response) => {
    const url = new URL(request.url, 'http://knowledge.local');
    if (url.pathname !== '/documents') return false;
    const reply = (status, body) => { response.writeHead(status, { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store' }); response.end(JSON.stringify(body)); };
    if (request.headers.authorization !== `Bearer ${token}`) { reply(401, { error: 'unauthorized' }); return true; }
    if (request.method !== 'GET') { reply(405, { error: 'read_only' }); return true; }
    try { reply(200, directoryPage(await load(), url.searchParams)); }
    catch (error) { reply(error.status || 503, { error: error.status ? error.message : 'knowledge_directory_unavailable' }); }
    return true;
  };
}
