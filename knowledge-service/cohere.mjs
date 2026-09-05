import { sanitizeText } from "./lib.mjs";

export class CohereRerankError extends Error {
  constructor(message, options = {}) {
    super(message);
    this.statusCode = options.statusCode ?? 502;
    this.retryAfter = options.retryAfter;
  }
}

function retrySeconds(headers) {
  const value = Number(headers.get("retry-after") ?? headers.get("x-ratelimit-reset-requests"));
  return Number.isFinite(value) && value > 0 ? Math.min(3600, Math.ceil(value)) : 60;
}

function rankedCandidates(results, candidates, count) {
  if (!Array.isArray(results) || results.length !== count) {
    throw new CohereRerankError("Cohere returned an invalid result count");
  }
  const seen = new Set();
  let previous = Infinity;
  return results.map((result) => {
    const { index, relevance_score: score } = result ?? {};
    if (!Number.isInteger(index) || index < 0 || index >= candidates.length || seen.has(index)
        || !Number.isFinite(score) || score < 0 || score > 1 || score > previous) {
      throw new CohereRerankError("Cohere returned an invalid ranking");
    }
    seen.add(index);
    previous = score;
    return { ...candidates[index], rerankScore: score };
  });
}

export class CohereReranker {
  constructor(config) {
    const url = new URL(config.url);
    if (url.protocol !== "https:" || url.username || url.password) {
      throw new Error("Cohere endpoint must use HTTPS without embedded credentials");
    }
    this.config = config;
  }

  async rerank(question, candidates, limit) {
    if (!candidates.length) return [];
    const count = Math.min(limit, candidates.length);
    const documents = candidates.map(({ payload }) =>
      `title: ${JSON.stringify(sanitizeText(payload.title))}\ncontent: ${JSON.stringify(sanitizeText(payload.content))}`
    );
    let response;
    try {
      response = await fetch(this.config.url, {
        method: "POST", redirect: "error", signal: AbortSignal.timeout(60_000),
        headers: { "content-type": "application/json", "api-key": this.config.apiKey },
        body: JSON.stringify({ model: this.config.model, query: question, documents, top_n: count }),
      });
    } catch {
      throw new CohereRerankError("Cohere rerank request failed or timed out");
    }
    if (!response.ok) {
      await response.body?.cancel();
      if (response.status === 429) {
        throw new CohereRerankError("Cohere rerank rate limit exceeded", {
          statusCode: 429, retryAfter: retrySeconds(response.headers),
        });
      }
      throw new CohereRerankError(`Cohere rerank upstream HTTP ${response.status}`);
    }
    let data;
    try {
      data = await response.json();
    } catch {
      throw new CohereRerankError("Cohere returned invalid JSON");
    }
    return rankedCandidates(data?.results, candidates, count);
  }
}
