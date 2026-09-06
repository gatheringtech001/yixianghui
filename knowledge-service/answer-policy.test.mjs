import test from "node:test";
import assert from "node:assert/strict";
import { policyAnswer } from "./answer-policy.mjs";

const candidate = (content) => ({ id: "S1", content, source: { title: "测试基地" } });
test("inclusive ten-day clauses cannot promise a free cancellation at the boundary", () => {
  const r = policyAnswer("正好连住10天，提前4天取消能无损吗？", [candidate(
    "10天以内连住订单，入住前3天无损取消。\n10天以上连住订单，入住前5天无损取消。")]);
  assert.equal(r.reason, "ambiguous_policy_boundary");
  assert.match(r.answer, /不能确定/);
  assert.equal(r.sources[0].evidence.length, 2);
});
test("overlapping age brackets need explicit clarification, not a generic disclaimer", () => {
  const r = policyAnswer("孩子正好6周岁，不占床餐费多少？", [candidate(
    "儿童收费：3岁以下免费，3-6岁每天餐费10元，6岁以上不占床50元/天")]);
  assert.match(r.answer, /6.*边界/);
  assert.match(r.answer, /10元/);
  assert.match(r.answer, /50元/);
});
test("non-overlapping rules and ordinary ages still use normal answering", () => {
  assert.equal(policyAnswer("孩子正好6岁餐费多少？", [candidate("3-6岁10元，7岁以上50元")]), null);
  assert.equal(policyAnswer("孩子5岁餐费多少？", [candidate("3-6岁10元，6岁以上50元")]), null);
  assert.equal(policyAnswer("孩子正好6岁餐费多少？", [candidate("3-6岁（不含6岁）10元，6岁以上50元")]), null);
  assert.equal(policyAnswer("孩子正好六岁餐费多少？", [candidate("3-6岁10元，6岁以上50元")]).reason, "ambiguous_policy_boundary");
});
test("ordinary housekeeping fees cannot answer unknown pet fees", () => {
  const r = policyAnswer("这个基地能带宠物入住吗，清洁费100元？", [candidate("额外清扫收费50元")]);
  assert.equal(r.grounded, false);
  assert.deepEqual(r.sources, []);
  assert.match(r.answer, /不能.*保洁费/);
  assert.ok(!r.answer.includes("50元"));
  assert.equal(policyAnswer("宠物清洁费多少？", [candidate("可带宠物，清洁费一次100元")]), null);
});
