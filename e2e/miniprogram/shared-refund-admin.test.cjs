const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const loadPage = require('./vue-page-harness.cjs')
const tick = () => new Promise(resolve => setImmediate(resolve))

for (const businessType of ['1', '3']) {
  test(`business ${businessType} uses the original after-sales approval, rejection and recovery API`, async () => {
    const calls = []
    const row = {afterId: 7, businessType, bookingAdjustmentId: 9, appRefundMoney: '20.00', orderId: 1}
    const {page} = loadPage('../ruoyi-ui/src/views/system/app_goods_order_after/index.vue', {
      getApp_goods_order_after: async id => { assert.equal(id, 7); return {data: row} },
      approvalApp_goods_order_after: async payload => { calls.push(payload); return {msg: 'ok'} }
    })
    page.$modal = {confirm: async () => {}, msgSuccess() {}, msgError(message) { assert.fail(message) }}
    page.$refs = {refundform: {validate: async () => true}}
    page.getList = () => {}
    page.ids = [7]
    await page.handleInfo()
    assert.equal(page.selrow.businessType, businessType)
    assert.equal(page.refundform.refundMoney, '20.00')
    page.approveAudit(7)
    await tick()
    assert.equal(calls[0].status, '1')
    assert.equal(calls[0].afterId, 7)
    assert.equal(calls[0].refundMoney, '20.00')
    page.refundform.remark = '不符合退款条件'
    page.rejectAudit(7)
    await tick()
    assert.equal(calls[1].status, '2')
    assert.equal(calls[1].remark, '不符合退款条件')
    await page.recoverRefund('sync')
    await page.recoverRefund('retry')
    assert.equal(calls[2].refundAction, 'sync')
    assert.equal(calls[3].refundAction, 'retry')
    assert.ok(calls.every(call => call.afterId === 7))
  })
}

test('no separate activity/card administrator approval route or queue remains', () => {
  const root = path.resolve(__dirname, '../..')
  const controller = fs.readFileSync(path.join(root, 'lankong-admin/src/main/java/com/ruoyi/web/controller/app/BookingAdjustmentController.java'), 'utf8')
  const ledger = fs.readFileSync(path.join(root, 'ruoyi-ui/src/views/system/app_pay_refund_log/index.vue'), 'utf8')
  assert.doesNotMatch(controller, /\/system\/booking_adjustment|public\s+\w+\s+review\(/)
  assert.doesNotMatch(ledger, /\/system\/booking_adjustment|reviewBooking|pendingReviews/)
  assert.match(ledger, /订单售后/)
})
