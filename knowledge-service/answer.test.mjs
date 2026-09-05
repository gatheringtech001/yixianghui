import test from "node:test";
import assert from "node:assert/strict";
import { answerQuestion } from "./answer.mjs";
import { createServer } from "./server.mjs";

const point = { id: "p1", payload: { title: "建水基地", content: "提供双人标间，套餐包含三餐。",
  source_url: "https://example.test/base", source_id: "base-1", source_type: "mysql_catalog",
  entity_id: "1", product_status: "1", snapshot_at: "2026-09-05T00:00:00Z" } };
const grounded = { answer: "基地提供双人标间，包含三餐。[S1]", grounded: true,
  citations: [{ id: "S1" }] };

function fixture(output = grounded, points = [point]) {
  const calls = { embedding: 0, retrieval: 0, luna: 0, rerank: 0 };
  const service = {
    models: { async embed() { calls.embedding++; return [[0.1]]; } },
    store: { async search(question, vector, limit) { calls.retrieval++; assert.equal(limit, 30); return points; } },
    reranker: { config: { model: "gpt-5.6-luna" },
      async rerank() { calls.rerank++; throw new Error("Separate rerank must not run"); },
      async complete(body, event) {
        calls.luna++;
        assert.equal(event, "luna_answer");
        assert.equal(body.response_format.json_schema.strict, true);
        assert.equal(JSON.parse(body.messages[1].content).candidates[0].status, "1");
        return { output, usage: { prompt_tokens: 100, completion_tokens: 20 } };
      },
    },
  };
  return { service, calls };
}

test("combined answering invokes Luna exactly once with source-grounded citations", async () => {
  const { service, calls } = fixture();
  const result = await answerQuestion(service, "基地提供什么住宿餐饮？");
  assert.deepEqual(calls, { embedding: 1, retrieval: 1, luna: 1, rerank: 0 });
  assert.equal(result.grounded, true);
  assert.equal(result.sources[0].url, point.payload.source_url);
  assert.equal(result.sources[0].entityId, "1");
  assert.equal(result.sources[0].quote, point.payload.content);
  assert.equal(result.usage.inputTokens, 100);
});

test("unsupported answers return an explicit knowledge gap, not generated guesses", async () => {
  const { service } = fixture({ answer: "Untrusted guess", grounded: false, citations: [] });
  const result = await answerQuestion(service, "火星基地的价格？");
  assert.equal(result.grounded, false);
  assert.match(result.answer, /资料不足/);
  assert.deepEqual(result.sources, []);
});

test("an empty retrieval does not call Luna", async () => {
  const { service, calls } = fixture(grounded, []);
  assert.equal((await answerQuestion(service, "不存在的资料？")).grounded, false);
  assert.equal(calls.luna, 0);
});

test("unknown sources, fabricated quotes and unmatched markers fail explicitly", async () => {
  for (const output of [
    { ...grounded, citations: [{ id: "S99", quote: "三餐" }] },
    { ...grounded, citations: [{ id: "S1", quote: "每天免费温泉" }] },
    { ...grounded, answer: "包含三餐。[S2]" },
    { ...grounded, citations: [] },
    { ...grounded, citations: [grounded.citations[0], grounded.citations[0]] },
  ]) {
    await assert.rejects(answerQuestion(fixture(output).service, "住宿餐饮？"), /citation|quote|sources/);
  }
});

test("question and source limit validation happen before API calls", async () => {
  const { service, calls } = fixture();
  for (const value of [null, {}, "", "问".repeat(501)]) await assert.rejects(answerQuestion(service, value));
  for (const limit of [0, 11, "5", 2.5]) await assert.rejects(answerQuestion(service, "住宿餐饮？", limit));
  assert.equal(calls.embedding, 0);
});

test("ask HTTP endpoint authenticates, handles invalid input and uses one model call", async (context) => {
  const settings = { models: { baseUrl: "https://embedding.test" },
    rerank: { url: "https://luna.test/chat", model: "gpt-5.6-luna", apiKey: "test" },
    source: {}, qdrant: { url: "https://qdrant.test" }, apiToken: "search-test" };
  const server = createServer(settings);
  context.after(() => { server.closeAllConnections(); server.close(); });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const realFetch = globalThis.fetch;
  let modelCalls = 0;
  context.mock.method(globalThis, "fetch", async (url, options) => {
    if (String(url).startsWith("http://127.0.0.1")) return realFetch(url, options);
    if (String(url).includes("embeddings")) return Response.json({ data: [{ index: 0, embedding: [0.1] }] });
    if (String(url).includes("points/query")) return Response.json({ result: { points: [point] } });
    modelCalls++;
    return Response.json({ choices: [{ finish_reason: "stop", message: { content: JSON.stringify(grounded) } }] });
  });
  const ask = (body, token = "search-test") => fetch(`http://127.0.0.1:${server.address().port}/ask`, {
    method: "POST", headers: { authorization: `Bearer ${token}` }, body,
  });
  assert.equal((await ask("{}", "wrong")).status, 401);
  for (const body of ["{", "null", "[]", '{"question":1}', '{"question":"住宿？","maxSources":0}']) {
    assert.equal((await ask(body)).status, 400);
  }
  const response = await ask('{"question":"住宿餐饮？"}');
  assert.equal(response.status, 200);
  assert.equal((await response.json()).grounded, true);
  assert.equal(modelCalls, 1);
});
