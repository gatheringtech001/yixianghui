import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'
import { createRequire } from 'node:module'
const require = createRequire(new URL('../package.json', import.meta.url))
const QRCode = require('qrcode')
const source = readFileSync(new URL('../src/views/guanjiaLogin.vue', import.meta.url), 'utf8')
const script = source.split('<script>')[1].split('</script>')[0].replace(/^import .*$/gm, '').replace('export default', 'component =')
const ticket = 'a'.repeat(64), origin = 'https://shzxj.lk01.cn', scan = `${origin}/api/system/auth/guanjia/scan?ticket=${ticket}`
function fixture() {
  const calls = [], timers = [], tokens = [], nav = []
  let response = { code: 200, ticket, expires: Date.now() + 300000, url: scan }
  const context = { component: null, URLSearchParams, Date, navigator: { userAgent: 'Desktop' },
    location: { search: '', hash: '', pathname: '/guanjia-login', assign: url => nav.push(url), replace: url => nav.push(url) },
    history: { replaceState() {} }, setToken: t => tokens.push(t), QRCode,
    setTimeout: fn => { timers.push(fn); return timers.length }, clearTimeout() {},
    fetch: async (url, opts) => { calls.push({ url, opts }); return { ok: response.code === 200, json: async () => response } } }
  vm.runInNewContext(script, context)
  const c = context.component, state = c.data()
  for (const [k, fn] of Object.entries(c.methods)) state[k] = fn.bind(state)
  return { state, context, calls, timers, tokens, nav, c, reply: r => { response = r } }
}
test('native QR is rendered locally; requests bypass duplicate-body caching and carry browser proof cookies', async () => {
  const f = fixture(); await f.state.start()
  assert.match(f.state.image, /^data:image\/png;base64,/)
  assert.equal(f.state.ticket, ticket); assert.equal(f.timers.length, 1)
  const { url, opts } = f.calls[0]; assert.equal(url, '/api/system/auth/guanjia/qr')
  assert.equal(opts.credentials, 'same-origin'); assert.equal(opts.cache, 'no-store'); assert.equal(opts.method, 'POST')
  assert.ok(!source.includes('gatheringtech.com')); assert.ok(!source.includes('/exchange'))
})
test('only successful token response completes login; waiting keeps polling', async () => {
  const f = fixture(); await f.state.start(); f.reply({ code: 200, status: 'waiting' }); await f.state.poll(1)
  assert.equal(f.tokens.length, 0); f.reply({ code: 200, status: 'signed-in', token: 'test-token' }); await f.state.poll(1)
  assert.deepEqual(f.tokens, ['test-token']); assert.deepEqual(f.nav, ['/index']); assert.equal(f.state.ticket, '')
})
test('expiry and permission errors stop polling, without logging in', async () => {
  const f = fixture(); await f.state.start(); f.state.expires = 1; await f.state.poll(1)
  assert.equal(f.state.failed, true); assert.equal(f.tokens.length, 0)
  f.state.expires = Date.now() + 10000; f.reply({ code: 403, msg: '权限已变更' }); await f.state.poll(1)
  assert.equal(f.state.message, '权限已变更'); assert.equal(f.tokens.length, 0)
})
test('refresh and leaving cancel owned ticket; stale responses never sign in', async () => {
  const f = fixture(); await f.state.start(); await f.state.start()
  assert.ok(f.calls.some(c => c.url.endsWith('/cancel')))
  f.reply({ code: 200, status: 'signed-in', token: 'stale' }); await f.state.poll(1); assert.equal(f.tokens.length, 0)
  f.c.beforeDestroy.call(f.state); assert.equal(f.state.disposed, true); await f.state.poll(2); assert.equal(f.tokens.length, 0)
})
test('same-device WeChat authorization stays native and can resume browser-bound ticket', async () => {
  const f = fixture(); f.context.navigator.userAgent = 'MicroMessenger'; await f.state.start(); assert.deepEqual(f.nav, [scan]); assert.equal(f.state.image, '')
  const g = fixture(); g.context.location.search = '?ticket=' + ticket; g.reply({ code: 200, status: 'waiting' });g.c.created.call(g.state)
  await Promise.resolve(); await Promise.resolve(); assert.equal(g.state.ticket, ticket); assert.equal(g.calls[0].url, '/api/system/auth/guanjia/poll')
})
