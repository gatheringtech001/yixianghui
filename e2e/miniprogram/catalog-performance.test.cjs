const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const loadPage = require('./vue-page-harness.cjs')
const read = file => fs.readFileSync(path.resolve(__dirname, '../../shop-mnp', file), 'utf8')
const importSource = source => import('data:text/javascript;base64,' + Buffer.from(source).toString('base64'))

test('catalog cache coalesces requests, expires and isolates mutable responses', async () => {
  const {createCatalogCache} = await importSource(read('utils/catalogCache.js'))
  let clock = 0, calls = 0
  const cache = createCatalogCache({ttl: 60, now: () => clock})
  const loader = async () => { calls++; return {data: [{id: 1}]} }
  const [a, b] = await Promise.all([cache.get('goods', loader), cache.get('goods', loader)])
  assert.equal(calls, 1)
  a.data[0].id = 2
  assert.equal(b.data[0].id, 1)
  assert.equal((await cache.get('goods', loader)).data[0].id, 1)
  clock = 61
  await cache.get('goods', loader)
  assert.equal(calls, 2)
  cache.clear()
  await cache.get('goods', loader)
  assert.equal(calls, 3)
})

test('failed loads are retried and clearing cache rejects obsolete pending writes', async () => {
  const {createCatalogCache} = await importSource(read('utils/catalogCache.js'))
  const cache = createCatalogCache()
  await assert.rejects(cache.get('x', async () => {throw Error('offline')}))
  assert.equal(await cache.get('x', async () => 3), 3)
  let resolve
  const pending = cache.get('slow', () => new Promise(r => {resolve = r}))
  await Promise.resolve()
  cache.clear()
  assert.equal(await cache.get('slow', async () => 5), 5)
  resolve(4)
  await pending
  assert.equal(await cache.get('slow', async () => 6), 5)
})

test('card images use thumbnails with original fallback; static/third-party URLs stay intact', async () => {
  const source = read('utils/catalogImage.js').replace(/import thumbnails[^\n]+/, "const thumbnails = {'/profile/upload/a.jpg': '/profile/upload/catalog-thumbs-v1/hash.jpg'}")
  const {catalogImageUrl} = await importSource(source)
  const host = 'https://example.invalid/api'
  assert.equal(catalogImageUrl('/profile/upload/a.jpg', host), host + '/profile/upload/catalog-thumbs-v1/hash.jpg')
  assert.equal(catalogImageUrl(host + '/profile/upload/a.jpg', host), host + '/profile/upload/catalog-thumbs-v1/hash.jpg')
  assert.equal(catalogImageUrl('/profile/upload/a.jpg', host, {'/profile/upload/a.jpg': true}), host + '/profile/upload/a.jpg')
  assert.equal(catalogImageUrl('/static/icon.png', host), '/static/icon.png')
  assert.equal(catalogImageUrl('https://other.invalid/a.jpg', host), 'https://other.invalid/a.jpg')
  assert.equal(catalogImageUrl('', host), '')
})

function service(dependencies = {}) {
  const result = loadPage('pages/classify/classify.vue', {
    TabBar: {}, AuthProfilePopup: {}, sharePageMixin: {},
    isVisibleCatalogGoods: () => true, catalogImageKey: path => path,
    catalogImageUrl: path => path,
    ...dependencies
  })
  result.page.setBodyScrollHeight = () => {}
  result.page.buildGoodsListItem = row => row
  result.page.$set = (target, key, value) => { target[key] = value }
  result.page.navbarList = [{categoryId: 25, categoryName: '全国旅居'}, {categoryId: 58, categoryName: '老年教育'}]
  result.page.navbarSelect = 58
  return result
}

test('service initially renders 12 cards, reveals all cards incrementally and resets on category change', () => {
  const {page, source} = service()
  page.goodsList = Array.from({length:71}, (_, goodsId) => ({goodsId}))
  assert.equal(page.visibleGoodsList.length, 12)
  page.loadMoreCards()
  assert.equal(page.visibleGoodsList.length, 24)
  for (let i=0; i<4; i++) page.loadMoreCards()
  assert.equal(page.visibleGoodsList.length, 71)
  page.loadContentList = () => {}
  page.$nextTick = fn => fn()
  page.menuFn({categoryId: 1})
  assert.equal(page.visibleCardCount, 12)
  assert.match(source, /@scrolltolower="loadMoreCards"/)
  assert.equal((source.match(/mode="aspectFill" lazy-load/g) || []).length, 2)
})

test('slow old service requests cannot overwrite a new category', async () => {
  const resolvers = []
  const {page} = service({getGoodsList: () => new Promise(r => resolvers.push(r))})
  page.getGoodsListFn()
  page.goodsCatrgorySelect = 60
  page.getGoodsListFn()
  resolvers[1]({data: [{goodsId: 2}]})
  await new Promise(r => setImmediate(r))
  resolvers[0]({data: [{goodsId: 1}]})
  await new Promise(r => setImmediate(r))
  assert.equal(page.goodsList[0].goodsId, 2)
})

test('home city data is not blocked by slow course recommendations', async () => {
  let finishRecommendations
  const {page} = loadPage('pages/home/home.vue', {
    TabBar: {}, AuthProfilePopup: {}, sharePageMixin: {},
    getBannerList: async () => ({data: []}), getGoodsList: async () => ({data: []}),
    buildHotCityCards: () => [{adName: '城市'}]
  })
  page.getClsList = async () => [{categoryId: 25, categoryName: '全国旅居'}]
  page.loadRecommendations = () => new Promise(r => {finishRecommendations = r})
  let initialized = false
  page.initCitySelection = () => {initialized = true}
  const pending = page.gethotCardList()
  await new Promise(r => setImmediate(r))
  assert.equal(initialized, true)
  finishRecommendations()
  await pending
  assert.match(read('pages/home/home.vue'), /clearCatalogCache\(\)/)
})

test('only public lists use catalog cache; detail and checkout APIs are untouched', () => {
  const source = read('utils/catalogRequests.js')
  assert.doesNotMatch(source, /getGoodsInfo|getGoodsSku|pay|Order|Coupon|userInfo|token/)
  assert.match(source, /deptId/)
  assert.match(read('pages/home/home.vue'), /product-cover[^>]+lazy-load/)
})

test('legacy Yunnan category name still opens only the preparing mask and never requests retail goods', () => {
  const {page} = service()
  page.navbarList.push({categoryId: 33, categoryName: '云野好物'})
  page.$nextTick = fn => fn()
  page.switchToYunnanGoods()
  assert.equal(page.isYunnanPreparing, true)
  assert.equal(page.goodsList.length, 0)
})
