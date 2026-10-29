const test = require('node:test')
const assert = require('node:assert/strict')
const loadPage = require('./vue-page-harness.cjs')
const file = 'packagesMember/MyActivity/detail/index.vue'
const booking = { orderId: 1, status: '1', payStatus: '1', payMoney: 20, signCount: 2, signName: '测试', signMobile: '13800138000', activityInfo: { activityName: '测试活动', isFree: 0, maxCount: 10, signCount: 2 } }
function detail(overrides = {}) {
  const toasts = [], edits = []
  let latest = null
  const harness = loadPage(file, {
    ActivityTeacherContact: {},
    getActivityOrderInfo: async () => ({ data: booking }),
    getBookingAdjustment: async () => ({ data: latest }),
    editActivityOrder: async data => { edits.push(data); latest = { adjustmentId: 7, state: data.signCount > 2 ? 'PAYMENT' : 'REVIEW', fromCount: 2, toCount: data.signCount, amount: 10 } },
    syncBookingAdjustment: async () => ({ data: latest }),
    prepayBookingAdjustment: async () => ({ data: { timeStamp: '1', nonceStr: 'test', packageVal: 'prepay_id=test', signType: 'RSA', paySign: 'test' } }),
    uni: { setNavigationBarTitle() {}, showToast: o => toasts.push(o.title), showModal: o => o.success({ confirm: true }), requestPayment: o => o.success({}) },
    ...overrides
  })
  harness.page.orderId = 1
  harness.page.applyOrderData(booking)
  return { ...harness, toasts, edits }
}
const flush = () => new Promise(resolve => setImmediate(resolve))

test('paid bookings permit count editing but free bookings do not', () => {
  const { page, source } = detail()
  assert.equal(page.canEditSignCount, true)
  assert.doesNotMatch(source, /付费报名人数已锁定/)
  page.detailInfo.payMoney = 0
  assert.equal(page.canEditSignCount, false)
  assert.match(source, /免费活动仅限本人报名/)
})
test('saved increase keeps confirmed headcount and exposes payment intent', async () => {
  const { page, edits, toasts } = detail()
  page.isEdit = true; page.signForm.signCount = 3; page.handleEdit(); await flush()
  assert.equal(edits.length, 1); assert.equal(edits[0].signCount, 3)
  assert.equal(page.signForm.signCount, 2)
  assert.equal(page.adjustment.state, 'PAYMENT'); assert.equal(page.canEditSignCount, false)
  assert.ok(toasts.includes('请支付新增人数差价'))
})
test('saved decrease is a review request not an immediately changed booking', async () => {
  const { page, toasts } = detail()
  page.isEdit = true; page.signForm.signCount = 1; page.handleEdit(); await flush()
  assert.equal(page.signForm.signCount, 2); assert.equal(page.adjustment.state, 'REVIEW')
  assert.match(page.adjustmentTitle, /待审核/); assert.ok(toasts.includes('退款申请已提交，待审核'))
})
test('free cancellation text does not promise a monetary refund', () => {
  let content = ''
  const { page } = detail({ uni: { setNavigationBarTitle() {}, showModal: o => { content = o.content } } })
  page.detailInfo.payMoney = 0; page.handleCancel()
  assert.match(content, /取消本人预约/); assert.doesNotMatch(content, /退款/)
})
test('cancelled payment preserves original booking and makes the intent retryable', async () => {
  const { page, toasts } = detail({ uni: { setNavigationBarTitle() {}, showToast: o => toasts.push(o.title), requestPayment: o => o.fail({ errMsg: 'requestPayment:fail cancel' }) } })
  page.adjustment = { adjustmentId: 7, state: 'PAYMENT' }
  await page.payDifference()
  assert.equal(page.signForm.signCount, 2); assert.equal(page.adjustmentBusy, false)
  assert.ok(toasts.includes('已取消支付，原预约不变'))
})
test('available edit count includes own confirmed seats and does not grow with user input', () => {
  const { page } = detail()
  page.detailInfo.signCount = 9
  assert.equal(page.editMaxCount, 3)
  page.signForm.signCount = 20
  assert.equal(page.editMaxCount, 3)
})
