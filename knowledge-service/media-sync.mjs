import fs from "node:fs/promises";
import { pathToFileURL } from "node:url";
import { chunkDocument, contentHash, pointId, sparseVector } from "./lib.mjs";
import { AzureModels, QdrantStore } from "./service.mjs";

export function mediaPoints(record, createdAt) {
  if (!record || !/^[A-Za-z0-9:_-]{1,250}$/.test(record.id) || typeof record.text !== "string"
      || !["image", "video", "pdf"].includes(record.media?.kind)) throw new Error("Invalid media record");
  const source = new URL(record.sourceUrl);
  if (source.protocol !== "https:" || source.hostname !== "vcnnjnb870d6.feishu.cn") throw new Error("Invalid media source");
  if (record.media.kind === "video" && (!Number.isFinite(record.media.startSeconds)
      || !Number.isFinite(record.media.endSeconds) || record.media.startSeconds < 0
      || record.media.endSeconds <= record.media.startSeconds)) throw new Error("Invalid video time range");
  const sourceId = `media:${record.id}`;
  return chunkDocument(record.title, `媒体类型: ${record.media.kind}\n${record.text}`).map((part) => ({
    id: pointId(sourceId, part.index), text: part.text,
    payload: { source_type: "yuque_media", source_id: sourceId, title: record.title,
      content: part.text, chunk_index: part.index, source_url: record.sourceUrl,
      permission_scope: "internal", snapshot_at: createdAt, source_updated_at: record.sourceUpdatedAt,
      media: record.media, yuque_url: record.yuqueUrl ?? "" },
  }));
}

export async function syncMedia({ snapshot, store, models, manifestFile }) {
  if (snapshot.version !== 1 || !Array.isArray(snapshot.records) || !Number.isFinite(Date.parse(snapshot.createdAt))) {
    throw new Error("Invalid media snapshot");
  }
  if (new Set(snapshot.records.map((record) => record.id)).size !== snapshot.records.length) throw new Error("Duplicate media IDs");
  let manifest = { version: 1, records: {} };
  try { manifest = JSON.parse(await fs.readFile(manifestFile, "utf8")); }
  catch (error) { if (error.code !== "ENOENT") throw error; }
  if (manifest.createdAt && snapshot.createdAt < manifest.createdAt) throw new Error("Stale media snapshot");
  const pending = [];
  const jobs = [];
  let payloadUpdates = 0;
  for (const record of snapshot.records) {
    const points = mediaPoints(record, snapshot.createdAt);
    const textHash = contentHash(JSON.stringify(points.map((point) => point.text)));
    const metadataHash = contentHash(JSON.stringify(record));
    const saved = manifest.records[record.id];
    if (saved?.textHash === textHash && saved.metadataHash === metadataHash) continue;
    if (saved?.textHash === textHash) {
      for (const point of points) await store.api(`/collections/${store.config.collection}/points/payload?wait=true`, {
        method: "POST", body: JSON.stringify({ points: [point.id], payload: point.payload }),
      });
      manifest.records[record.id] = { ...saved, metadataHash };
      payloadUpdates++;
      continue;
    }
    pending.push(...points);
    jobs.push({ id: record.id, textHash, metadataHash, pointIds: points.map((point) => point.id), end: pending.length });
  }
  await store.ensureCollection();
  let completed = 0;
  const save = async () => {
    await fs.writeFile(`${manifestFile}.next`, JSON.stringify(manifest), { mode: 0o600 });
    await fs.rename(`${manifestFile}.next`, manifestFile);
  };
  for (let offset = 0; offset < pending.length; offset += 16) {
    const batch = pending.slice(offset, offset + 16);
    const vectors = await models.embed(batch.map((point) => point.text));
    if (vectors.length !== batch.length) throw new Error("Media embedding count mismatch");
    await store.upsert(batch.map(({ id, text, payload }, index) => ({ id, payload,
      vector: { dense: vectors[index], lexical: sparseVector(text) } })));
    while (jobs[completed]?.end <= offset + batch.length) {
      const job = jobs[completed++];
      const prior = manifest.records[job.id]?.pointIds ?? [];
      await store.deleteIds(prior.filter((id) => !job.pointIds.includes(id)));
      manifest.records[job.id] = { textHash: job.textHash, metadataHash: job.metadataHash, pointIds: job.pointIds };
    }
    await save();
  }
  // Missing/deleted source candidates are reported, never automatically purged.
  const current = new Set(snapshot.records.map((record) => record.id));
  const missingCandidates = snapshot.partial ? [] : Object.keys(manifest.records).filter((id) => !current.has(id));
  manifest.createdAt = snapshot.createdAt;
  await save();
  return { records: snapshot.records.length, indexedPoints: pending.length, changedRecords: completed,
    payloadUpdates, missingCandidates: missingCandidates.length, failures: snapshot.failures?.length ?? 0 };
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const snapshot = JSON.parse(await fs.readFile(process.argv[2], "utf8"));
  const result = await syncMedia({ snapshot,
    manifestFile: "/var/lib/yixianghui-knowledge/media-manifest.json",
    store: new QdrantStore({ url: process.env.QDRANT_URL, apiKey: process.env.QDRANT_API_KEY,
      collection: process.env.QDRANT_COLLECTION, dimensions: Number(process.env.EMBEDDING_DIMENSIONS) }),
    models: new AzureModels({ baseUrl: process.env.AZURE_OPENAI_BASE_URL, apiKey: process.env.AZURE_OPENAI_KEY,
      apiVersion: process.env.AZURE_OPENAI_API_VERSION, embeddingModel: process.env.AZURE_EMBEDDING_MODEL }),
  });
  console.log(JSON.stringify(result));
}
