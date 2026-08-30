const assert = require('node:assert/strict')
const fs = require('node:fs/promises')
const path = require('node:path')
const test = require('node:test')

async function loadActivityOrderState() {
  const sourcePath = path.resolve(
    __dirname,
    '../../shop-mnp/utils/activityOrderState.js'
  )
  const source = await fs.readFile(sourcePath, 'utf8')
  const encoded = Buffer.from(source).toString('base64')
  return import(`data:text/javascript;base64,${encoded}`)
}

test('finds the current active booking and ignores cancelled bookings', async () => {
  const { findActiveActivityOrder } = await loadActivityOrderState()
  const rows = [
    { orderId: 9, activityId: 3, status: '2', payStatus: '2' },
    { orderId: 10, activityId: 3, status: '1', payStatus: '1' },
    { orderId: 11, activityId: 4, status: '1', payStatus: '1' }
  ]

  assert.equal(findActiveActivityOrder(rows, 3).orderId, 10)
  assert.equal(findActiveActivityOrder(rows, 5), null)
})

test('maps activity bookings into the shared order center without changing storage', async () => {
  const {
    filterOrdersByTab,
    mapActivityOrderForOrderList,
    mergeOrdersByCreateTime
  } = await loadActivityOrderState()
  const paid = mapActivityOrderForOrderList({
    orderId: 20,
    activityId: 8,
    status: '1',
    payStatus: '1',
    signCount: 2,
    moneyPayable: 0.02,
    payMoney: 0.02,
    createTime: '2026-08-30 10:00:00',
    activityInfo: {
      activityName: '一分钱测试活动',
      activityCover: '/profile/activity.png',
      activityTime: '2026-09-01 09:00'
    }
  })

  assert.equal(paid.orderKind, 'activity')
  assert.equal(paid.status, '1')
  assert.equal(paid.goodsCount, 2)
  assert.equal(paid.goodsList[0].goodsType, 'activity')
  assert.equal(paid.goodsList[0].goodsName, '一分钱测试活动')
  assert.deepEqual(filterOrdersByTab([paid], 2), [paid])
  assert.deepEqual(filterOrdersByTab([paid], 1), [])

  const olderGoodsOrder = { orderId: 1, createTime: '2026-08-29 10:00:00' }
  assert.deepEqual(mergeOrdersByCreateTime([olderGoodsOrder], [paid]), [paid, olderGoodsOrder])
})

test('keeps pending and refunding activity bookings visible as existing bookings', async () => {
  const { findActiveActivityOrder, mapActivityOrderForOrderList } = await loadActivityOrderState()
  const pending = { orderId: 30, activityId: 8, status: '0', payStatus: '0' }
  const refunding = { orderId: 31, activityId: 9, status: '3', payStatus: '3' }

  assert.equal(findActiveActivityOrder([pending], 8).orderId, 30)
  assert.equal(findActiveActivityOrder([refunding], 9).orderId, 31)
  assert.equal(mapActivityOrderForOrderList(pending).status, '0')
  assert.equal(mapActivityOrderForOrderList(refunding).status, '3')
})

test('activity detail switches the primary action to the exact booking detail', async () => {
  const source = await fs.readFile(path.resolve(
    __dirname,
    '../../shop-mnp/packagesMall/Activity/detail/index.vue'
  ), 'utf8')

  assert.match(source, /v-if="activeActivityOrder"[^>]*>查看我的预约<\/view>/)
  assert.match(source, /findActiveActivityOrder\(res && res\.rows, this\.activityId\)/)
  assert.match(source, /MyActivity\/detail\/index\?orderId=\$\{this\.activeActivityOrder\.orderId\}/)
})

test('shared order center loads activity bookings and routes them to activity details', async () => {
  const source = await fs.readFile(path.resolve(
    __dirname,
    '../../shop-mnp/packagesMall/MyOrderList/MyOrderList.vue'
  ), 'utf8')

  assert.match(source, /getActivityOrderList\(\{ pageNum: 1, pageSize: 100 \}\)/)
  assert.match(source, />活动预约<\/text>/)
  assert.match(source, /MyActivity\/detail\/index\?orderId=\$\{item\.orderId\}/)
})
