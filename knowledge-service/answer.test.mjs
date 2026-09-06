import test from "node:test";
import assert from "node:assert/strict";
import { answerQuestion } from "./answer.mjs";
import { createServer } from "./server.mjs";

const point = { id: "p1", payload: { title: "建水基地", content: "提供双人标间，套餐包含三餐。",
  source_url: "https://example.test/base", source_id: "base-1", source_type: "mysql_catalog",
  entity_id: "1", product_status: "1", snapshot_at: "2026-09-05T00:00:00Z" } };
const citations = [{ id: "S1", evidenceIds: [0] }];
const output = (text, refs = citations) => ({ grounded: true, citations: refs,
  statements: [{ text, sourceIndexes: refs.map((_, i) => i) }] });
const grounded = output("基地提供双人标间，包含三餐。");

function fixture(output = grounded, points = [point]) {
  const calls = { embedding: 0, retrieval: 0, luna: 0, rerank: 0 };
  const service = {
    models: { async embed(texts) { calls.embedding++; return texts.map(() => [0.1]); } },
    store: { async search(question, vector, limit) { calls.retrieval++; assert.equal(limit, 30); return points; },
      async searchScoped(q, v, options) { calls.retrieval++; return points.filter((p) =>
        (!options.titles.length || options.titles.includes(p.payload.title)) && (!options.kind || options.kind === p.payload.media?.kind)); } },
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
  assert.deepEqual(result.sources[0].evidence, [point.payload.content]);
  assert.equal(result.usage.inputTokens, 100);
});

test("unsupported answers return an explicit knowledge gap, not generated guesses", async () => {
  const { service } = fixture({ grounded: false, statements: [], citations: [] });
  const result = await answerQuestion(service, "火星基地的价格？");
  assert.equal(result.grounded, false);
  assert.match(result.answer, /资料不足/);
  assert.deepEqual(result.sources, []);
});

test("an explicit insufficient verdict never exposes unrelated citations or guesses", async () => {
  const { service } = fixture({ ...grounded, grounded: false });
  const result = await answerQuestion(service, "不存在的客房照片？");
  assert.equal(result.grounded, false);
  assert.deepEqual(result.sources, []);
  assert.match(result.answer, /资料不足/);
});

test("an empty retrieval does not call Luna", async () => {
  const { service, calls } = fixture(grounded, []);
  assert.equal((await answerQuestion(service, "不存在的资料？")).grounded, false);
  assert.equal(calls.luna, 0);
});

test("unknown sources, fabricated quotes and unmatched markers fail explicitly", async () => {
  for (const value of [
    output("三餐", [{ id: "S99", evidenceIds: [0] }]),
    output("三餐", [{ id: "S1", quote: "每天免费温泉" }]),
    output("包含三餐。[S2]"),
    output("三餐", []),
    output("三餐", [citations[0], citations[0]]),
  ]) {
    await assert.rejects(answerQuestion(fixture(value).service, "住宿餐饮？"), /citation|quote|sources/);
  }
});

test("citations require valid unique passage IDs from the indexed source", async () => {
  for (const evidenceIds of [undefined, [], [-1], [5], [0, 0], ["0"], [0.5]]) {
    const value = output("三餐", [{ id: "S1", evidenceIds }]);
    await assert.rejects(answerQuestion(fixture(value).service, "住宿餐饮？"), /evidence/);
  }
});

test("long Unicode source passages are bounded and extracted verbatim", async () => {
  const content = "客房🍀".repeat(200);
  const { service } = fixture(grounded, [{ ...point, payload: { ...point.payload, content } }]);
  const complete = service.reranker.complete;
  service.reranker.complete = async (body, event) => {
    const passages = JSON.parse(body.messages[1].content).candidates[0].passages;
    assert.equal(passages.map((p) => p.text).join(""), content);
    for (const passage of passages) {
      assert.ok(passage.text.length <= 350);
      assert.ok(content.includes(passage.text));
      assert.ok(passage.text.isWellFormed());
    }
    return complete(body, event);
  };
  const result = await answerQuestion(service, "客房信息？");
  assert.ok(content.includes(result.sources[0].evidence[0]));
});

test("an explicit numbered base excludes other entities but comparison retains both", async () => {
  const points = ["弥勒二号温泉基地", "普洱一号基地", "九蒸九晒滇黄精"].map((title, i) =>
    ({ ...point, id: String(i), payload: { ...point.payload, title } }));
  for (const [question, expected] of [["弥勒二号温泉基地的客房？", 1], ["对比弥勒二号和普洱一号基地客房", 2]]) {
    const { service } = fixture(grounded, points);
    const complete = service.reranker.complete;
    service.reranker.complete = async (body, event) => {
      assert.equal(JSON.parse(body.messages[1].content).candidates.length, expected);
      return complete(body, event);
    };
    await answerQuestion(service, question);
  }
});

test("paragraph citations are rendered by code and shared sources are merged", async () => {
  const value = { grounded: true, citations, statements: [
    { text: "提供双人标间。", sourceIndexes: [0] }, { text: "套餐包含三餐。", sourceIndexes: [0] },
  ] };
  const result = await answerQuestion(fixture(value).service, "房型餐食？");
  assert.equal(result.answer, "提供双人标间。[S1]\n\n套餐包含三餐。[S1]");
  assert.equal(result.sources.length, 1);
  assert.deepEqual(result.sources[0].evidence, [point.payload.content]);
});

test("source budget is global and paragraph indexes cannot cite missing evidence", async () => {
  for (const sourceIndexes of [[1], [-1], [0, 0], ["0"]]) {
    await assert.rejects(answerQuestion(fixture({ grounded: true, citations,
      statements: [{ text: "三餐", sourceIndexes }] }).service, "餐食资料？"), /citation/);
  }
  const { service } = fixture();
  const complete = service.reranker.complete;
  service.reranker.complete = async (body, event) => {
    const properties = body.response_format.json_schema.schema.properties;
    assert.equal(properties.citations.maxItems, 2);
    assert.deepEqual(properties.statements.items.properties.sourceIndexes.items.enum, [0, 1]);
    return complete(body, event);
  };
  await answerQuestion(service, "餐食资料？", 2);
});

test("ambiguous cancellation is answered from both clauses without asking the model to choose", async () => {
  const p = { ...point, payload: { ...point.payload,
    content: "10天以内连住，入住前3天无损取消。10天以上连住，入住前5天无损取消。" } };
  const { service, calls } = fixture(grounded, [p]);
  const result = await answerQuestion(service, "正好连住10天，提前4天取消可以无损吗？");
  assert.equal(result.reason, "ambiguous_policy_boundary");
  assert.equal(calls.luna, 0);
});

test("signed source links are not exposed to the model or quote output", async () => {
  const item = { ...point, payload: { ...point.payload,
    content: point.payload.content + " https://example.test/a.mp4?auth_key=PRIVATE_SIGNATURE" } };
  const { service } = fixture(grounded, [item]);
  const complete = service.reranker.complete;
  service.reranker.complete = async (body, event) => {
    assert.ok(!JSON.stringify(body).includes("PRIVATE_SIGNATURE"));
    return complete(body, event);
  };
  const result = await answerQuestion(service, "住宿餐饮？");
  assert.ok(!result.sources[0].quote.includes("PRIVATE_SIGNATURE"));
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
