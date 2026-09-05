import { createHash, randomBytes, timingSafeEqual } from "node:crypto";
import { readFile } from "node:fs/promises";
import { mediaDescriptor, streamPreview } from "./preview.mjs";
import { publicMediaUrl } from "./asset-schema.mjs";

const PREFIX = "/knowledge/console/";
const COOKIE = "yxh_kb_session";
const SESSION_MS = 8 * 60 * 60 * 1000;
const WINDOW_MS = 60 * 1000;
const STATIC = new Map([["", ["index.html", "text/html"]], ["app.js", ["app.js", "text/javascript"]], ["style.css", ["style.css", "text/css"]]]);
const digest = (value) => createHash("sha256").update(value).digest();

function send(response, status, body) {
  response.writeHead(status, { "content-type": "application/json; charset=utf-8" });
  response.end(JSON.stringify(body));
}

function securityHeaders(response) {
  response.setHeader("cache-control", "no-store");
  response.setHeader("x-content-type-options", "nosniff");
  response.setHeader("referrer-policy", "no-referrer");
  response.setHeader("content-security-policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' https:; media-src 'self' https:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
}

async function readJson(request) {
  let size = 0;
  const chunks = [];
  for await (const chunk of request) {
    size += chunk.length;
    if (size > 8192) throw Object.assign(new Error("请求过大"), { status: 413 });
    chunks.push(chunk);
  }
  try { return JSON.parse(Buffer.concat(chunks).toString("utf8")); }
  catch { throw Object.assign(new Error("请求不是有效的 JSON"), { status: 400 }); }
}

function validateQuery(body) {
  if (!body || typeof body !== "object" || Array.isArray(body) || !["ask", "search"].includes(body.mode)
    || typeof body.question !== "string" || body.question.trim().length < 2 || body.question.trim().length > 500
    || !Number.isInteger(body.limit) || body.limit < 1 || body.limit > 10) {
    throw Object.assign(new Error("问题应为 2–500 字，来源数量应为 1–10"), { status: 400 });
  }
  return { mode: body.mode, question: body.question.trim(), limit: body.limit };
}

export function createConsoleHandler(options) {
  const console = new ConsoleGateway(options);
  return console.handle.bind(console);
}

class ConsoleGateway {
  constructor(options) {
    this.options = options;
    this.origin = options.origin ?? "https://gatheringtech.com";
    this.secure = new URL(this.origin).protocol === "https:" ? "; Secure" : "";
    this.sessions = new Map();
    this.attempts = new Map();
    this.running = 0;
    this.mediaStreams = 0;
    this.queryWindow = { start: 0, count: 0 };
  }
  cookie(id, age) { return `${COOKIE}=${id}; Path=${PREFIX}; Max-Age=${age}; HttpOnly; SameSite=Strict${this.secure}`; }
  sessionId(request) { return request.headers.cookie?.match(/(?:^|;\s*)yxh_kb_session=([A-Za-z0-9_-]{43})(?:;|$)/)?.[1]; }
  prune() {
    const now = Date.now();
    for (const [id, session] of this.sessions) if (session.expires <= now) this.sessions.delete(id);
    for (const [ip, entry] of this.attempts) if (entry.until <= now) this.attempts.delete(ip);
  }
  login(request, response, body) {
    const ip = String(request.headers["x-real-ip"] ?? request.socket.remoteAddress).slice(0, 80);
    const entry = this.attempts.get(ip) ?? { until: Date.now() + 10 * WINDOW_MS, count: 0 };
    if (entry.count >= 5 || this.attempts.size >= 2000) {
      response.setHeader("retry-after", "600");
      return send(response, 429, { error: "登录尝试过多，请稍后重试" });
    }
    const token = typeof body?.token === "string" ? body.token.trim() : "";
    if (!timingSafeEqual(digest(token), digest(this.options.token))) {
      entry.count++; this.attempts.set(ip, entry);
      return send(response, 401, { error: "查询令牌不正确" });
    }
    this.attempts.delete(ip);
    if (this.sessions.size >= 100) this.sessions.delete(this.sessions.keys().next().value);
    this.sessions.delete(this.sessionId(request));
    const id = randomBytes(32).toString("base64url");
    this.sessions.set(id, { expires: Date.now() + SESSION_MS, busy: false, media: new Map(), streams: 0 });
    response.setHeader("set-cookie", this.cookie(id, SESSION_MS / 1000));
    return send(response, 200, { authenticated: true });
  }
  async query(response, session, input) {
    const body = validateQuery(input);
    if (Date.now() - this.queryWindow.start > WINDOW_MS) this.queryWindow = { start: Date.now(), count: 0 };
    if (session.busy || this.running >= 3 || this.queryWindow.count >= 30) {
      response.setHeader("retry-after", "10");
      return send(response, 429, { error: "查询繁忙，请稍后再试", retryAfterSeconds: 10 });
    }
    session.busy = true; this.running++; this.queryWindow.count++;
    const started = performance.now();
    try {
      const result = await this.options.query(body);
      this.addPreviews(result, session);
      return send(response, 200, { ...result, diagnostics: { mode: body.mode, elapsedMs: Math.round(performance.now() - started) } });
    } catch (error) {
      const limited = error.statusCode === 429;
      const retry = limited ? Math.max(1, Number(error.retryAfter) || 60) : undefined;
      if (retry) response.setHeader("retry-after", String(retry));
      return send(response, limited ? 429 : 502, { error: limited ? "模型服务限流，请稍后重试" : "知识库查询失败，请重试；若持续失败请检查服务日志", retryAfterSeconds: retry });
    } finally { session.busy = false; this.running--; }
  }
  addPreviews(result, session) {
    for (const source of result.sources ?? result.results ?? []) {
      const descriptor = mediaDescriptor(source);
      if (!descriptor && source.sourceType === "external_asset" && source.media?.access === "public" && publicMediaUrl(source.media.url)) {
        source.media = { ...source.media, previewUrl: source.media.url, previewMode: "external-public" };
        continue;
      }
      if (!descriptor || !this.options.openMedia) continue;
      const id = randomBytes(18).toString("hex");
      if (session.media.size >= 100) session.media.delete(session.media.keys().next().value);
      session.media.set(id, descriptor);
      source.media = { ...source.media, previewUrl: PREFIX + "media/" + id };
    }
  }
  async preview(request, response, session) {
    if (!session) return send(response, 401, { error: "请重新登录后查看媒体" });
    const id = request.url.slice((PREFIX + "media/").length);
    const descriptor = /^[a-f0-9]{36}$/.test(id) ? session.media.get(id) : null;
    if (!descriptor) return send(response, 404, { error: "媒体预览已失效，请重新查询" });
    if (session.streams >= 6 || this.mediaStreams >= 12) return send(response, 429, { error: "媒体加载繁忙，请稍后重试" });
    session.streams++; this.mediaStreams++;
    try { await streamPreview({ request, response, descriptor, open: this.options.openMedia }); }
    finally { session.streams--; this.mediaStreams--; }
  }
  async handle(request, response) {
    if (request.url === PREFIX.slice(0, -1)) {
      response.writeHead(302, { location: PREFIX }); response.end(); return true;
    }
    if (!request.url?.startsWith(PREFIX)) return false;
    securityHeaders(response);
    try {
      this.prune();
      const route = request.url.slice(PREFIX.length);
      if (request.method === "GET" && STATIC.has(route)) {
        const [file, type] = STATIC.get(route);
        const bytes = await readFile(new URL("./console/" + file, import.meta.url));
        response.writeHead(200, { "content-type": type + "; charset=utf-8" }); response.end(bytes); return true;
      }
      const id = this.sessionId(request);
      const session = this.sessions.get(id);
      if (["GET", "HEAD"].includes(request.method) && route.startsWith("media/")) { await this.preview(request, response, session); return true; }
      if (request.method === "GET" && route === "session") { send(response, 200, { authenticated: Boolean(session) }); return true; }
      if (request.method !== "POST" || !["session", "logout", "query"].includes(route)) { send(response, 404, { error: "接口不存在" }); return true; }
      if (request.headers.origin !== this.origin || !request.headers["content-type"]?.startsWith("application/json")) { send(response, 403, { error: "请求来源或格式不允许" }); return true; }
      if (route !== "session" && !session) { send(response, 401, { error: "会话已过期，请重新登录" }); return true; }
      const body = await readJson(request);
      if (route === "session") this.login(request, response, body);
      else if (route === "logout") { this.sessions.delete(id); response.setHeader("set-cookie", this.cookie("", 0)); send(response, 200, { authenticated: false }); }
      else await this.query(response, session, body);
    } catch (error) {
      if (response.headersSent) response.destroy();
      else send(response, error.status ?? 500, { error: error.status ? error.message : "测试页面服务暂不可用" });
    }
    return true;
  }
}
