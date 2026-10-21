const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const loadPage = require('./vue-page-harness.cjs')
const read = file => fs.readFileSync(path.resolve(__dirname, '../../shop-mnp', file), 'utf8')
const importSource = source => import('data:text/javascript;base64,' + Buffer.from(source).toString('base64'))

function service(deps = {}) {
  const result = loadPage('pages/classify/classify.vue', {
    TabBar: {}, AuthProfilePopup: {}, sharePageMixin: {},
    isVisibleCatalogGoods: () => true, ...deps
  })
  Object.assign(result.page, {
    navbarList: [{categoryId:58, categoryName:'老年教育'}, {categoryId:32, linkType:'activity'}, {categoryId:33, categoryName:'云南好物'}],
    navbarSelect:58, goodsCatrgoryList:[{categoryId:0}],
    setBodyScrollHeight() {}, buildGoodsListItem: row => row,
    $nextTick: fn => fn(), syncNavbarListFromCache() {}, syncSiteDisplay() {}, brandLogoUrl:'loaded'
  })
  return result
}

test('service errors never masquerade as empty results; retry recovers and failed refresh keeps old rows', async () => {
  let fail = true
  const {page, source} = service({getGoodsList: async () => {
    if (fail) throw Error('offline')
    return {data:[{goodsId:1, paidSignupCount:3}]}
  }})
  await page.loadContentList()
  assert.match(page.contentError, /加载失败/)
  assert.equal(page.loading, false)
  assert.equal(page.goodsList.length, 0)
  assert.equal((source.match(/!loading && !contentError && isListEmpty/g) || []).length, 3)
  assert.match(source, /class="retry-button" @click="loadContentList"/)
  fail = false
  await page.loadContentList()
  assert.equal(page.contentError, '')
  fail = true
  await page.loadContentList()
  assert.match(page.contentError, /上次内容/)
  assert.equal(page.goodsList[0].paidSignupCount, 3)
})

test('activity retry keeps old rows and rejects a late error from a different filter', async () => {
  let reject
  const {page} = service({getActivityList: () => new Promise((_, r) => {reject = r})})
  page.navbarSelect = 32
  page.activityList = [{activityId:1}]
  const pending = page.loadContentList()
  page.contentRequestId++
  page.contentError = 'current filter'
  reject(Error('obsolete'))
  await pending
  assert.equal(page.contentError, 'current filter')
  assert.equal(page.activityList.length, 1)
})

test('populated course and activity tabs refresh on returning, but preparing tab never fetches goods', async () => {
  const options = []
  const {page, component} = service({
    bindPageAuthPopup() {}, uni:{getStorageSync:()=>''},
    getGoodsList: async (_, o) => {options.push(o); return {data:[{goodsId:1,paidSignupCount:4}]}},
    getActivityList: async (_, o) => {options.push(o); return {rows:[{activityId:1,signCount:5}]}}
  })
  page.goodsList = [{goodsId:1,paidSignupCount:3}]
  component.onShow.call(page)
  await new Promise(r=>setImmediate(r))
  assert.equal(page.goodsList[0].paidSignupCount, 4)
  page.navbarSelect = 32
  page.activityList = [{activityId:1,signCount:4}]
  component.onShow.call(page)
  await new Promise(r=>setImmediate(r))
  assert.equal(page.activityList[0].signCount, 5)
  assert.ok(options.every(o => o.refresh))
  page.navbarSelect = 33
  page.goodsList = []
  component.onShow.call(page)
  await new Promise(r=>setImmediate(r))
  assert.equal(options.length, 2)
})

test('a pending service request is not duplicated on show', () => {
  let calls = 0
  const {page, component} = service({bindPageAuthPopup() {}, uni:{getStorageSync:()=>''}})
  page.goodsCatrgoryList = []
  page.loading = true
  page.getGoodsCatrgoryFn = () => {calls++}
  component.onShow.call(page)
  assert.equal(calls, 0)
})

test('targeted catalog refresh bypasses only that key and fences off obsolete responses', async () => {
  const {createCatalogCache} = await importSource(read('utils/catalogCache.js'))
  const cache = createCatalogCache()
  let complete
  const pending = cache.get('course', () => new Promise(r=>{complete = r}))
  await Promise.resolve()
  await cache.get('travel', async()=>9)
  cache.invalidate('course')
  assert.equal(await cache.get('course', async()=>5), 5)
  complete(3)
  await pending
  assert.equal(await cache.get('course', async()=>7), 5)
  assert.equal(await cache.get('travel', async()=>0), 9)
})

function requestHarness(token = 'expired') {
  const storage = new Map(['token','userInfo','userData','userCard','consultant','setTokenTime'].map(key=>[key, key==='token'?token:'cached']))
  storage.set('SearchRecordArr', 'keep')
  const requests = [], notices = []
  const uni = {getStorageSync:key=>storage.get(key), removeStorageSync:key=>storage.delete(key),
    request:config=>requests.push(config), showToast:config=>notices.push(config.title)}
  const context = {module:{exports:{}}, uni, console, BaseUrl:{publicUrl:'https://example.invalid/api'}}
  const helper = read('utils/sessionStorage.js').replace('export function', 'function')
  const source = read('api/request.js').replace(/import[^\n]+\n/g, '').replace('export default function', 'function')
  vm.runInNewContext(helper + '\n' + source + '\nmodule.exports = request', context)
  return {request:context.module.exports, requests, storage, notices}
}

for (const response of [{statusCode:401,data:{msg:'Unauthorized'}}, {statusCode:200,data:{code:401}}, {statusCode:200,data:{code:'401'}}]) {
  test(`HTTP ${response.statusCode} / code ${response.data.code} expires the whole account cache and allows public browsing`, async () => {
    const {request, requests, storage, notices} = requestHarness()
    const failed = request('/private', 'GET')
    const rejection = assert.rejects(failed, error=>error.code === 401 && /重新登录/.test(error.message))
    requests[0].success(response)
    await rejection
    assert.deepEqual([...storage.keys()], ['SearchRecordArr'])
    assert.equal(notices.length, 1)
    const publicRequest = request('/public', 'GET')
    assert.equal(requests[1].header.Authorization, '')
    requests[1].success({statusCode:200,data:{code:200,data:[]}})
    await publicRequest
  })
}

test('parallel expired requests notify once and late old-session responses cannot clear a new login', async () => {
  const h = requestHarness()
  const pending = [h.request('/one','GET'), h.request('/two','GET')]
  const settled = Promise.allSettled(pending)
  h.requests.forEach(config=>config.success({statusCode:401}))
  await settled
  assert.equal(h.notices.length, 1)
  h.storage.set('token','old')
  const old = h.request('/old','GET')
  const rejected = assert.rejects(old)
  h.storage.set('token','new')
  h.storage.set('userInfo','new-profile')
  h.requests[2].success({statusCode:401})
  await rejected
  assert.equal(h.storage.get('token'), 'new')
  assert.equal(h.storage.get('userInfo'), 'new-profile')
})

test('course signup reuses the auth popup and continues only after login; cancellation never navigates', () => {
  let authorize
  const routes = []
  const {page, source} = loadPage('packagesMall/GoodsDetails/EducationGoodsDetails.vue', {
    AuthProfilePopup:{}, sharePageMixin:{}, runWithAuth:(_, callback)=>{authorize=callback},
    uni:{navigateTo:o=>routes.push(o.url)}
  })
  page.courseData={id:279}
  page.signupPhase='open'
  page.applyCourse()
  assert.equal(routes.length, 0)
  authorize(false)
  assert.equal(routes.length, 0)
  page.applyCourse()
  authorize(true)
  assert.deepEqual(routes,['/packagesMall/ConfirmOrder/EducationConfirmOrder?id=279'])
  const apply = source.slice(source.indexOf('applyCourse()'))
  assert.doesNotMatch(apply, /removeStorageSync|setTimeout/)
})

test('search thumbnails fall back to original and then placeholder, without affecting other cards', () => {
  const {page, source} = loadPage('packagesMall/search/search.vue', {
    catalogImageKey:path=>path,
    catalogImageUrl:(path,host,failed={})=>host+(failed[path]?path:'/thumb'+path)
  })
  page.$set = (target,key,value)=>{target[key]=value}
  assert.equal(page.imageUrl('/one'), page.host+'/thumb/one')
  page.onImageError('/one')
  assert.equal(page.imageUrl('/one'), page.host+'/one')
  assert.equal(page.failedOriginalImages['/one'], undefined)
  page.onImageError('/one')
  assert.equal(page.failedOriginalImages['/one'], true)
  assert.equal(page.imageUrl('/two'), page.host+'/thumb/two')
  assert.match(source, /lazy-load @error="onImageError/)
})

test('order list hides long course specifications and contact data without changing detail data', () => {
  const {page, source} = loadPage('packagesMall/MyOrderList/MyOrderList.vue', {getOrderProductSpec:()=> '房型'})
  const order = {contactName:'测试',contactPhone:'13800138000'}
  const goods = {goodsType:'education',specifications:'完整的报名说明',educationExt:{courseTime:'每周一'}}
  const before = JSON.stringify({order,goods})
  assert.equal(page.getProductSpec(order,goods), '')
  assert.equal(page.getProductSpec(order,{goodsType:'hotel'}), '房型')
  assert.equal(JSON.stringify({order,goods}), before)
  assert.doesNotMatch(source.split('<script>')[0], /contactPhone|contactName|teacherName|coursePlace/)
  assert.match(read('packagesMall/OrderDetails/OrderDetails.vue'), /contactPhone/)
})

test('settings service label matches its destination; existing detail and service buttons reach 44px', () => {
  const routes = []
  const {page, source} = loadPage('packagesPublic/Setting/Setting.vue', {AuthProfilePopup:{}, uni:{switchTab:o=>routes.push(o.url)}})
  page.onSetting('service')
  assert.deepEqual(routes, ['/pages/MembersOpened/MembersOpened'])
  assert.match(source, /客服中心/)
  assert.doesNotMatch(source, /逸享荟会员|查看会员权益/)
  assert.match(read('packagesMall/GoodsDetails/EducationGoodsDetails.vue'), /eyebrow: '芳华学院'/)
  for (const file of ['pages/classify/classify.scss', 'packagesMall/GoodsDetails/EducationGoodsDetails.scss', 'packagesMall/GoodsDetails/SojournGoodsDetails.scss', 'packagesMall/Activity/detail/index.scss']) {
    assert.match(read(file), /min-height: 44px/, file)
  }
})
