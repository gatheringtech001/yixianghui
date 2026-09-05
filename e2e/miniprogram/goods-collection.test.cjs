const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')

function loadDetail(api = {}) {
  const file = path.resolve(__dirname, '../../shop-mnp/packagesMall/GoodsDetails/GoodsDetails.vue')
  const script = fs.readFileSync(file, 'utf8').match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/import\s+[\s\S]*?\s+from\s+['"][^'"]+['"];?/g, '')
    .replace('export default', 'module.exports =')
  const toasts = []
  const context = { module: { exports: {} }, sharePageMixin: {},
    uni: { getStorageSync: () => ({ userId: 1 }), showToast: value => toasts.push(value.title) },
    goodsCollectList: async () => ({ rows: [] }), ...api }
  vm.runInNewContext(script, context)
  const component = context.module.exports
  const page = { ...component.data.call({ $host: '' }), goodsId: 260, goodsDetail: { goodsId: 260 } }
  for (const [name, method] of Object.entries(component.methods)) page[name] = method.bind(page)
  return { page, toasts }
}

test('collect then uncollect immediately uses the returned record id', async () => {
  const deleted = []
  const { page } = loadDetail({
    goodsCollect: async () => ({ data: { collectId: 789 } }),
    deleteCollect: async data => { deleted.push(data.collectId) }
  })
  await page.onAttention()
  assert.equal(page.AttentionShow, 1)
  assert.equal(page.collectId, 789)
  await page.onAttention()
  assert.deepEqual(deleted, [789])
  assert.equal(page.AttentionShow, 0)
  assert.equal(page.collectId, null)
})

test('an empty collection response clears stale state', async () => {
  const { page } = loadDetail()
  page.AttentionShow = 1
  page.collectId = 789
  await page.getCollects()
  assert.equal(page.AttentionShow, 0)
  assert.equal(page.collectId, null)
})

test('failed uncollect keeps the selected state and shows the error', async () => {
  const { page, toasts } = loadDetail({ deleteCollect: async () => { throw new Error('网络断开') } })
  page.AttentionShow = 1
  page.collectId = 789
  await page.onAttention()
  assert.equal(page.AttentionShow, 1)
  assert.equal(page.collectId, 789)
  assert.ok(toasts.includes('网络断开'))
})

test('a second tap cannot submit while collection is pending', async () => {
  let finish
  let requests = 0
  const { page } = loadDetail({ goodsCollect: () => {
    requests++
    return new Promise(resolve => { finish = resolve })
  } })
  const pending = page.onAttention()
  await page.onAttention()
  assert.equal(requests, 1)
  finish({ data: { collectId: 789 } })
  await pending
  assert.equal(page.collectId, 789)
})
