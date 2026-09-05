"use strict";
const $ = (id) => document.getElementById(id);
let lastResult = null;
let lastAnswer = "";
let authenticated = false;

function notice(message = "") { $("notice").textContent = message; $("notice").hidden = !message; }
function setAuth(value) {
  authenticated = value;
  $("login").hidden = value; $("workspace").hidden = !value; $("logout").hidden = !value;
  if (!value) clearResult();
}
async function api(route, body) {
  const response = await fetch("./" + route, { method: body === undefined ? "GET" : "POST",
    headers: body === undefined ? {} : { "content-type": "application/json" }, credentials: "same-origin",
    body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(85000) });
  const data = await response.json();
  if (!response.ok) {
    if (response.status === 401) setAuth(false);
    throw new Error((data.error || "请求失败") + (data.retryAfterSeconds ? `（建议 ${data.retryAfterSeconds} 秒后重试）` : ""));
  }
  return data;
}
function node(tag, text, className) {
  const element = document.createElement(tag);
  if (text !== undefined) element.textContent = text;
  if (className) element.className = className;
  return element;
}
function clearResult() {
  lastResult = null; lastAnswer = "";
  $("result").hidden = true; $("clear").hidden = true; $("empty").hidden = false;
  for (const id of ["answer", "sources", "raw-json", "metrics", "result-question"]) $(id).replaceChildren();
}
function safeUrl(value) {
  try { const url = new URL(value); return url.protocol === "https:" ? url.href : null; }
  catch { return null; }
}
function mediaLabel(media) {
  const labels = { image: "图片", video: "视频", pdf: "PDF" };
  return labels[media?.kind];
}
function seconds(value) {
  const n = Math.max(0, Math.floor(Number(value)));
  return `${Math.floor(n / 60)}:${String(n % 60).padStart(2, "0")}`;
}
function renderMedia(media, title) {
  if (!["image", "video"].includes(media?.kind)) return null;
  const container = node("div", undefined, "media-preview");
  const external = media.previewMode === "external-public" && safeUrl(media.previewUrl);
  if (!external && !/^\/knowledge\/console\/media\/[a-f0-9]{36}$/.test(media.previewUrl ?? "")) {
    container.append(node("p", "该来源暂无可用预览，请打开原始媒体核实。", "media-message")); return container;
  }
  const status = node("p", "正在加载原始媒体…", "media-message");
  const element = node(media.kind === "image" ? "img" : "video");
  element.referrerPolicy = "no-referrer";
  const start = Number.isFinite(media.startSeconds) ? Math.max(0, media.startSeconds) : 0;
  element.src = media.previewUrl + (media.kind === "video" ? "#t=" + start : "");
  element.addEventListener("error", () => { status.hidden = false; status.textContent = "预览加载失败或浏览器不支持此格式，请重新查询或打开飞书原件。"; });
  if (media.kind === "image") {
    element.alt = title || "检索命中的原图"; element.loading = "lazy";
    element.addEventListener("load", () => { status.hidden = true; });
    const link = node("a", "查看原图", "source-link"); link.href = media.previewUrl; link.target = "_blank"; link.rel = "noopener noreferrer";
    container.append(element, status, link);
  } else {
    element.controls = true; element.playsInline = true; element.preload = "metadata";
    element.setAttribute("aria-label", (title || "检索命中视频") + "预览");
    element.addEventListener("loadedmetadata", () => { if (start < element.duration) element.currentTime = start; });
    element.addEventListener("loadeddata", () => { status.hidden = true; });
    const play = node("button", "从命中片段播放", "quiet"); play.type = "button";
    play.addEventListener("click", async () => {
      try { element.currentTime = Math.min(start, element.duration || start); await element.play(); }
      catch { status.hidden = false; status.textContent = "视频暂时无法播放，请重试或打开原始文件。"; }
    });
    container.append(element, status, play);
  }
  return container;
}
function renderSource(source, index, mode) {
  const card = node("article", undefined, "source");
  card.id = "source-" + (source.id || index + 1);
  const meta = node("div", undefined, "source-meta");
  const type = mediaLabel(source.media) || ({ feishu_docx: "飞书文档", mysql_catalog: "商品资料", yuque_media: "媒体分析", external_asset: "外部素材" }[source.sourceType] || "知识资料");
  meta.append(node("span", mode === "ask" ? source.id : `排序 ${source.rank ?? index + 1}`), node("span", type));
  if (source.asset?.source) meta.append(node("span", "来源：" + source.asset.source), node("span", "素材ID：" + source.asset.id));
  if (source.snapshotAt) meta.append(node("span", "快照 " + String(source.snapshotAt).slice(0, 10)));
  if (mode === "search" && Number.isFinite(source.retrievalScore)) meta.append(node("span", "召回分数 " + source.retrievalScore.toFixed(4)));
  card.append(meta, node("h4", source.title || "未命名来源"));
  const media = source.media;
  if (media?.kind === "video" && Number.isFinite(media.startSeconds) && Number.isFinite(media.endSeconds)) card.append(node("p", `命中时间段：${seconds(media.startSeconds)} – ${seconds(media.endSeconds)}`));
  if (media?.page) card.append(node("p", `页码：${media.page}`));
  if (media?.folderPath) card.append(node("p", "素材路径：" + media.folderPath));
  const preview = renderMedia(media, source.title);
  if (preview) card.append(preview);
  const details = node("details");
  if (index === 0) details.open = true;
  details.append(node("summary", "查看引用原文"), node("blockquote", source.quote ?? source.content ?? "此来源没有返回原文片段。"));
  card.append(details);
  const url = safeUrl(media?.url) || safeUrl(source.url ?? source.sourceUrl);
  if (url) { const link = node("a", media ? "打开原始媒体 / 飞书来源" : "打开来源文档", "source-link"); link.href = url; link.target = "_blank"; link.rel = "noopener noreferrer"; card.append(link); }
  return card;
}
function renderAnswer(answer, sources) {
  $("answer").replaceChildren();
  for (const part of answer.split(/(\[S\d+\]|\*\*[^*]+\*\*)/g)) {
    const id = part.slice(1, -1);
    if (/^\[S\d+\]$/.test(part) && sources.some((source) => source.id === id)) {
      const button = node("button", part, "citation"); button.type = "button"; button.setAttribute("aria-label", "查看引用 " + id);
      button.addEventListener("click", () => { const target = $("source-" + id); target?.scrollIntoView({ block: "start" }); const detail = target?.querySelector("details"); if (detail) detail.open = true; });
      $("answer").append(button);
    } else if (part.startsWith("**") && part.endsWith("**")) $("answer").append(node("strong", part.slice(2, -2)));
    else $("answer").append(document.createTextNode(part));
  }
}
function renderResult(data, mode) {
  lastResult = data; lastAnswer = data.answer ?? "";
  $("empty").hidden = true; $("result").hidden = false; $("clear").hidden = false;
  const sources = mode === "ask" ? data.sources ?? [] : data.results ?? [];
  const metrics = $("metrics"); metrics.replaceChildren();
  metrics.append(node("span", mode === "ask" ? (data.grounded ? "有引用支撑" : "资料不足") : "检索结果", "grounded"));
  metrics.append(node("span", `${sources.length} 个来源`), node("span", `耗时 ${(data.diagnostics.elapsedMs / 1000).toFixed(1)} 秒`));
  if (data.retrievedCount !== undefined) metrics.append(node("span", `召回 ${data.retrievedCount} 个片段`));
  if (data.model || data.rerankModel) metrics.append(node("span", data.model || data.rerankModel));
  if (data.usage) metrics.append(node("span", `Luna tokens：${data.usage.inputTokens ?? "—"} 输入 / ${data.usage.outputTokens ?? "—"} 输出`));
  $("result-question").textContent = "本次问题：" + data.question;
  $("answer-section").hidden = mode !== "ask";
  renderAnswer(lastAnswer, sources);
  $("grounding-note").textContent = data.grounded ? "“有引用支撑”表示模型选用了资料，并不等于每句话都已通过事实审核。" : "知识库没有提供足够依据时，不应将猜测当作答案。";
  $("sources-title").textContent = `${mode === "ask" ? "引用来源" : "检索片段"} · ${sources.length}`;
  $("sources").replaceChildren(...sources.map((source, index) => renderSource(source, index, mode)));
  if (!sources.length) $("sources").append(node("p", "没有可展示的来源。请尝试更具体的基地名称、设施或商品问题。"));
  $("raw-json").textContent = JSON.stringify(data, null, 2);
}
async function submitQuery(event) {
  event.preventDefault();
  const mode = document.querySelector("input[name=mode]:checked").value;
  const question = $("question").value.trim();
  if (question.length < 2) { notice("请至少输入两个字的问题。"); return; }
  notice(); clearResult(); $("empty").hidden = true; $("loading").hidden = false;
  $("query-fields").disabled = true; $("logout").disabled = true; $("clear").disabled = true;
  try { const data = await api("query", { mode, question, limit: Number($("limit").value) }); if (authenticated) renderResult(data, mode); }
  catch (error) { notice(error.name === "TimeoutError" ? "查询超时，请稍后重试；本次未获得有效返回。" : error.message); $("empty").hidden = false; }
  finally { $("loading").hidden = true; $("query-fields").disabled = false; $("logout").disabled = false; $("clear").disabled = false; }
}
$("login-form").addEventListener("submit", async (event) => {
  event.preventDefault(); notice(); $("login-button").disabled = true;
  try { await api("session", { token: $("token").value }); $("token").value = ""; setAuth(true); $("question").focus(); }
  catch (error) { notice(error.message); }
  finally { $("login-button").disabled = false; }
});
$("logout").addEventListener("click", async () => { try { await api("logout", {}); notice(); setAuth(false); } catch (error) { notice(error.message); } });
$("query-form").addEventListener("submit", submitQuery);
$("question").addEventListener("input", () => { $("count").textContent = `${$("question").value.length} / 500`; });
$("question").addEventListener("keydown", (event) => { if ((event.metaKey || event.ctrlKey) && event.key === "Enter") { event.preventDefault(); if (!$("query-fields").disabled) $("query-form").requestSubmit(); } });
document.querySelectorAll("[data-example]").forEach((button) => button.addEventListener("click", () => { $("question").value = button.dataset.example; $("question").dispatchEvent(new Event("input")); $("question").focus(); }));
document.querySelectorAll("input[name=mode]").forEach((input) => input.addEventListener("change", () => { $("mode-hint").textContent = input.value === "ask" ? "检索资料后，由 Luna 筛选引用并回答。" : "查看 Luna 排序后的原文片段，不生成回答。"; }));
$("clear").addEventListener("click", () => { clearResult(); notice(); });
async function copy(text) { try { await navigator.clipboard.writeText(text); notice("已复制。"); } catch { notice("复制不可用，请选中文字手动复制。"); } }
$("copy-answer").addEventListener("click", () => copy(lastAnswer));
$("copy-json").addEventListener("click", () => copy(JSON.stringify(lastResult, null, 2)));
api("session").then((data) => setAuth(data.authenticated)).catch(() => notice("无法连接测试服务，请刷新页面后重试。"));
