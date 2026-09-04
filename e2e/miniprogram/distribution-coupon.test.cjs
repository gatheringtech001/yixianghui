const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')

test('launch source captures channel and referring mini program', () => {
  const source = read('shop-mnp/utils/invite.js')
  assert.match(source, /referrerInfo/)
  assert.match(source, /extraData\.channelCode/)
  assert.match(source, /sourceAppId/)
})

test('home loads and claims the channel offer', () => {
  const home = read('shop-mnp/pages/home/home.vue')
  assert.match(home, /loadDistributionOffer/)
  assert.match(home, /claimDistributionCoupon/)
  assert.match(home, /立即领取/)
})

test('all goods orders can auto-apply a claimed channel coupon', () => {
  const order = read('ruoyi-system/src/main/java/com/ruoyi/system/service/impl/AppGoodsOrderServiceImpl.java')
  assert.match(order, /selectBestChannelCoupon/)
  assert.match(order, /markUsed/)
  assert.match(order, /setMoneyDiscount/)
})
