import { readFile } from "node:fs/promises";
import { sparseVector } from "./lib.mjs";

const MAX_CANDIDATES = 30;
export class RetrievalInputError extends Error {}

export async function queryStore(store, options) {
  const { question, dense, titles, kind, limit = MAX_CANDIDATES } = options;
  const filter = { must: [{ key: "permission_scope", match: { value: "internal" } }] };
  if (titles?.length) filter.must.push({ key: "title", match: { any: titles } });
  const kinds = kind ? [kind] : mediaKinds(question);
  if (kinds.length) filter.must.push({ key: "media.kind", match: { any: kinds } });
  const result = await store.api(`/collections/${encodeURIComponent(store.config.collection)}/points/query`, {
    method: "POST", body: JSON.stringify({ prefetch: [
      { query: dense, using: "dense", limit: 60, filter, params: { exact: true } },
      { query: sparseVector(question), using: "lexical", limit: 60, filter },
    ], query: { fusion: "rrf" }, limit, with_payload: true }),
  });
  if (!Array.isArray(result.result?.points)) throw new Error("Qdrant returned no valid retrieval result");
  return result.result.points;
}

function mediaKinds(question) {
  const kinds = [];
  if (/图片|照片|原图/.test(question) && !/不要(?:任何)?(?:图片|照片|原图)/.test(question)) kinds.push("image");
  if (/视频|录像/.test(question) && !/不要(?:任何)?(?:视频|录像)/.test(question)) kinds.push("video");
  return kinds;
}

function namedGroups(question, titles) {
  const names = [...new Set(titles.map((title) => title.match(/^[\p{Script=Han}]{2,5}[一二三四五六七八九十\d]+号/u)?.[0])
    .filter((name) => name && question.includes(name)))];
  if (names.length > 4) throw new RetrievalInputError("Compare no more than four named bases per question");
  return names.map((name) => {
    const matches = titles.filter((title) => title.startsWith(name));
    const exact = matches.filter((title) => question.includes(title));
    const scoped = exact.length ? matches.filter((title) => exact.some((full) =>
      title === full || title.startsWith(full + "（") || title.startsWith(full + "("))) : matches;
    return { name, titles: [...new Set(scoped)] };
  });
}

function mergeBalanced(groups) {
  const points = [], seen = new Set();
  for (let index = 0; points.length < MAX_CANDIDATES && groups.some((group) => index < group.length); index++) {
    for (const group of groups) {
      const item = group[index];
      if (!item || seen.has(String(item.id)) || points.length >= MAX_CANDIDATES) continue;
      seen.add(String(item.id)); points.push(item);
    }
  }
  return points;
}

export async function retrieveForAnswer(service, question) {
  const [dense] = await service.models.embed([question]);
  const initial = await service.store.search(question, dense, MAX_CANDIDATES);
  let titles = initial.map((item) => String(item.payload.title ?? ""));
  if (service.manifestFile) {
    const manifest = JSON.parse(await readFile(service.manifestFile, "utf8"));
    if (!manifest.documents || Array.isArray(manifest.documents)) throw new Error("Knowledge directory unavailable");
    titles = titles.concat(Object.values(manifest.documents).map((doc) => String(doc.title ?? "")));
  }
  const entities = namedGroups(question, titles);
  const kinds = mediaKinds(question);
  if (!entities.length && kinds.length < 2) return { points: initial, coverage: [] };
  let topic = question;
  for (const entity of entities) topic = topic.replaceAll(entity.name, "");
  const plans = (entities.length ? entities : [{ name: "", titles: [] }]).flatMap((entity) =>
    (kinds.length ? kinds : [null]).map((kind) => ({ ...entity, kind,
      question: `${entity.name} ${kind === "image" ? "图片 " : kind === "video" ? "视频 " : ""}${topic}`.slice(0, 500) })));
  const vectors = await service.models.embed(plans.map((plan) => plan.question));
  if (vectors.length !== plans.length) throw new Error("Query embedding count mismatch");
  const groups = [];
  for (let i = 0; i < plans.length; i++) {
    const plan = plans[i];
    const found = await service.store.searchScoped(plan.question, vectors[i], { limit: MAX_CANDIDATES, titles: plan.titles, kind: plan.kind });
    groups.push(found.filter(({ payload }) => (!plan.titles.length || plan.titles.includes(payload.title))
      && (!plan.kind || payload.media?.kind === plan.kind)));
  }
  return { points: mergeBalanced(groups), coverage: plans.map((plan, i) => ({ entity: plan.name,
    kind: plan.kind, retrieved: groups[i].length, exhaustive: false })) };
}
