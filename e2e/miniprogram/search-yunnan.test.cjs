const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../shop-mnp/packagesMall/search/search.vue'), 'utf8')
const corn = { goodsId: 260, goodsName: '白拇指玉米', goodsType: 'online',
  price: 29.9, vipPrice: 19.9, goodsCover: '/profile/corn.jpg', tags: '玉米,云南' }

function searchPage(api = {}) {
  const navigations = [], requests = [], notices = []
  const script = source.match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/import[^\n]+\n/g, '').replace('export default', 'module.exports =')
  const context = { module: {exports: {}}, console,
    getGoodsList: async params => { requests.push(params); return {data: [corn]} },
    getActivityList: async () => ({rows: []}), ...api,
    uni: {navigateTo: options => navigations.push(options.url),
      showToast: options => notices.push(options.title), showLoading() {}, hideLoading() {}} }
  vm.runInNewContext(script, context)
  const component = context.module.exports
  const page = component.data.call({$host: 'https://example.invalid'})
  for (const [name, method] of Object.entries(component.methods)) page[name] = method.bind(page)
  return {page, navigations, requests, notices}
}

test('Yunnan goods appear in search with their ordinary price', () => {
  const {page} = searchPage()
  const groups = page.buildResultGroups([corn], [], '玉米')
  assert.equal(groups.length, 1)
  assert.equal(groups[0].label, '云南好物')
  assert.equal(groups[0].items[0].id, 260)
  assert.equal(groups[0].items[0].priceText, '￥29.9')
  assert.equal(groups[0].items[0].image, '/profile/corn.jpg')
})

test('search retains all four groups and routes each result to its own detail page', () => {
  const {page, navigations} = searchPage()
  const groups = page.buildResultGroups([
    corn, {goodsId: 106, goodsType: 'hotel', goodsName: '旅居', price: 100},
    {goodsId: 52, goodsType: 'education', goodsName: '课程', price: 250},
    {goodsId: 999, goodsType: 'unknown', goodsName: '其他'}
  ], [{activityId: 9, activityName: '活动', isFree: 1}], '玉米')
  assert.equal(groups.length, 4)
  for (const group of groups) page.openResult(group.items[0])
  assert.deepEqual(navigations, [
    '/packagesMall/GoodsDetails/SojournGoodsDetails?id=106',
    '/packagesMall/GoodsDetails/GoodsDetails?id=260',
    '/packagesMall/Activity/detail/index?id=9',
    '/packagesMall/GoodsDetails/EducationGoodsDetails?id=52'
  ])
})

test('a search for corn shows results rather than the no-results state', async () => {
  const {page, requests} = searchPage()
  await page.doSearch('玉米')
  assert.equal(requests[0].goodsName, '玉米')
  assert.equal(requests[0].ignoreSite, true)
  assert.equal(page.showSearchResult, true)
  assert.equal(page.showSearchEmpty, false)
  assert.equal(page.searching, false)
  assert.equal(page.resultGroups[0].label, '云南好物')
})

test('empty results and unavailable services keep distinct states', async () => {
  const empty = searchPage({getGoodsList: async () => ({data: []})}).page
  await empty.doSearch('不存在的商品')
  assert.equal(empty.showSearchEmpty, true)
  assert.equal(empty.searchError, '')
  const failed = searchPage({
    getGoodsList: async () => { throw Error('offline') },
    getActivityList: async () => { throw Error('offline') }
  }).page
  await failed.doSearch('玉米')
  assert.equal(failed.showSearchEmpty, false)
  assert.equal(failed.searchError, '搜索服务暂不可用，请稍后重试')
})

test('search placeholder includes goods as a searchable category', () => {
  assert.match(source, /placeholder="搜索旅居、好物、活动和课程"/)
})

test('Yunnan results omit redundant source tags and per-card section labels', () => {
  const {page} = searchPage()
  const item = page.buildGoodsResult({...corn, tags: '云野集,云南好物,玉米'})
  assert.deepEqual(Array.from(item.tags), ['玉米'])
  assert.match(source, /class="result-type" v-if="item.type !== 'yunnan'"/)
})
