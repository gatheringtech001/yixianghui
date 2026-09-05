const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const assert = require('node:assert/strict')
const vm = require('node:vm')

const root = path.resolve(__dirname, '../..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')

function homeOfferHarness() {
  const home = read('shop-mnp/pages/home/home.vue')
  const method = home.slice(home.indexOf('async loadDistributionOffer()'), home.indexOf('formatCouponDiscount(percent)'))
  const state = { source: { channelCode: 'channel-a' }, token: 'session', calls: [], fail: false }
  const context = {
    uni: { getStorageSync: key => key === 'token' ? state.token : { userId: 7 } },
    getDistributionLaunchSource: () => state.source,
    clearDistributionLaunchSource: () => { state.source = null },
    getDistributionOffer: async source => {
      state.calls.push(source.channelCode)
      if (state.fail) throw new Error('offline')
      return { data: { coupon: { channelCode: source.channelCode }, claimed: false } }
    },
    console: { warn() {} }
  }
  const methods = vm.runInNewContext(`({${method}})`, context)
  return { state, page: { ...methods, distributionOfferChecked: false, distributionOfferKey: '', loadingDistributionKey: '' } }
}

test('switching channel refreshes its offer while repeated shows do not duplicate visits', async () => {
  const { state, page } = homeOfferHarness()
  await page.loadDistributionOffer()
  await page.loadDistributionOffer()
  state.source = { channelCode: 'channel-b' }
  await page.loadDistributionOffer()
  assert.deepEqual(state.calls, ['channel-a', 'channel-b'])
  assert.equal(page.distributionOffer.coupon.channelCode, 'channel-b')
})

test('temporary offer failure preserves attribution and allows retry', async () => {
  const { state, page } = homeOfferHarness()
  state.fail = true
  await page.loadDistributionOffer()
  assert.equal(state.source.channelCode, 'channel-a')
  state.fail = false
  await page.loadDistributionOffer()
  assert.equal(page.distributionOffer.coupon.channelCode, 'channel-a')
})

test('guest channel entry exposes a login-to-claim invitation', async () => {
  const { state, page } = homeOfferHarness()
  state.token = ''
  await page.loadDistributionOffer()
  assert.equal(page.showDistributionCoupon, true)
  assert.equal(state.calls.length, 0)
})

test('WeChat launch scene is not an inviter id; explicit QR invitation remains valid', async () => {
  const storage = new Map()
  global.uni = { getStorageSync: key => storage.get(key), setStorageSync: (key, value) => storage.set(key, value) }
  const source = read('shop-mnp/utils/invite.js')
  const invite = await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`)
  invite.parseLaunchInviteOptions({ scene: 1194, query: { channelCode: 'channel-a' } })
  assert.equal(storage.get('parentUserId'), undefined)
  invite.parseInvitePageOptions({ scene: 'u42' })
  assert.equal(storage.get('parentUserId'), 42)
  assert.doesNotThrow(() => invite.parseLaunchInviteOptions({ query: { scene: '%' } }))
  invite.saveInviteParentUserId('99garbage')
  assert.equal(storage.get('parentUserId'), 42)
  delete global.uni
})

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
