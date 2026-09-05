import { Readable, Transform } from "node:stream";
import { pipeline } from "node:stream/promises";

const IMAGE_TYPES = new Set(["image/jpeg", "image/png", "image/webp", "image/gif", "image/avif"]);
const VIDEO_TYPES = new Set(["video/mp4", "video/quicktime", "video/webm"]);
const LIMITS = { image: 20 * 1024 * 1024, video: 1024 * 1024 * 1024 };
const fail = (status, message) => Object.assign(new Error(message), { status });

export function mediaDescriptor(source) {
  const media = source.media;
  if (!media || !["image", "video"].includes(media.kind) || !/^[A-Za-z0-9]{15,50}$/.test(media.fileToken ?? "")) return null;
  let url;
  try { url = new URL(source.url ?? source.sourceUrl); } catch { return null; }
  if (url.origin !== "https://vcnnjnb870d6.feishu.cn") return null;
  const parts = url.pathname.split("/");
  const type = parts[1] === "docx" && media.kind === "image" ? "medias"
    : parts[1] === "file" && parts[2] === media.fileToken ? "files" : null;
  return type ? { kind: media.kind, token: media.fileToken, type } : null;
}

function validateRange(range) {
  if (!range) return;
  const match = /^bytes=(\d*)-(\d*)$/.exec(range);
  if (!match || (!match[1] && !match[2])) throw fail(416, "不支持这个视频读取范围");
  const start = Number(match[1]), end = Number(match[2]);
  if (!Number.isSafeInteger(start) || !Number.isSafeInteger(end) || (match[1] && match[2] && start > end)) throw fail(416, "无效的视频读取范围");
}

export async function openFeishuMedia(source, descriptor, options = {}) {
  validateRange(options.range);
  if (!source.accessToken || Date.now() >= source.tokenExpiresAt) await source.authenticate();
  const headers = { Authorization: "Bearer " + source.accessToken };
  if (options.range) headers.Range = options.range;
  const response = await (options.fetcher ?? fetch)(
    `https://open.feishu.cn/open-apis/drive/v1/${descriptor.type}/${descriptor.token}/download`,
    { headers, redirect: "error", signal: options.signal ?? AbortSignal.timeout(120000) });
  const type = response.headers.get("content-type")?.split(";")[0].trim();
  const valid = descriptor.kind === "image" ? IMAGE_TYPES : VIDEO_TYPES;
  if (![200, 206].includes(response.status) || !valid.has(type)) {
    await response.body?.cancel();
    throw fail(response.status === 416 ? 416 : response.status === 429 ? 429 : 502,
      "原始媒体暂时无法读取，请重试或打开飞书来源核实权限");
  }
  if (Number(response.headers.get("content-length")) > LIMITS[descriptor.kind]) {
    await response.body?.cancel(); throw fail(413, "媒体超过预览大小限制");
  }
  return response;
}

export async function streamPreview(options) {
  const { request, response, descriptor, open } = options;
  const controller = new AbortController();
  const abort = () => controller.abort();
  response.once("close", abort);
  try {
    const upstream = await open(descriptor, { range: request.headers.range,
      signal: AbortSignal.any([controller.signal, AbortSignal.timeout(120000)]) });
    for (const name of ["content-type", "content-length", "content-range", "accept-ranges"]) {
      const value = upstream.headers.get(name); if (value) response.setHeader(name, value);
    }
    response.setHeader("content-disposition", "inline");
    response.setHeader("cross-origin-resource-policy", "same-origin");
    response.setHeader("x-content-type-options", "nosniff");
    response.setHeader("cache-control", "private, no-store");
    response.writeHead(upstream.status);
    if (request.method === "HEAD") { await upstream.body?.cancel(); response.end(); return; }
    let total = 0;
    const bounded = new Transform({ transform(chunk, encoding, done) {
      total += chunk.length;
      done(total > LIMITS[descriptor.kind] ? fail(413, "媒体超过预览大小限制") : null, chunk);
    } });
    await pipeline(Readable.fromWeb(upstream.body), bounded, response);
  } finally { response.off("close", abort); controller.abort(); }
}
