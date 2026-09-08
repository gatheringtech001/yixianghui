const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const loadPage = require('./vue-page-harness.cjs')
const detailFile = 'packagesMember/MyActivity/detail/index.vue'
const contactFile = 'packagesMember/MyActivity/detail/ActivityTeacherContact.vue'
const positionCode = 'mnp_activity_teacher_qr'
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

test('returning to a reservation refreshes the backend QR configuration', () => {
  const {page, component} = detail()
  let refreshes = 0
  page.$refs = {teacherContact: {loadQr: () => { refreshes++ }}}
  component.onShow.call(page)
  assert.equal(refreshes, 1)
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

function contact(overrides = {}) {
  return loadPage(contactFile, {
    AD_POSITION: {ACTIVITY_TEACHER_QR: positionCode},
    resolveAdImageUrl: (host, url) => /^https:/.test(url) ? url : host + url,
    getBannerPosList: async () => ({data: [{positionId: 11, positionCode, status: '0'}]}),
    getBannerList: async () => ({data: [{contentId: 35, adImage: '/profile/new-qr.png', status: '1'}]}),
    ...overrides
  })
}

test('teacher QR resolves its backend position code and previews the configured image', async () => {
  const previews = []
  const {page, source} = contact({
    getBannerPosList: async params => {
      assert.equal(params.positionCode, positionCode)
      return {data: [{positionId: 11, positionCode, status: '0'}]}
    },
    getBannerList: async params => {
      assert.equal(params.positionId, 11)
      assert.equal(params.status, '1')
      return {data: [{contentId: 35, adImage: '/profile/new-qr.png', status: '1'}]}
    },
    uni: {previewImage: o => previews.push(o)}
  })
  await page.loadQr()
  page.previewQr()
  assert.equal(previews[0].urls[0], 'https://example.invalid/profile/new-qr.png')
  assert.match(source, /扫码加齐齐老师/)
  assert.match(source, /mode="aspectFit"/)
  assert.match(source, /show-menu-by-longpress/)
  assert.doesNotMatch(source, /qiqi-teacher-qr\.png|copyFile|USER_DATA_PATH/)
})

test('backend replacement is picked up on refresh without stale QR fallback', async () => {
  let url = '/profile/first.png'
  const {page} = contact({getBannerList: async () => ({data: [{adImage:url,status:'1'}]})})
  await page.loadQr()
  assert.match(page.qrImage, /first.png$/)
  url = '/profile/replacement.png'
  await page.loadQr()
  assert.match(page.qrImage, /replacement.png$/)
})

test('missing disabled and failed QR configuration exposes a retry state instead of an old image', async () => {
  for (const data of [[], [{adImage:'/profile/old.png',status:'0'}]]) {
    const {page} = contact({getBannerList: async () => ({data})})
    page.qrImage = 'https://example.invalid/old.png'
    await page.loadQr()
    assert.equal(page.qrImage, '')
    assert.match(page.qrError, /暂未配置/)
  }
  const {page} = contact({getBannerPosList: async () => {throw Error('网络不可用')}})
  await page.loadQr()
  assert.equal(page.qrImage, '')
  assert.equal(page.qrError, '网络不可用')
  assert.equal(page.loading, false)
})

test('an unavailable QR cannot open a broken image preview', () => {
  const previews = []
  const {page} = contact({uni: {previewImage: o => previews.push(o)}})
  page.previewQr()
  assert.equal(previews.length, 0)
})

test('the compiled member package no longer ships a hardcoded QR asset', {
  skip: !process.env.MINIPROGRAM_PROJECT_PATH
}, () => {
  assert.equal(fs.existsSync(path.join(process.env.MINIPROGRAM_PROJECT_PATH, 'packagesMember/static/qiqi-teacher-qr.png')), false)
})
