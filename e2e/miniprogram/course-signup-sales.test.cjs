const test = require('node:test')
const assert = require('node:assert/strict')
const loadPage = require('./vue-page-harness.cjs')

function course() {
  const h = loadPage('packagesMall/ConfirmOrder/EducationConfirmOrder.vue', {
    getCourseSignupPhase: () => 'applying'
  })
  Object.assign(h.page, {goodsDetail: {goodsId: 279, status: '1', stock: 1}, contactName: '测试联系人', contactPhone: '13800138000'})
  return h
}

test('activity and course forms show required markers and real contact guidance without consent gates', () => {
  for (const file of ['ActivityConfirmOrder', 'EducationConfirmOrder']) {
    const {source} = loadPage(`packagesMall/ConfirmOrder/${file}.vue`)
    assert.equal((source.match(/class="required-mark"/g) || []).length, 2)
    assert.match(source, /请填写真实姓名/)
    assert.match(source, /正确的11位手机号/)
    assert.doesNotMatch(source, /u-checkbox|agreedNotice|agreement-row/)
  }
})

test('valid course contacts require no checkbox and retain stock and signup-period validation', () => {
  const {page} = course()
  assert.equal(page.validateForm(), '')
  page.goodsDetail.stock = 0
  assert.match(page.validateForm(), /名额已满/)
  page.goodsDetail.stock = 1
  page.getSignupPeriodError = () => '报名时间已截止'
  assert.match(page.validateForm(), /已截止/)
})

test('course validates missing name and correct mobile before submission', () => {
  const {page} = course()
  page.contactName = ' '
  assert.match(page.validateForm(), /真实姓名/)
  page.contactName = '测试联系人'
  for (const phone of ['', '12345678901', '1380013800', '138001380001', '138abc38000']) {
    page.contactPhone = phone
    assert.match(page.validateForm(), /正确的11位手机号码/)
  }
  page.contactPhone = ' 13900139000 '
  assert.equal(page.validateForm(), '')
})

test('course submission without consent preserves contact payload and cashier routing', async () => {
  const calls = [], routes = []
  const {page} = loadPage('packagesMall/ConfirmOrder/EducationConfirmOrder.vue', {
    getCourseSignupPhase: () => 'applying',
    createOrder: async payload => { calls.push(payload); return {code: 200, data: {orderId: 10, orderNo: 'test', moneyPayable: 320}} },
    uni: {showToast: () => {}, redirectTo: o => routes.push(o.url)},
    setTimeout: fn => fn()
  })
  page.$u = {throttle: fn => fn()}
  Object.assign(page, {goodsDetail: {goodsId: 279, status: '1', stock: 1}, contactName: ' 测试联系人 ', contactPhone: ' 13800138000 '})
  page.submitOrder()
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(calls.length, 1)
  assert.equal(calls[0].goodsCount, 1)
  assert.equal(calls[0].contactName, '测试联系人')
  assert.equal(calls[0].contactPhone, '13800138000')
  assert.match(routes[0], /CashierDesk\?orderAmount=320&orderId=10/)
})

test('course sales use only the paid signup aggregate, not pending reservations or order count', () => {
  const {page, source} = loadPage('pages/classify/classify.vue', {TabBar: {}, AuthProfilePopup: {}, sharePageMixin: {}, parseCourseMeta: () => ({}) , compactListingText: () => ''})
  for (const count of [0, 7, '12']) {
    assert.equal(page.buildGoodsListItem({paidSignupCount: count, saleCount: 99}).paidSignupCount, Number(count))
  }
  for (const count of [undefined, null, 'invalid']) {
    assert.equal(page.buildGoodsListItem({paidSignupCount: count, saleCount: 99}).paidSignupCount, null)
  }
  assert.match(source, /已售 {{ item.paidSignupCount }} 人/)
  assert.match(source, /item.paidSignupCount !== null/)
})
