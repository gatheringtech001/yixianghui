import { isIP } from "node:net";
import { sanitizeText } from "./lib.mjs";

export class AssetError extends Error {
  constructor(status, code, message) { super(message); this.status = status; this.code = code; }
}
const invalid = (message) => { throw new AssetError(400, "invalid_asset", message); };
const FIELDS = ["kind", "title", "content", "url", "tags", "updatedAt", "checksum", "media", "base"];
function object(value, allowed) {
  if (!value || typeof value !== "object" || Array.isArray(value) || Object.keys(value).some((key) => !allowed.includes(key))) invalid("对象包含不支持的字段；此接口只接收索引，不接收文件");
}
function text(value, max, name) {
  if (typeof value !== "string" || !value.trim() || value.length > max) invalid(`${name} 必须是 1–${max} 字符的文本`);
  if (sanitizeText(value) !== value) invalid(`${name} 包含无效Unicode字符`);
  return value.trim();
}
export function validateIdentity(value) {
  if (!/^[a-z][a-z0-9_-]{1,47}$/.test(value.source ?? "") || !/^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$/.test(value.id ?? "")) invalid("source或素材ID无效");
  return value;
}
export function publicMediaUrl(value) {
  try {
    const url = new URL(value);
    return url.protocol === "https:" && !url.username && !url.password && !isIP(url.hostname.replace(/^\[|\]$/g, ""))
      && url.hostname.includes(".") && !/(^|\.)(localhost|local|internal|test|invalid)$/.test(url.hostname);
  } catch { return false; }
}
function mediaFields(input, kind, url) {
  const value = input ?? {}; object(value, ["access", "fileToken", "startSeconds", "endSeconds", "page"]);
  const access = value.access ?? "restricted";
  if (!["restricted", "public"].includes(access)) invalid("media.access应为restricted或public");
  if (access === "public" && !publicMediaUrl(url)) invalid("公开预览必须使用有效的公网HTTPS域名");
  const output = { kind, url, access };
  if (value.fileToken !== undefined) {
    if (!/^[A-Za-z0-9]{15,50}$/.test(value.fileToken) || new URL(url).hostname !== "vcnnjnb870d6.feishu.cn") invalid("fileToken仅支持已配置的飞书来源");
    output.fileToken = value.fileToken;
  }
  if (value.startSeconds !== undefined || value.endSeconds !== undefined) {
    if (kind !== "video" || !Number.isFinite(value.startSeconds) || !Number.isFinite(value.endSeconds)
      || value.startSeconds < 0 || value.endSeconds <= value.startSeconds || value.endSeconds > 864000) invalid("视频时间段无效");
    output.startSeconds = value.startSeconds; output.endSeconds = value.endSeconds;
  }
  if (value.page !== undefined) {
    if (kind !== "pdf" || !Number.isInteger(value.page) || value.page < 1 || value.page > 100000) invalid("PDF页码无效");
    output.page = value.page;
  }
  return output;
}
export function normalizeAsset(identity, input) {
  validateIdentity(identity); object(input, FIELDS);
  if (!["text", "image", "video", "pdf"].includes(input.kind)) invalid("kind应为text、image、video或pdf");
  let url;
  try { url = new URL(text(input.url, 2048, "url")); } catch { invalid("url必须是有效HTTPS地址"); }
  if (url.protocol !== "https:" || url.username || url.password) invalid("原件地址应为不含凭据的HTTPS链接");
  if ([...url.searchParams.keys()].some((key) => /token|signature|secret|password|^pwd$|api.?key|authorization|^sig$/i.test(key))) invalid("请提交稳定原件地址，不要提交带密钥或临时签名的链接");
  if (!Array.isArray(input.tags ?? []) || (input.tags ?? []).length > 20) invalid("tags最多20项");
  const updatedAt = text(input.updatedAt, 40, "updatedAt"); const time = Date.parse(updatedAt);
  if (!/^\d{4}-\d{2}-\d{2}T.*(?:Z|[+-]\d{2}:\d{2})$/.test(updatedAt) || !Number.isFinite(time) || time > Date.now() + 300000) invalid("updatedAt应为带时区的有效时间，不能超前超过5分钟");
  const asset = { ...identity, kind: input.kind, title: text(input.title, 200, "title"), content: text(input.content, 2700, "content"),
    url: url.href, tags: [...new Set((input.tags ?? []).map((tag) => text(tag, 40, "tag")))].sort(), updatedAt: new Date(time).toISOString() };
  if (input.base !== undefined) {
    object(input.base, ["id", "name"]);
    asset.base = { id: text(input.base.id, 128, "base.id"), name: text(input.base.name, 160, "base.name") };
  }
  if (input.checksum !== undefined) {
    if (!/^[a-fA-F0-9]{64}$/.test(input.checksum)) invalid("checksum应为原件SHA-256");
    asset.checksum = input.checksum.toLowerCase();
  }
  if (input.kind !== "text") asset.media = mediaFields(input.media, input.kind, asset.url);
  else if (input.media !== undefined) invalid("文字索引不应带media字段");
  if (assetText(asset).length > 3000) invalid("标题、描述和标签合计检索文本超过3000字符；请按片段拆成独立索引");
  return asset;
}
export function assetText(asset) {
  return `${asset.title}\n素材类型: ${{ text: "文字", image: "图片", video: "视频", pdf: "PDF" }[asset.kind]}\n${asset.content}\n标签: ${asset.tags.join("、")}`
    + (asset.base ? `\n关联基地: ${asset.base.name} (${asset.base.id})` : "");
}
