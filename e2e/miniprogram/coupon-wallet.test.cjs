const test = require('node:test')
const assert = require('node:assert/strict')
const loadPage = require('./vue-page-harness.cjs')
const file = 'packagesMember/MyCoupon/MyCoupon.vue'
const coupon = {status:'1', discountType:'1', goodsId:260, categoryId:0}
const unused = {gotId:1,isUsed:0,status:'1',couponInfo:coupon}
const used = {gotId:2,isUsed:1,status:'0',couponInfo:coupon}
const expired = {gotId:3,isUsed:0,status:'1',couponInfo:{...coupon,enableEndTime:'2000-01-01 00:00:00'}}

test('wallet classifies used and expired coupons and preserves the selected tab on refresh', async () => {
  const {page} = loadPage(file, {getMyCouponList:async()=>({rows:[unused,used,expired],total:3})})
  await page.getCoupons()
  assert.deepEqual(Array.from(page.recList, row=>row.gotId), [1])
  page.onCouponTab(1)
  assert.deepEqual(Array.from(page.recList, row=>row.gotId), [2])
  await page.getCoupons()
  assert.deepEqual(Array.from(page.recList, row=>row.gotId), [2])
  page.onCouponTab(2)
  assert.deepEqual(Array.from(page.recList, row=>row.gotId), [3])
})

test('use coupon opens the exact eligible product or an explicitly scoped catalog', async () => {
  const paths = []
  const {page} = loadPage(file, {getGoodsInfo:async()=>({data:{goodsType:'online'}}),uni:{navigateTo:o=>paths.push(o.url)}})
  await page.onCouponUse(unused)
  assert.equal(paths[0], '/packagesMall/GoodsDetails/GoodsDetails?id=260')
  await page.onCouponUse({...unused,couponInfo:{...coupon,goodsId:0,categoryId:83}})
  assert.match(paths[1], /couponScope=1/)
  assert.match(paths[1], /categoryId=83/)
})

test('wallet loading errors are visible and older coupons can be loaded', async () => {
  let fail = true
  const {page} = loadPage(file, {getMyCouponList:async({pageNum})=>{
    if (fail) throw Error('网络不可用')
    return {rows:pageNum===1?[unused]:[used],total:2}
  }})
  await page.getCoupons()
  assert.equal(page.error, '网络不可用')
  fail=false
  await page.getCoupons()
  assert.equal(page.hasMore,true)
  await page.getCoupons(false)
  assert.equal(page.couponList.length,2)
  assert.equal(page.hasMore,false)
})
