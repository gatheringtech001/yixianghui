const assert = require('node:assert/strict')
const fs = require('node:fs/promises')
const path = require('node:path')
const test = require('node:test')

const projectRoot = path.resolve(__dirname, '../..')

async function read(relativePath) {
  return fs.readFile(path.join(projectRoot, relativePath), 'utf8')
}

async function loadTravelPresentation() {
  const source = await read('shop-mnp/utils/travelPresentation.js')
  const moduleUrl = 'data:text/javascript;base64,' + Buffer.from(source).toString('base64')
  return import(moduleUrl)
}

test('travel presentation derives honest unit prices and trims list copy', async () => {
  const {
    compactListingText,
    formatCalendarPrice,
    resolveCalendarUnitPrice
  } = await loadTravelPresentation()

  assert.equal(resolveCalendarUnitPrice({ average: 99, total: 693, nights: 7 }), 99)
  assert.equal(resolveCalendarUnitPrice({ total: 693, nights: 7 }), 99)
  assert.equal(resolveCalendarUnitPrice({ total: 693, nights: 0 }), 0)
  assert.equal(formatCalendarPrice(99), '￥99/人')
  assert.equal(formatCalendarPrice(0), '')
  assert.equal(compactListingText('<p> 安心旅居\n康养配套 </p>', 8), '安心旅居 康养配…')
})

test('authorization is a full-page gated flow and phone binding stays optional', async () => {
  const source = await read('shop-mnp/components/AuthProfilePopup/AuthProfilePopup.vue')
  const login = await read('shop-mnp/utils/login.js')

  assert.match(source, /agreementChecked/)
  assert.match(source, /请先阅读并同意用户协议和隐私政策/)
  assert.match(source, /goProfileStep\(\)/)
  assert.match(source, />微信登录<\/view>/)
  assert.doesNotMatch(source, /微信授权登录/)
  assert.match(source, /loginFn/)
  assert.match(login, /popup\.open\(resolve,\s*async\s*\(\)\s*=>/)
  assert.match(source, /open-type="getPhoneNumber"/)
  assert.match(source, /onSkipPhone\(\)/)
  assert.match(source, /授权手机/)
  assert.match(source, /maybeAdvanceProfile\(\)/)
  assert.doesNotMatch(source, /便于管家联系您|手机号仅用于订单通知/)
  assert.match(source, /\.avatar-btn\s*\{[\s\S]*?overflow:\s*visible/)
  assert.match(source, /background:\s*#fff/)
  assert.match(source, /height:\s*100%/)
})

test('travel calendars derive their label from the selected SKU instead of a constant', async () => {
  const detail = await read('shop-mnp/packagesMall/GoodsDetails/SojournGoodsDetails.vue')
  const confirm = await read('shop-mnp/packagesMall/ConfirmOrder/SojournConfirmOrder.vue')

  for (const source of [detail, confirm]) {
    assert.doesNotMatch(source, /price="￥196\/人"/)
    assert.match(source, /:price="calendarPriceText"/)
    assert.match(source, /calendarPriceText\(\)/)
  }
})

test('travel list uses a compact summary and the sign-in reward cards are shorter', async () => {
  const classify = await read('shop-mnp/pages/classify/classify.vue')
  const classifyStyle = await read('shop-mnp/pages/classify/classify.scss')
  const signInStyle = await read('shop-mnp/pages/SignIn/SignIn.scss')

  assert.match(classify, /item\.listingSummary/)
  assert.match(classify, /compactListingText\(item\.description/)
  assert.match(classify, /v-else-if="!isActivityTab"[\s\S]*?class="stay-card travel-card"/)
  assert.match(classifyStyle, /\.travel-card\s*>\s*image\s*\{[\s\S]*?height:\s*220rpx/)
  assert.match(classifyStyle, /-webkit-line-clamp:\s*2/)
  assert.match(signInStyle, /\.day\s*\{[\s\S]*?min-height:\s*168rpx/)
})
