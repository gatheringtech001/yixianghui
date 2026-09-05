import test from "node:test";
import assert from "node:assert/strict";
import { LunaReranker } from "./luna.mjs";
import { createServer } from "./server.mjs";

const config = { url: "https://example.test/openai/v1/chat/completions", apiKey: "test-key", model: "gpt-5.6-luna" };
const candidates = [
  { id: "a", score: 0.8, payload: { title: "腾冲", content: "温泉" } },
  { id: "b", score: 0.6, payload: { title: "元阳", content: "梯田", product_status: "0" } },
];
const answer = (order) => Response.json({
  choices: [{ finish_reason: "stop", message: { content: JSON.stringify({ order }) } }],
});

test("Luna requests strict ranking IDs and preserves retrieval scores without invented relevance scores", async (context) => {
  context.mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, config.url);
    assert.equal(options.headers["api-key"], config.apiKey);
    assert.equal(options.redirect, "error");
    const body = JSON.parse(options.body);
    assert.equal(body.model, "gpt-5.6-luna");
    assert.equal(body.response_format.json_schema.strict, true);
    assert.deepEqual(body.response_format.json_schema.schema.properties.order.items.enum, ["a", "b"]);
    const request = JSON.parse(body.messages[1].content);
    assert.equal(request.result_count, 2);
    assert.equal(request.candidates[1].status, "0");
    return answer(["b", "a"]);
  });
  const result = await new LunaReranker(config).rerank("哪里有梯田？", candidates, 8);
  assert.deepEqual(result.map((row) => row.id), ["b", "a"]);
  assert.equal(result[0].rerankScore, null);
  assert.equal(result[0].score, 0.6);
  assert.equal(candidates[1].rerankScore, undefined);
});

test("Luna rejects duplicate, missing and unknown IDs", async (context) => {
  const mock = context.mock.method(globalThis, "fetch");
  for (const order of [null, [], ["a"], ["a", "a"], ["x", "a"], [0, 1]]) {
    mock.mock.mockImplementation(async () => answer(order));
    await assert.rejects(new LunaReranker(config).rerank("梯田", candidates, 2), /invalid ranking/);
  }
});

test("Luna fails on truncated, refused or non-JSON responses", async (context) => {
  const mock = context.mock.method(globalThis, "fetch");
  for (const choice of [
    { finish_reason: "length", message: { content: '{"order":["a","b"]}' } },
    { finish_reason: "stop", message: { refusal: "refused" } },
    { finish_reason: "stop", message: { content: "not JSON" } },
  ]) {
    mock.mock.mockImplementation(async () => Response.json({ choices: [choice] }));
    await assert.rejects(new LunaReranker(config).rerank("梯田", candidates, 2), /structured output/);
  }
});

test("Luna handles empty candidates and requires HTTPS", async () => {
  assert.deepEqual(await new LunaReranker(config).rerank("梯田", [], 2), []);
  assert.throws(() => new LunaReranker({ ...config, url: "http://example.test" }), /HTTPS/);
});

test("search API uses Luna and returns rank-only metadata or explicit rate limits", async (context) => {
  const settings = { models: { baseUrl: "https://embedding.test" }, rerank: config,
    source: {}, qdrant: { url: "https://qdrant.test" }, apiToken: "search-test" };
  const server = createServer(settings);
  context.after(() => { server.closeAllConnections(); server.close(); });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const realFetch = globalThis.fetch;
  let limited = false;
  context.mock.method(globalThis, "fetch", async (url, options) => {
    if (String(url).startsWith("http://127.0.0.1")) return realFetch(url, options);
    if (String(url).includes("embeddings")) return Response.json({ data: [{ index: 0, embedding: [0.1] }] });
    if (String(url).includes("points/query")) return Response.json({ result: { points: candidates } });
    assert.equal(url, config.url);
    return limited ? new Response("private upstream body", { status: 429, headers: { "retry-after": "61" } }) : answer(["b", "a"]);
  });
  const query = () => fetch(`http://127.0.0.1:${server.address().port}/search`, {
    method: "POST", headers: { authorization: "Bearer search-test" },
    body: JSON.stringify({ question: "哪里有梯田？" }),
  });
  const success = await query();
  assert.equal(success.status, 200);
  const data = await success.json();
  assert.equal(data.rerankModel, "gpt-5.6-luna");
  assert.equal(data.scoreType, "rank_only");
  assert.equal(data.results[0].score, null);
  assert.equal(data.results[0].retrievalScore, 0.6);
  limited = true;
  const response = await query();
  assert.equal(response.status, 429);
  assert.equal(response.headers.get("retry-after"), "61");
  const error = await response.json();
  assert.equal(error.retryAfterSeconds, 61);
  assert.doesNotMatch(error.error, /private/);
});
