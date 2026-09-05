const test = require('node:test')
const assert = require('node:assert/strict')
const loadPage = require('./vue-page-harness.cjs')
const payment = {timeStamp:'1',nonceStr:'test',packageVal:'prepay_id=test',paySign:'test',signType:'RSA'}

test('confirming an ordinary payment retry actually requests payment again', async () => {
  let requests=0
  const payments=[], dialogs=[]
  const {page}=loadPage('packagesMall/CashierDesk/CashierDesk.vue',{
    payOrder:async()=>{requests++;return {data:payment}},
    uni:{requestPayment:o=>payments.push(o),showModal:o=>dialogs.push(o)}
  })
  await page.pay()
  payments[0].fail({errMsg:'requestPayment:fail cancel'})
  payments[0].complete()
  await dialogs[0].success({confirm:true})
  assert.equal(requests,2)
})

test('travel prepay serializes requests and rejects missing payment parameters', async () => {
  let requests=0, resolve
  const payments=[], messages=[]
  const {page}=loadPage('packagesMall/CashierDesk/SojournCashierDesk.vue',{
    payOrder:()=>{requests++;return new Promise(r=>{resolve=r})},
    uni:{requestPayment:o=>payments.push(o),showToast:o=>messages.push(o.title)}
  })
  page.orderLoading=false
  const first=page.pay()
  await page.pay()
  assert.equal(requests,1)
  resolve({code:200,data:{}})
  await first
  assert.equal(payments.length,0)
  assert.equal(page.paying,false)
  assert.match(messages[0],/参数不完整/)
})

test('travel cashier derives room, dates and meal wording from the order', () => {
  const {page,source}=loadPage('packagesMall/CashierDesk/SojournCashierDesk.vue')
  page.applyOrderInfo({orderNo:'20001',moneyPayable:420,goodsCount:2,selfGoodsCount:3,selfSkuId:20,
    checkInDate:'2026-10-01',checkOutDate:'2026-10-08',
    goodsList:[{goodsName:'测试基地',specifications:'江景大床房',optionList:[{skuId:20,skuName:'一日三餐'}]}]})
  assert.equal(page.roomName,'江景大床房')
  assert.equal(page.reserveData.roomNumber,2)
  assert.equal(page.mealDescription,'一日三餐 · 3人')
  assert.doesNotMatch(source,/豪华双床房|住满7晚含接或送机站1次/)
})

test('travel prepay displays backend errors and releases its lock', async () => {
  const messages=[]
  const {page}=loadPage('packagesMall/CashierDesk/SojournCashierDesk.vue',{
    payOrder:async()=>{throw Error('订单已取消')},uni:{showToast:o=>messages.push(o.title)}
  })
  page.orderLoading=false
  await page.pay()
  assert.equal(messages[0],'订单已取消')
  assert.equal(page.paying,false)
})
