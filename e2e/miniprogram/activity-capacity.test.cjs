const test = require('node:test')
const assert = require('node:assert/strict')
const loadPage = require('./vue-page-harness.cjs')

function detail() {
  const routes = []
  const h = loadPage('packagesMall/Activity/detail/index.vue', {
    sharePageMixin: {}, AuthProfilePopup: {}, getActivityPhase: () => 'applying',
    uni: {getStorageSync: () => ({nickName: '测试用户'}), navigateTo: o => routes.push(o.url)}
  })
  return {...h, routes}
}

test('activity cards show actual enrollments and explicitly mark unlimited capacity', () => {
  const {page, source} = loadPage('pages/classify/classify.vue', {
    TabBar: {}, AuthProfilePopup: {}, sharePageMixin: {}
  })
  assert.match(source, /已报人数/)
  assert.match(source, /activitySignupText\(item\)/)
  for (const maxCount of [0, '0', null, undefined, '', -1, 'invalid']) {
    assert.equal(page.activitySignupText({signcount: 5, maxCount}), '5 人 · 不限人数')
    assert.equal(page.activitySignupText({signCount: 0, maxCount}), '0 人 · 不限人数')
  }
  assert.equal(page.activitySignupText({signCount: '5', maxCount: '20'}), '5 / 20 人')
})

test('unlimited details hide the capacity progress bar and never block empty activities as full', () => {
  const {page, source, routes} = detail()
  assert.match(source, /人 · 不限人数/)
  assert.match(source, /class="progress" v-if="activityMaxCount"/)
  assert.match(source, /disabled: activityFull/)
  for (const maxCount of [0, '0', null, undefined, '', -1, 'invalid']) {
    for (const signCount of [0, 5]) {
      page.detailInfo = {activityId: 146, maxCount, signCount}
      assert.equal(page.activitySignCount, signCount)
      assert.equal(page.activityMaxCount, 0)
      assert.equal(page.activityFull, false)
      assert.equal(page.enrollRatio, '0%')
      page.apply()
      assert.equal(routes.at(-1), '/packagesMall/ConfirmOrder/ActivityConfirmOrder?id=146')
    }
  }
  assert.equal(routes.length, 14)
})

test('finite activities retain progress and reject full or overfull signups', () => {
  const {page, routes} = detail()
  page.detailInfo = {activityId: 146, maxCount: '10', signcount: '5'}
  assert.equal(page.enrollRatio, '50%')
  assert.equal(page.activityFull, false)
  page.apply()
  assert.equal(routes.length, 1)
  for (const signCount of [10, 11]) {
    page.detailInfo = {activityId: 146, maxCount: 10, signCount}
    assert.equal(page.activityFull, true)
    assert.equal(page.enrollRatio, '100%')
    page.apply()
    assert.equal(routes.length, 1)
  }
})

test('unlimited signup forms accept numeric and string zero limits consistently', () => {
  const {page, source} = loadPage('packagesMall/ConfirmOrder/ActivityConfirmOrder.vue', {
    getActivityPhase: () => 'applying'
  })
  assert.match(source, /activityMaxCount \? remainCount/)
  Object.assign(page, {signName: '测试联系人', signMobile: '13800138000', signCount: 2})
  for (const maxCount of [0, '0', null, undefined, '', -1]) {
    page.activityDetail = {status: '1', isFree: 1, maxCount, signCount: 5}
    assert.equal(page.remainCount, '不限')
    assert.equal(page.maxSignCount, 99)
    assert.equal(page.validateForm(), '')
  }
  page.activityDetail = {status: '1', isFree: 1, maxCount: 6, signCount: 5}
  assert.match(page.validateForm(), /剩余名额不足/)
})
