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

test('Yunnan goods stay out of search while the section is preparing', () => {
  const {page} = searchPage()
  const groups = page.buildResultGroups([corn], [], '玉米')
  assert.equal(groups.length, 0)
})

test('search retains the other three groups and their detail routes', () => {
  const {page, navigations} = searchPage()
  const groups = page.buildResultGroups([
    corn, {goodsId: 106, goodsType: 'hotel', goodsName: '旅居', price: 100},
    {goodsId: 52, goodsType: 'education', goodsName: '课程', price: 250},
    {goodsId: 999, goodsType: 'unknown', goodsName: '其他'}
  ], [{activityId: 9, activityName: '活动', isFree: 1}], '玉米')
  assert.equal(groups.length, 3)
  for (const group of groups) page.openResult(group.items[0])
  assert.deepEqual(navigations, [
    '/packagesMall/GoodsDetails/SojournGoodsDetails?id=106',
    '/packagesMall/Activity/detail/index?id=9',
    '/packagesMall/GoodsDetails/EducationGoodsDetails?id=52'
  ])
})

test('a search for corn does not display retail products during preparation', async () => {
  const {page, requests} = searchPage()
  await page.doSearch('玉米')
  assert.equal(requests[0].goodsName, '玉米')
  assert.equal(requests[0].ignoreSite, true)
  assert.equal(page.showSearchResult, false)
  assert.equal(page.showSearchEmpty, true)
  assert.equal(page.searching, false)
  assert.equal(page.resultGroups.length, 0)
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

test('search placeholder only advertises currently available sections', () => {
  assert.match(source, /placeholder="搜索旅居、活动和课程"/)
})

test('Yunnan results omit redundant source tags and per-card section labels', () => {
  const {page} = searchPage()
  const item = page.buildGoodsResult({...corn, tags: '云野集,云南好物,玉米'})
  assert.deepEqual(Array.from(item.tags), ['玉米'])
  assert.match(source, /class="result-type" v-if="item.type !== 'yunnan'"/)
})

test('all search categories use ordinary prices, not unsupported member prices', () => {
  const {page}=searchPage()
  assert.equal(page.buildGoodsResult({goodsType:'hotel',price:100,vipPrice:80}).priceText,'￥100')
  assert.equal(page.buildActivityResult({isFree:0,price:50,vipPrice:30}).priceText,'￥50')
})
