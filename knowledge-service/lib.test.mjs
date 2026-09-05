import test from "node:test";
import assert from "node:assert/strict";
import {
  applyRerankOrder,
  chunkDocument,
  lexicalTokens,
  pointId,
  sanitizeText,
  sparseVector,
  validateQuestion,
} from "./lib.mjs";

test("chunkDocument preserves content and adds overlap", () => {
  const chunks = chunkDocument("建水基地", `${"甲".repeat(180)}\n\n${"乙".repeat(180)}`, {
    maxChars: 240,
    overlap: 20,
  });
  assert.equal(chunks.length, 2);
  assert.match(chunks[0].text, /^建水基地/);
  assert.match(chunks[1].content, /^甲{20}/);
  assert.match(chunks[1].content, /乙{180}$/);
});

test("lexicalTokens includes Chinese unigrams and bigrams", () => {
  const tokens = lexicalTokens("云南旅居 Base-2026");
  assert.ok(tokens.includes("云"));
  assert.ok(tokens.includes("云南"));
  assert.ok(tokens.includes("旅居"));
  assert.ok(tokens.includes("base"));
  assert.ok(tokens.includes("2026"));
});

test("sparseVector is deterministic and sorted", () => {
  const left = sparseVector("建水 建水");
  const right = sparseVector("建水 建水");
  assert.deepEqual(left, right);
  assert.ok(left.indices.length > 0);
  assert.deepEqual(left.indices, [...left.indices].sort((a, b) => a - b));
  assert.equal(left.indices.length, left.values.length);
});

test("pointId creates stable UUID values", () => {
  const id = pointId("doc-token", 3);
  assert.equal(id, pointId("doc-token", 3));
  assert.match(id, /^[0-9a-f]{8}-[0-9a-f]{4}-5[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
  assert.notEqual(id, pointId("doc-token", 4));
});

test("validateQuestion enforces bounds", () => {
  assert.equal(validateQuestion("  建水住宿？ "), "建水住宿？");
  assert.throws(() => validateQuestion("a"));
  assert.throws(() => validateQuestion("问".repeat(501)));
});

test("applyRerankOrder rejects duplicates and unknown IDs", () => {
  const candidates = [{ id: "a" }, { id: "b" }, { id: "c" }];
  assert.deepEqual(applyRerankOrder(candidates, ["c", "x", "c", "a"], 2), [
    candidates[2],
    candidates[0],
  ]);
});

test("sanitizeText replaces unpaired surrogates before JSON transport", () => {
  const sanitized = sanitizeText(`正常\ud800内容\udc00和😀`);
  assert.equal(sanitized, "正常�内容�和😀");
  assert.doesNotMatch(JSON.stringify(sanitized), /\\ud[89a-f][0-9a-f]{2}/i);
});

test("chunkDocument does not split a valid surrogate pair at a chunk boundary", () => {
  const chunks = chunkDocument("边界测试", `${"甲".repeat(199)}😀${"乙".repeat(80)}`, {
    maxChars: 200,
    overlap: 20,
  });
  assert.ok(chunks.some((chunk) => chunk.content.includes("😀")));
  for (const chunk of chunks) {
    assert.doesNotMatch(JSON.stringify(chunk), /\\ud[89a-f][0-9a-f]{2}/i);
  }
});
