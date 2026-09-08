const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const crypto = require('node:crypto')
const loadPage = require('./vue-page-harness.cjs')
const detailFile = 'packagesMember/MyActivity/detail/index.vue'
const contactFile = 'packagesMember/MyActivity/detail/ActivityTeacherContact.vue'
const qrPath = '/packagesMember/static/qiqi-teacher-qr.png'
const booking = {orderId: 27, status: '1', payStatus: '1', payMoney: 20,
  signCount: 1, activityInfo: {activityName: '活动测试'}}
const flush = () => new Promise(resolve => setImmediate(resolve))

function detail(overrides = {}) {
  return loadPage(detailFile, {ActivityTeacherContact: {},
    getActivityOrderInfo: async () => ({data: booking}),
    uni: {setNavigationBarTitle() {}}, ...overrides})
}

test('successful booking entry opens the teacher popup once and retains the card', async () => {
  const {page, component} = detail()
  component.onLoad.call(page, {orderId: '27', showTeacher: '1'})
  assert.equal(page.canShowTeacher, false)
  await page.getDetail(page.orderId)
  assert.equal(page.canShowTeacher, true)
  assert.equal(page.showTeacherPopup, true)
  page.showTeacherPopup = false
  await page.getDetail(page.orderId)
  assert.equal(page.canShowTeacher, true)
  assert.equal(page.showTeacherPopup, false)
})

test('opening an existing free or paid reservation displays the QR without auto-popup', async () => {
  for (const payMoney of [0, 20]) {
    const {page, component} = detail()
    component.onLoad.call(page, {orderId: '27'})
    page.applyOrderData({...booking, payMoney})
    assert.equal(page.canShowTeacher, true)
    assert.equal(page.showTeacherPopup, false)
  }
})

test('pending cancelled refunding and unknown orders never display the teacher QR', () => {
  for (const [status, payStatus] of [['0','0'], ['2','2'], ['3','3'], ['4','4'], ['2','1'], ['1','0'], [null,null]]) {
    const {page, component} = detail()
    component.onLoad.call(page, {orderId: '27', showTeacher: '1'})
    page.applyOrderData({...booking, status, payStatus})
    assert.equal(page.canShowTeacher, false)
    assert.equal(page.showTeacherPopup, false)
  }
})

test('delayed payment opens the QR only after the server confirms the reservation', async () => {
  let reads = 0
  const {page, component} = detail({
    getActivityOrderInfo: async () => ({data: ++reads === 1 ? {...booking, status:'0', payStatus:'0'} : booking}),
    syncActivityOrderPay: async () => {
      assert.equal(page.canShowTeacher, false)
      assert.equal(page.showTeacherPopup, false)
    }
  })
  component.onLoad.call(page, {orderId: '27', showTeacher: '1'})
  await page.getDetail(page.orderId)
  assert.equal(page.showTeacherPopup, true)
  assert.equal(reads, 2)
})

test('failed confirmation cannot show a QR or consume the pending success prompt', async () => {
  const {page, component} = detail({
    getActivityOrderInfo: async () => ({data: {...booking, status:'0', payStatus:'0'}}),
    syncActivityOrderPay: async () => { throw Error('未支付') }
  })
  component.onLoad.call(page, {orderId: '27', showTeacher: '1'})
  await page.getDetail(page.orderId)
  assert.equal(page.showTeacherPopup, false)
  assert.equal(page.teacherPromptPending, true)
})

function signup(result, error) {
  const dialogs = [], navigations = [], notices = []
  const {page} = loadPage('packagesMall/Activity/detail/index.vue', {
    sharePageMixin: {}, AuthProfilePopup: {}, getActivityPhase: () => 'applying',
    addActivityOrder: async () => { if (error) throw error; return result },
    uni: {getStorageSync: () => ({}), showModal: o => dialogs.push(o),
      navigateTo: o => navigations.push(o.url), showToast: o => notices.push(o.title)}
  })
  page.detailInfo = {activityId: 8, isFree: 1, maxCount: 10}
  page.activityId = 8
  page.getDetail = () => {}
  page.loadBookingState = () => {}
  return {page, dialogs, navigations, notices}
}

test('free signup navigates the returned reservation to the teacher success prompt', async () => {
  const {page, dialogs, navigations} = signup({data: booking})
  page.apply()
  dialogs[0].success({confirm: true, content: '1'})
  await flush()
  assert.deepEqual(navigations, ['/packagesMember/MyActivity/detail/index?orderId=27&showTeacher=1'])
})

test('failed or malformed free signup never navigates to a successful reservation', async () => {
  for (const error of [undefined, Error('报名失败')]) {
    const {page, dialogs, navigations, notices} = signup({data: {}}, error)
    page.apply()
    dialogs[0].success({confirm: true, content: '1'})
    await flush()
    assert.equal(navigations.length, 0)
    assert.ok(notices.length > 0)
  }
})

function cashier() {
  const payments = [], navigations = []
  const {page} = loadPage('packagesMall/CashierDesk/ActivityCashierDesk.vue', {
    payActivityOrder: async () => ({data: {timeStamp:'1', nonceStr:'test', packageVal:'prepay_id=test', signType:'RSA', paySign:'test'}}),
    syncActivityOrderPay: async () => {},
    uni: {requestPayment: o => payments.push(o), removeStorageSync() {},
      redirectTo: o => navigations.push(o.url), showModal() {}, showToast() {}}
  })
  page.orderId = 27
  return {page, payments, navigations}
}

test('successful WeChat payment opens the exact reservation with a one-time QR prompt', async () => {
  const {page, payments, navigations} = cashier()
  page.pay()
  await flush()
  payments[0].success()
  await flush()
  assert.deepEqual(navigations, ['/packagesMember/MyActivity/detail/index?orderId=27&showTeacher=1'])
})

test('cancelled WeChat payment never opens the teacher prompt', async () => {
  const {page, payments, navigations} = cashier()
  page.pay()
  await flush()
  payments[0].fail({errMsg:'requestPayment:fail cancel'})
  payments[0].complete()
  assert.equal(navigations.length, 0)
  assert.equal(page.paying, false)
})

test('the reservation card and popup preserve the supplied QR and allow image preview', () => {
  const previews = []
  const {page, source} = loadPage(contactFile, {uni: {
    env: {USER_DATA_PATH: 'wxfile://usr'},
    getFileSystemManager: () => ({copyFile: o => {
      assert.equal(o.srcPath, qrPath.slice(1))
      assert.equal(o.destPath, 'wxfile://usr/activity-qiqi-teacher.png')
      o.success()
    }}),
    previewImage: o => previews.push(o)
  }})
  page.previewQr()
  assert.equal(previews[0].urls[0], 'wxfile://usr/activity-qiqi-teacher.png')
  assert.match(source, /扫码加齐齐老师/)
  assert.match(source, /mode="aspectFit"/)
  assert.match(source, /show-menu-by-longpress/)
  const image = fs.readFileSync(path.resolve(__dirname, '../../shop-mnp' + qrPath))
  assert.equal(crypto.createHash('sha256').update(image).digest('hex'), '567d62e1463136d12948bf9d2faccba35419daa73edfeec90e8c5ea34f78e236')
  assert.equal(image.readUInt32BE(16), 239)
  assert.equal(image.readUInt32BE(20), 247)
})

test('a QR image resolution failure is visible and does not open a broken preview', () => {
  const previews = [], notices = []
  const {page} = loadPage(contactFile, {uni: {
    env: {USER_DATA_PATH: 'wxfile://usr'},
    getFileSystemManager: () => ({copyFile: o => o.fail()}),
    previewImage: o => previews.push(o),
    showToast: o => notices.push(o.title)
  }})
  page.previewQr()
  assert.equal(previews.length, 0)
  assert.deepEqual(notices, ['二维码打开失败，请重试'])
})

test('the compiled member package contains the original QR asset', {
  skip: !process.env.MINIPROGRAM_PROJECT_PATH
}, () => {
  const image = fs.readFileSync(path.join(process.env.MINIPROGRAM_PROJECT_PATH, qrPath))
  assert.equal(crypto.createHash('sha256').update(image).digest('hex'), '567d62e1463136d12948bf9d2faccba35419daa73edfeec90e8c5ea34f78e236')
})
