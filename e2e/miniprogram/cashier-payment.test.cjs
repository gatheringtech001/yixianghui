const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')

function cashier(payOrder) {
  const source = fs.readFileSync(path.resolve(__dirname,'../../shop-mnp/packagesMall/CashierDesk/CashierDesk.vue'),'utf8')
  const script = source.match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/import[^\n]+\n/g,'').replace('export default','module.exports =')
  const dialogs = [], payments = []
  const context = {module:{exports:{}},payOrder,console,setTimeout,clearTimeout,
    uni:{showModal:options=>dialogs.push(options),requestPayment:options=>payments.push(options)}}
  vm.runInNewContext(script,context)
  const component = context.module.exports
  const page = {...component.data(),orderId:448,orderNo:'20448'}
  for (const [name,method] of Object.entries(component.methods)) page[name]=method.bind(page)
  return {page,dialogs,payments}
}

test('prepay rejection displays a useful error and permits retry',async()=>{
  const {page,dialogs,payments}=cashier(async()=>{throw Error('微信预支付失败（PARAM_ERROR），请联系客服')})
  await page.pay()
  assert.equal(page.paying,false)
  assert.match(dialogs[0].content,/PARAM_ERROR/)
  assert.equal(payments.length,0)
})

test('incomplete prepay response cannot invoke WeChat payment',async()=>{
  const {page,dialogs,payments}=cashier(async()=>({data:{}}))
  await page.pay()
  assert.match(dialogs[0].content,/参数不完整/)
  assert.equal(payments.length,0)
})

test('concurrent payment taps send only one prepay request',async()=>{
  let resolve, calls=0
  const {page,payments}=cashier(()=>{calls++;return new Promise(r=>{resolve=r})})
  const pending=page.pay()
  await page.pay()
  assert.equal(calls,1)
  resolve({data:{timeStamp:'1',nonceStr:'test',packageVal:'prepay_id=test',signType:'RSA',paySign:'test'}})
  await pending
  assert.equal(payments.length,1)
  assert.equal(page.paying,true)
  payments[0].complete()
  assert.equal(page.paying,false)
})
