const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const loadPage = require('./vue-page-harness.cjs')

async function harness(api) {
  const source=fs.readFileSync(path.resolve(__dirname,'../../shop-mnp/utils/activityOrderState.js'),'utf8')
  const state=await import('data:text/javascript;base64,'+Buffer.from(source).toString('base64'))
  return loadPage('packagesMall/MyOrderList/MyOrderList.vue',{
    ...state, collectGoodsIds:()=>[], ...api
  }).page
}

test('one unavailable order source does not hide orders from the other source', async () => {
  const page=await harness({getOrderList:async()=>({rows:[{orderId:1,status:'1'}],total:1}),
    getActivityOrderList:async()=>{throw Error('活动加载失败')}})
  await page.getOrders()
  assert.equal(page.orderList.length,1)
  assert.match(page.loadError,/活动/)
  assert.equal(page.loading,false)
})

test('orders load more pages without duplicating existing entries', async () => {
  const calls=[]
  const page=await harness({getOrderList:async p=>{calls.push(p.pageNum);return {rows:[{orderId:p.pageNum,status:'1'}],total:2}},
    getActivityOrderList:async()=>({rows:[],total:0})})
  await page.getOrders()
  assert.equal(page.hasMore,true)
  await page.getOrders(false)
  assert.deepEqual(calls,[1,2])
  assert.equal(page.orderList.length,2)
  assert.equal(page.hasMore,false)
})

test('profile shipping entries use shipping filters, not cancelled-order filters', () => {
  const source=fs.readFileSync(path.resolve(__dirname,'../../shop-mnp/pages/my/my.vue'),'utf8')
  assert.match(source, /onSkipOrder\(6\)/)
  assert.match(source, /onSkipOrder\(7\)/)
})
