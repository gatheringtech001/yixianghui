import { AssetError, assetText, normalizeAsset, validateIdentity } from "./asset-schema.mjs";
import { contentHash, pointId, sparseVector } from "./lib.mjs";

const sourceId = ({ source, id }) => `external:${source}:${id}`;
export class AssetIndex {
  constructor({ store, models }) { this.store = store; this.models = models; this.locks = new Set(); }
  async read(identity) {
    validateIdentity(identity);
    const id = pointId(sourceId(identity), 0);
    const result = await this.store.api(`/collections/${encodeURIComponent(this.store.config.collection)}/points`, {
      method: "POST", body: JSON.stringify({ ids: [id], with_payload: true, with_vector: false }),
    });
    if (!Array.isArray(result.result) || result.result.length > 1) throw new Error("Invalid index read response");
    const point = result.result[0];
    if (point && (point.id !== id || point.payload?.source_type !== "external_asset" || point.payload.source_id !== sourceId(identity)
      || point.payload.asset?.source !== identity.source || point.payload.asset?.id !== identity.id || !Number.isFinite(Date.parse(point.payload.asset?.updatedAt)))) {
      throw new AssetError(409, "identity_conflict", "索引标识冲突，未修改任何数据");
    }
    return point;
  }
  async get(identity) {
    const point = await this.read(identity);
    if (!point) throw new AssetError(404, "asset_not_found", "素材索引不存在");
    return { pointId: point.id, status: "indexed", asset: point.payload.asset, indexedAt: point.payload.snapshot_at };
  }
  async put(identity, input) {
    const asset = normalizeAsset(identity, input); const key = sourceId(identity);
    if (this.locks.has(key)) throw new AssetError(409, "write_in_progress", "此素材正在写入，请稍后原样重试");
    this.locks.add(key);
    try { return await this.save(asset); } finally { this.locks.delete(key); }
  }
  async save(asset) {
    const prior = await this.read(asset); const hash = contentHash(JSON.stringify(asset));
    if (prior?.payload.asset_hash === hash) return this.result("unchanged", prior);
    if (prior && Date.parse(asset.updatedAt) <= Date.parse(prior.payload.asset.updatedAt)) {
      throw new AssetError(409, "stale_revision", "更新版本时间不晚于现有版本；请核实updatedAt");
    }
    const content = assetText(asset); const textHash = contentHash(content);
    const point = { id: pointId(sourceId(asset), 0), payload: {
      source_type: "external_asset", source_id: sourceId(asset), title: asset.title, content, chunk_index: 0,
      source_url: asset.url, permission_scope: "internal", snapshot_at: new Date().toISOString(),
      source_updated_at: asset.updatedAt, media: asset.media ?? null, asset, asset_hash: hash, text_hash: textHash,
    } };
    if (prior?.payload.text_hash === textHash) {
      await this.store.api(`/collections/${encodeURIComponent(this.store.config.collection)}/points/payload?wait=true`, {
        method: "POST", body: JSON.stringify({ points: [point.id], payload: point.payload }),
      });
    } else {
      const vectors = await this.models.embed([content]); const vector = vectors[0];
      if (vectors.length !== 1 || !Array.isArray(vector) || vector.length !== this.store.config.dimensions
        || vector.some((value) => !Number.isFinite(value)) || vector.every((value) => value === 0)) throw new Error("Invalid asset embedding");
      await this.store.upsert([{ ...point, vector: { dense: vector, lexical: sparseVector(content) } }]);
    }
    return this.result(prior ? "updated" : "created", point);
  }
  result(status, point) {
    return { status, source: point.payload.asset.source, id: point.payload.asset.id,
      pointId: point.id, updatedAt: point.payload.asset.updatedAt, indexedAt: point.payload.snapshot_at };
  }
}
