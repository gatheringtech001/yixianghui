const test = require('node:test')
const assert = require('node:assert/strict')
const loadPage = require('./vue-page-harness.cjs')
const file = 'packagesMall/ConfirmOrder/ActivityConfirmOrder.vue'
const user = {nickName: ' 测试昵称 ', phonenumber: '13800138000'}
const freeActivity = {activityId:146, status:'1', isFree:1, price:0, vipPrice:0, maxCount:null}
function form(overrides = {}) {
  const calls = [], routes = [], toasts = []
  const harness = loadPage(file, {
    getActivityPhase: () => 'applying',
    getInfo: async () => ({code:200, data:user}),
    getActivityInfo: async () => ({code:200, data:freeActivity}),
    addActivityOrder: async p => { calls.push(['free',p]); return {code:200,data:{orderId:27}} },
    createActivityPendingOrder: async p => { calls.push(['paid',p]); return {code:200,data:{orderId:28,moneyPayable:40,orderNo:'test'}} },
    uni: {getStorageSync: () => user, showToast: o => toasts.push(o.title), redirectTo:o=>routes.push(o.url)},
    setTimeout: callback => callback(),
    ...overrides
  })
  Object.assign(harness.page,{activityDetail:{...freeActivity},signName:' 测试联系人 ',signMobile:'13800138000'})
  harness.page.$nextTick = cb => cb()
  harness.page.setPageScrollHeight = () => {}
  return {...harness,calls,routes,toasts}
}

test('free and paid activity details both open the complete labelled signup form', () => {
  for (const isFree of [0,1]) {
    const routes = []
    const {page, source} = loadPage('packagesMall/Activity/detail/index.vue', {
      sharePageMixin:{}, AuthProfilePopup:{}, getActivityPhase:()=> 'applying',
      uni:{getStorageSync:()=>user,navigateTo:o=>routes.push(o.url)}
    })
    page.detailInfo = {...freeActivity,isFree}
    page.apply()
    assert.deepEqual(routes,['/packagesMall/ConfirmOrder/ActivityConfirmOrder?id=146'])
    assert.doesNotMatch(source,/placeholderText: '请输入报名人数'/)
  }
})

test('form explicitly labels required name phone and headcount and allows editing', () => {
  const {source} = form()
  for (const label of ['报名联系人（必填）','联系电话（必填）','参与人数（必填）','包含您本人','请输入11位手机号码']) assert.ok(source.includes(label))
  assert.match(source, /@input="contactEdited.name = true"/)
  assert.match(source, /@input="contactEdited.mobile = true"/)
})

test('profile nickname and bound mobile are prefilled without mutating the profile', async () => {
  const {page} = form()
  const before = JSON.stringify(user)
  await page.prefillContactInfo()
  assert.equal(page.signName,'测试昵称')
  assert.equal(page.signMobile,'13800138000')
  assert.equal(JSON.stringify(user),before)
})

test('missing profile remains editable and does not invent a real name or phone', () => {
  const {page} = form()
  page.signName = ''; page.signMobile = ''
  for (const profile of [null,'',{}, {nickName:'微信小程序用户',phonenumber:'138****8000'}]) page.applyContactDefaults(profile)
  assert.equal(page.signName,''); assert.equal(page.signMobile,'')
  page.applyContactDefaults({nickName:'另一昵称',mobile:'13900139000'})
  assert.equal(page.signMobile,'13900139000')
})

test('late profile refresh never overwrites edits or deliberate clearing', async () => {
  let resolve
  const {page} = form({getInfo:()=> new Promise(r=>{resolve=r})})
  const pending = page.prefillContactInfo()
  page.signName = ''; page.signMobile = '13900139000'
  page.contactEdited = {name:true,mobile:true}
  resolve({code:200,data:{nickName:'服务器昵称',phonenumber:'13700137000'}})
  await pending
  assert.equal(page.signName,''); assert.equal(page.signMobile,'13900139000')
})

test('profile request failure retains cached defaults and permits manual signup', async () => {
  const {page} = form({getInfo:async()=>{throw Error('离线')}})
  await page.prefillContactInfo()
  assert.equal(page.signName,'测试昵称'); assert.equal(page.validateForm(),'')
})

test('validation rejects blank contacts invalid mobiles noninteger and exhausted headcount', () => {
  for (const [patch,expected] of [
    [{signName:' '},'报名联系人'], [{signMobile:''},'手机号码'], [{signMobile:'12345678901'},'手机号码'],
    [{signCount:0},'整数'], [{signCount:1.5},'整数'], [{signCount:'1abc'},'整数'],
    [{signCount:100},'可报名人数']
  ]) {const {page}=form();Object.assign(page,patch);assert.ok(page.validateForm().includes(expected))}
  const {page}=form(); page.activityDetail={...freeActivity,maxCount:2,signCount:2}
  assert.match(page.validateForm(),/剩余名额不足/)
})

test('valid free submission persists all contact fields and skips the cashier', async () => {
  const {page,calls,routes}=form()
  page.signCount=2
  await page.submitOrder()
  assert.equal(calls.length,1);assert.equal(calls[0][0],'free')
  assert.deepEqual(JSON.parse(JSON.stringify(calls[0][1])),{activityId:146,signName:'测试联系人',signMobile:'13800138000',signCount:2,remark:''})
  assert.deepEqual(routes,['/packagesMember/MyActivity/detail/index?orderId=27&showTeacher=1'])
  assert.equal(page.submitting,false)
})

test('paid signup still creates a pending order and opens the existing cashier', async () => {
  const {page,calls,routes}=form()
  page.activityDetail={...freeActivity,isFree:0,price:20,vipPrice:20};page.signCount=2
  assert.equal(page.payAmount,40)
  await page.submitOrder()
  assert.equal(calls[0][0],'paid')
  assert.match(routes[0],/ActivityCashierDesk\?orderAmount=40&orderId=28/)
})

test('free price is always zero even if historical price fields are nonzero', () => {
  const {page}=form();page.activityDetail={...freeActivity,price:20,vipPrice:10}
  assert.equal(page.payAmount,0);assert.equal(page.validateForm(),'')
})

test('double tap and repeated submission after success cannot create duplicate orders', async () => {
  let resolve, count=0
  const {page}=form({addActivityOrder:()=>{count++;return new Promise(r=>{resolve=r})}})
  const pending=page.submitOrder()
  await page.submitOrder();assert.equal(count,1)
  resolve({code:200,data:{orderId:27}});await pending
  await page.submitOrder();assert.equal(count,1)
})

test('failed submission preserves input unlocks retry and never navigates to success', async () => {
  const {page,routes,toasts}=form({addActivityOrder:async()=>{throw Error('名额不足')}})
  await page.submitOrder()
  assert.equal(page.submitting,false);assert.equal(page.signupSubmitted,false)
  assert.equal(page.signName,' 测试联系人 ');assert.equal(routes.length,0);assert.ok(toasts.includes('名额不足'))
})

test('invalid form and malformed server response never create a successful booking view', async () => {
  const invalid=form();invalid.page.signMobile='';await invalid.page.submitOrder();assert.equal(invalid.calls.length,0)
  const bad=form({addActivityOrder:async()=>({code:200,data:{}})})
  await bad.page.submitOrder();assert.equal(bad.routes.length,0);assert.equal(bad.page.signupSubmitted,false)
})
