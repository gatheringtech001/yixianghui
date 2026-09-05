import { createHash, timingSafeEqual } from "node:crypto";
import { AssetError, validateIdentity } from "./asset-schema.mjs";

const PREFIX = "/knowledge/assets/";
const digest = (value) => createHash("sha256").update(value).digest();
export function parseWriterTokens(value = "{}", forbidden = []) {
  let tokens;
  try { tokens = typeof value === "string" ? JSON.parse(value) : value; }
  catch { throw new Error("Invalid asset writer configuration"); }
  if (!tokens || typeof tokens !== "object" || Array.isArray(tokens)) throw new Error("Invalid asset writer configuration");
  for (const [source, token] of Object.entries(tokens)) {
    validateIdentity({ source, id: "writer-check" });
    if (typeof token !== "string" || !/^[A-Za-z0-9_-]{32,256}$/.test(token) || forbidden.includes(token)) throw new Error("Asset writer tokens must be independent secrets");
  }
  if (new Set(Object.values(tokens)).size !== Object.keys(tokens).length) throw new Error("Writer tokens cannot be shared between sources");
  return tokens;
}
function reply(response, status, body) {
  response.writeHead(status, { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" });
  response.end(JSON.stringify(body));
}
async function readJson(request) {
  if (!/^application\/json(?:\s*;|$)/i.test(request.headers["content-type"] ?? "")) throw new AssetError(415, "json_required", "只接受JSON索引，不接受文件上传");
  let bytes = 0; const chunks = [];
  for await (const chunk of request) {
    bytes += chunk.length;
    if (bytes > 32768) throw new AssetError(413, "body_too_large", "请求超过32 KiB");
    chunks.push(chunk);
  }
  try { return JSON.parse(Buffer.concat(chunks).toString("utf8")); }
  catch { throw new AssetError(400, "invalid_json", "请求JSON无效"); }
}
export function createAssetHandler(options) {
  const tokens = parseWriterTokens(options.tokens, options.forbiddenTokens);
  const windows = new Map(); let inFlight = 0;
  return async (request, response) => {
    if (!request.url?.startsWith(PREFIX)) return false;
    try {
      if (!["GET", "PUT"].includes(request.method)) throw new AssetError(405, "method_not_allowed", "只支持GET回读和PUT写入");
      const parts = request.url.slice(PREFIX.length).split("/");
      if (parts.length !== 2 || parts.some((part) => !part)) throw new AssetError(404, "not_found", "请指定source和素材ID");
      const identity = validateIdentity({ source: parts[0], id: parts[1] });
      const expected = Object.hasOwn(tokens, identity.source) ? tokens[identity.source] : undefined;
      const authorization = request.headers.authorization ?? "";
      if (!expected || authorization.length > 300 || !timingSafeEqual(digest(authorization), digest("Bearer " + expected))) throw new AssetError(401, "unauthorized", "写入令牌缺失、错误或不属于此来源");
      if (request.method === "GET") { reply(response, 200, await options.index.get(identity)); return true; }
      const window = windows.get(identity.source) ?? { start: Date.now(), count: 0 };
      if (Date.now() - window.start > 60000) { window.start = Date.now(); window.count = 0; }
      if (inFlight >= 4 || window.count >= 60) { response.setHeader("retry-after", "60"); throw new AssetError(429, "write_rate_limit", "写入繁忙，请稍后重试"); }
      window.count++; windows.set(identity.source, window); inFlight++;
      try {
        const result = await options.index.put(identity, await readJson(request));
        console.log(JSON.stringify({ event: "asset_index_write", source: identity.source, id: identity.id, status: result.status }));
        reply(response, result.status === "created" ? 201 : 200, result);
      } finally { inFlight--; }
    } catch (error) {
      reply(response, error instanceof AssetError ? error.status : 502,
        { error: error instanceof AssetError ? error.code : "index_dependency_failed", message: error instanceof AssetError ? error.message : "索引依赖服务失败，未确认写入；请使用相同ID和内容重试" });
    }
    return true;
  };
}
