import test from "node:test";
import assert from "node:assert/strict";
import { CohereReranker } from "./cohere.mjs";
import { createServer } from "./server.mjs";

const config = {
  url: "https://example.test/providers/cohere/v2/rerank",
  apiKey: "test-key",
  model: "Cohere-rerank-v4.0-pro",
};
const candidates = [
  { id: "a", score: 0.8, payload: { title: "腾冲", content: "温泉" } },
  { id: "b", score: 0.6, payload: { title: "元阳", content: "梯田" } },
];

test("Cohere maps indices and scores while preserving original candidates", async (context) => {
  context.mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, config.url);
    assert.equal(options.headers["api-key"], config.apiKey);
    assert.equal(options.redirect, "error");
    assert.deepEqual(JSON.parse(options.body), {
      model: config.model, query: "哪里有梯田？",
      documents: ['title: "腾冲"\ncontent: "温泉"', 'title: "元阳"\ncontent: "梯田"'],
      top_n: 2,
    });
    return Response.json({ results: [
      { index: 1, relevance_score: 0.95 }, { index: 0, relevance_score: 0.2 },
    ] });
  });
  const result = await new CohereReranker(config).rerank("哪里有梯田？", candidates, 8);
  assert.deepEqual(result.map((row) => row.id), ["b", "a"]);
  assert.equal(result[0].rerankScore, 0.95);
  assert.equal(result[0].score, 0.6);
  assert.equal(candidates[1].rerankScore, undefined);
});

test("Cohere rejects invalid results without a fallback", async (context) => {
  const invalid = [
    [null, null],
    [], [{ index: 0, relevance_score: 0.8 }],
    [{ index: 0, relevance_score: 0.8 }, { index: 0, relevance_score: 0.7 }],
    [{ index: 2, relevance_score: 0.8 }, { index: 0, relevance_score: 0.7 }],
    [{ index: 0, relevance_score: 1.1 }, { index: 1, relevance_score: 0.7 }],
    [{ index: 0, relevance_score: "0.8" }, { index: 1, relevance_score: 0.7 }],
    [{ index: 0, relevance_score: 0.5 }, { index: 1, relevance_score: 0.9 }],
  ];
  const mock = context.mock.method(globalThis, "fetch");
  for (const results of invalid) {
    mock.mock.mockImplementation(async () => Response.json({ results }));
    await assert.rejects(new CohereReranker(config).rerank("梯田", candidates, 2), /invalid/);
  }
});

test("Cohere preserves rate limit retry time and does not expose upstream bodies", async (context) => {
  context.mock.method(globalThis, "fetch", async () => new Response("private upstream body", {
    status: 429, headers: { "retry-after": "93" },
  }));
  await assert.rejects(new CohereReranker(config).rerank("梯田", candidates, 2), (error) => {
    assert.equal(error.statusCode, 429);
    assert.equal(error.retryAfter, 93);
    assert.doesNotMatch(error.message, /private/);
    return true;
  });
});

test("Cohere handles empty candidates and rejects non-HTTPS endpoints", async () => {
  assert.deepEqual(await new CohereReranker(config).rerank("梯田", [], 2), []);
  assert.throws(() => new CohereReranker({ ...config, url: "http://example.test" }), /HTTPS/);
});

test("search API returns 429 with retry metadata after Cohere limits a request", async (context) => {
  const settings = { models: {}, rerank: config, source: {}, qdrant: {}, apiToken: "search-test" };
  const server = createServer(settings);
  context.after(() => { server.closeAllConnections(); server.close(); });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const realFetch = globalThis.fetch;
  const mock = context.mock.method(globalThis, "fetch", async (url, options) => {
    if (String(url).startsWith("http://127.0.0.1")) return realFetch(url, options);
    if (String(url).includes("embeddings")) return Response.json({ data: [{ index: 0, embedding: [0.1] }] });
    if (String(url).includes("points/query")) return Response.json({ result: { points: candidates } });
    return new Response("limited", { status: 429, headers: { "retry-after": "61" } });
  });
  settings.models.baseUrl = "https://embedding.test";
  settings.qdrant.url = "https://qdrant.test";
  const response = await fetch(`http://127.0.0.1:${server.address().port}/search`, {
    method: "POST", headers: { authorization: "Bearer search-test" },
    body: JSON.stringify({ question: "哪里有梯田？" }),
  });
  assert.equal(response.status, 429);
  assert.equal(response.headers.get("retry-after"), "61");
  assert.equal((await response.json()).retryAfterSeconds, 61);
  assert.equal(mock.mock.calls.filter((call) => String(call.arguments[0]).includes("chat/completions")).length, 0);
});
