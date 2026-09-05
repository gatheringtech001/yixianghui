import assert from "node:assert/strict";
import http from "node:http";
import { test } from "node:test";
import { createConsoleHandler } from "./console.mjs";

const origin = "https://gatheringtech.com";
const prefix = "/knowledge/console/";
async function fixture(t, query = async (body) => ({ answer: body.question, grounded: true, sources: [] }), openMedia) {
  const handle = createConsoleHandler({ token: "test-query-token", query, origin, openMedia });
  const server = http.createServer(async (req, res) => {
    if (!await handle(req, res)) { res.writeHead(404); res.end(); }
  });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  t.after(() => { server.closeAllConnections(); server.close(); });
  const url = `http://127.0.0.1:${server.address().port}${prefix}`;
  const request = (route, body, headers = {}) => fetch(url + route, {
    method: body === undefined ? "GET" : "POST",
    headers: { origin, "content-type": "application/json", ...headers },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const login = async () => {
    const response = await request("session", { token: "test-query-token" });
    assert.equal(response.status, 200);
    return response.headers.get("set-cookie");
  };
  return { request, login, url };
}

test("queries require a session and do not accept a Bearer shortcut", async (t) => {
  const { request } = await fixture(t);
  const response = await request("query", { question: "test", mode: "ask", limit: 3 }, { authorization: "Bearer test-query-token" });
  assert.equal(response.status, 401);
});
test("login exchanges token for a secure scoped cookie without exposing the token", async (t) => {
  const { login, request } = await fixture(t);
  const cookie = await login();
  assert.match(cookie, /HttpOnly/); assert.match(cookie, /Secure/); assert.match(cookie, /SameSite=Strict/);
  assert.match(cookie, /Path=\/knowledge\/console\//); assert.ok(!cookie.includes("test-query-token"));
  const response = await request("session", undefined, { cookie });
  assert.deepEqual(await response.json(), { authenticated: true });
});
test("cross-origin login and query are rejected", async (t) => {
  const { request, login } = await fixture(t);
  assert.equal((await request("session", { token: "test-query-token" }, { origin: "https://evil.example" })).status, 403);
  const cookie = await login();
  assert.equal((await request("query", { question: "test", mode: "ask", limit: 3 }, { cookie, origin: "https://evil.example" })).status, 403);
});
test("bad credentials are bounded by login throttling", async (t) => {
  const { request } = await fixture(t);
  for (let i = 0; i < 5; i++) assert.equal((await request("session", { token: "wrong" })).status, 401);
  assert.equal((await request("session", { token: "wrong" })).status, 429);
});
test("invalid questions and mode never reach the query service", async (t) => {
  let calls = 0;
  const { request, login } = await fixture(t, async () => { calls++; });
  const cookie = await login();
  for (const body of [null, [], { question: "x", mode: "ask", limit: 3 },
    { question: "test", mode: "admin", limit: 3 }, { question: "test", mode: "ask", limit: 11 }]) {
    assert.equal((await request("query", body, { cookie })).status, 400);
  }
  assert.equal(calls, 0);
});
test("successful query retains citations, media and adds measured latency", async (t) => {
  const source = { id: "S1", title: "Room", media: { kind: "video", startSeconds: 30, endSeconds: 60 } };
  const { request, login } = await fixture(t, async (body) => ({ question: body.question, answer: "Answer [S1]", grounded: true, sources: [source] }));
  const response = await request("query", { question: "Room video", mode: "ask", limit: 3 }, { cookie: await login() });
  const body = await response.json();
  assert.equal(response.status, 200); assert.deepEqual(body.sources, [source]);
  assert.equal(body.diagnostics.mode, "ask"); assert.ok(body.diagnostics.elapsedMs >= 0);
});
test("logout revokes the server session", async (t) => {
  const { request, login } = await fixture(t);
  const cookie = await login();
  assert.equal((await request("logout", {}, { cookie })).status, 200);
  assert.equal((await request("query", { question: "test", mode: "ask", limit: 3 }, { cookie })).status, 401);
});
test("upstream failures are explicit and never expose internal secrets", async (t) => {
  const { request, login } = await fixture(t, async () => { throw new Error("private-model-key"); });
  const response = await request("query", { question: "test", mode: "ask", limit: 3 }, { cookie: await login() });
  assert.equal(response.status, 502); assert.ok(!(await response.text()).includes("private-model-key"));
});
test("public shell has CSP and contains no injected token", async (t) => {
  const { request } = await fixture(t);
  const response = await request("");
  assert.equal(response.status, 200); assert.match(response.headers.get("content-security-policy"), /default-src 'self'/);
  assert.ok(!(await response.text()).includes("test-query-token"));
});
test("oversized bodies receive 413 and unsupported routes stay unavailable", async (t) => {
  const { request } = await fixture(t);
  assert.equal((await request("session", { token: "x".repeat(9000) })).status, 413);
  assert.equal((await request("admin/sync", {})).status, 404);
});
test("parallel queries in a session are rejected until the first finishes", async (t) => {
  let finish;
  let started;
  const entered = new Promise((resolve) => { started = resolve; });
  const { request, login } = await fixture(t, () => { started(); return new Promise((resolve) => { finish = resolve; }); });
  const cookie = await login();
  const body = { mode: "ask", question: "test", limit: 3 };
  const pending = request("query", body, { cookie });
  await entered;
  assert.equal((await request("query", body, { cookie })).status, 429);
  finish({ answer: "done" });
  assert.equal((await pending).status, 200);
});
test("only the session that retrieved media can preview it, including Range requests", async(t)=>{
  const token="QFHyb2GxUo0OHrxnA4AcREWVnyc";
  const calls=[];
  const {request,login}=await fixture(t,async()=>({results:[{sourceUrl:`https://vcnnjnb870d6.feishu.cn/file/${token}`,media:{kind:"video",fileToken:token}}]}),async(descriptor,options)=>{
    calls.push({descriptor,range:options.range});
    return new Response(new Uint8Array([0,1,2,3]),{status:206,headers:{"content-type":"video/mp4","content-length":"4","content-range":"bytes 0-3/100"}});
  });
  const cookie=await login();
  const result=await(await request("query",{mode:"search",question:"video",limit:1},{cookie})).json();
  const preview=result.results[0].media.previewUrl.slice(prefix.length);
  assert.ok(!preview.includes(token));
  assert.equal((await request(preview)).status,401);
  assert.equal((await request(preview,undefined,{cookie:await login()})).status,404);
  const response=await request(preview,undefined,{cookie,range:"bytes=0-3"});
  assert.equal(response.status,206); assert.equal(response.headers.get("content-range"),"bytes 0-3/100");
  assert.equal((await response.arrayBuffer()).byteLength,4);assert.equal(calls.length,1);
  await request("logout",{},{cookie});
  assert.equal((await request(preview,undefined,{cookie})).status,401);
});
test("external public URLs preview directly while private URLs remain links only", async(t)=>{
  const {request,login}=await fixture(t,async()=>({results:[
    {sourceType:"external_asset",sourceUrl:"https://example.com/a.jpg",media:{kind:"image",url:"https://example.com/a.jpg",access:"public"}},
    {sourceType:"external_asset",sourceUrl:"https://example.com/private.jpg",media:{kind:"image",url:"https://example.com/private.jpg",access:"restricted"}},
  ]}));
  const cookie=await login();
  const result=await(await request("query",{mode:"search",question:"image",limit:2},{cookie})).json();
  assert.equal(result.results[0].media.previewUrl,"https://example.com/a.jpg");
  assert.equal(result.results[0].media.previewMode,"external-public");
  assert.equal(result.results[1].media.previewUrl,undefined);
});
