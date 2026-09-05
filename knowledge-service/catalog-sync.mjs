import fs from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";
import { buildCatalog, catalogPoints } from "./catalog.mjs";
import { sparseVector } from "./lib.mjs";
import { AzureModels, QdrantStore } from "./service.mjs";

async function checkpoint(file, manifest) {
  await fs.mkdir(path.dirname(file), { recursive: true, mode: 0o700 });
  await fs.writeFile(`${file}.next`, JSON.stringify(manifest), { mode: 0o600 });
  await fs.rename(`${file}.next`, file);
}

async function readManifest(file) {
  try {
    const manifest = JSON.parse(await fs.readFile(file, "utf8"));
    if (manifest.version !== 1 || manifest.source !== "mysql_catalog"
        || Object.keys(manifest.documents).some((key) => !key.startsWith("catalog:"))) {
      throw new Error("Invalid catalog manifest namespace");
    }
    return manifest;
  } catch (error) {
    if (error.code !== "ENOENT") throw error;
    return { version: 1, source: "mysql_catalog", documents: {} };
  }
}

export async function syncCatalog({ snapshot, models, store, manifestFile, progress = () => {} }) {
  const catalog = buildCatalog(snapshot);
  const manifest = await readManifest(manifestFile);
  if (manifest.exportedAt && snapshot.exportedAt < manifest.exportedAt) throw new Error("Stale catalog snapshot");
  await store.ensureCollection();
  let changed = 0;
  let processed = 0;
  const jobs = [];
  const pendingPoints = [];
  for (const document of catalog.documents) {
    const saved = manifest.documents[document.sourceId];
    if (saved?.hash === document.hash) continue;
    const points = catalogPoints(document, snapshot.exportedAt);
    pendingPoints.push(...points);
    jobs.push({ document, saved, ids: points.map((point) => point.id), end: pendingPoints.length });
  }
  for (let offset = 0; offset < pendingPoints.length; offset += 16) {
    const batch = pendingPoints.slice(offset, offset + 16);
    const vectors = await models.embed(batch.map((point) => point.text));
    if (vectors.length !== batch.length || vectors.some((vector) =>
      !Array.isArray(vector) || !vector.length || vector.some((value) => !Number.isFinite(value)))) {
      throw new Error("Invalid catalog embedding batch");
    }
    await store.upsert(batch.map(({ id, text, payload }, index) => ({
      id, payload, vector: { dense: vectors[index], lexical: sparseVector(text) },
    })));
    processed += batch.length;
    while (jobs[changed]?.end <= processed) {
      const { document, saved, ids } = jobs[changed];
      await store.deleteIds((saved?.pointIds ?? []).filter((id) => !ids.includes(id)));
      manifest.documents[document.sourceId] = { hash: document.hash, pointIds: ids,
        table: document.table, id: document.id, title: document.title };
      changed += 1;
    }
    await checkpoint(manifestFile, manifest);
    if (offset % 160 === 0) progress({ changed, processed });
  }
  const current = new Set(catalog.documents.map((document) => document.sourceId));
  for (const [id, saved] of Object.entries(manifest.documents)) {
    if (current.has(id)) continue;
    await store.deleteIds(saved.pointIds);
    delete manifest.documents[id];
  }
  manifest.exportedAt = snapshot.exportedAt;
  manifest.fingerprint = snapshot.fingerprint;
  manifest.counts = catalog.counts;
  await checkpoint(manifestFile, manifest);
  return { documents: catalog.documents.length, rows: catalog.represented, counts: catalog.counts,
    orphanDocuments: catalog.orphans, changed, indexedChunks: processed,
    totalPoints: Object.values(manifest.documents).reduce((count, document) => count + document.pointIds.length, 0) };
}

function required(name) {
  if (!process.env[name]) throw new Error(`Missing ${name}`);
  return process.env[name];
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const file = process.argv[2];
  if (!file || !path.isAbsolute(file)) throw new Error("Provide an absolute catalog snapshot path");
  const manifestFile = "/var/lib/yixianghui-knowledge/catalog-manifest.json";
  await fs.mkdir(path.dirname(manifestFile), { recursive: true });
  const lock = await fs.open(`${manifestFile}.lock`, "wx", 0o600);
  try {
    const result = await syncCatalog({
      snapshot: JSON.parse(await fs.readFile(file, "utf8")), manifestFile,
      models: new AzureModels({ baseUrl: required("AZURE_OPENAI_BASE_URL"), apiKey: required("AZURE_OPENAI_KEY"),
        apiVersion: required("AZURE_OPENAI_API_VERSION"), embeddingModel: required("AZURE_EMBEDDING_MODEL") }),
      store: new QdrantStore({ url: required("QDRANT_URL"), apiKey: required("QDRANT_API_KEY"),
        collection: required("QDRANT_COLLECTION"), dimensions: Number(required("EMBEDDING_DIMENSIONS")) }),
      progress: (value) => console.log(JSON.stringify(value)),
    });
    console.log(JSON.stringify(result));
  } finally {
    await lock.close();
    await fs.unlink(`${manifestFile}.lock`);
  }
}
