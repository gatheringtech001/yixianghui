const assert = require('node:assert/strict')
const fs = require('node:fs/promises')
const path = require('node:path')
const test = require('node:test')
const zlib = require('node:zlib')

const projectRoot = path.resolve(__dirname, '../..')

async function read(relativePath) {
  return fs.readFile(path.join(projectRoot, relativePath), 'utf8')
}

function getPngChunks(image) {
  const chunks = []
  let offset = 8

  while (offset + 12 <= image.length) {
    const length = image.readUInt32BE(offset)
    const type = image.toString('ascii', offset + 4, offset + 8)
    chunks.push({ type, data: image.subarray(offset + 8, offset + 8 + length) })
    offset += length + 12
  }

  return chunks
}

function getTopLeftPaletteAlpha(image) {
  const chunks = getPngChunks(image)
  const header = chunks.find(({ type }) => type === 'IHDR')?.data
  const transparency = chunks.find(({ type }) => type === 'tRNS')?.data
  const compressed = chunks
    .filter(({ type }) => type === 'IDAT')
    .map(({ data }) => data)

  assert.equal(header?.[8], 8, 'icon should use 8-bit color')
  assert.equal(header?.[9], 3, 'icon should use indexed color')
  assert.ok(transparency, 'icon should include palette transparency')

  const scanlines = zlib.inflateSync(Buffer.concat(compressed))
  const topLeftPaletteIndex = scanlines[1]
  return transparency[topLeftPaletteIndex] ?? 255
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

test('settings page uses the profile design system and keeps every existing destination', async () => {
  const source = await read('shop-mnp/packagesPublic/Setting/Setting.vue')
  const style = await read('shop-mnp/packagesPublic/Setting/Setting.scss')

  assert.match(source, /class="settings-hero"/)
  assert.match(source, /class="settings-card"/)
  assert.match(source, /账户与服务/)
  assert.match(source, /品牌与支持/)
  assert.match(source, /class="logout-button"/)
  assert.match(source, /onAddress\(\)/)
  assert.match(source, /onSetting\('account'\)/)
  assert.match(source, /onSetting\('vip'\)/)
  assert.match(source, /onSetting\('about'\)/)
  assert.match(style, /\$accent:\s*#701018/)
  assert.match(style, /\$soft:\s*#f7f7f5/)
  assert.match(style, /\.settings-card\s*\{[\s\S]*?box-shadow:/)
  assert.doesNotMatch(style, /position:\s*fixed/)
})

test('profile identity sits lower and personal information follows the current account design', async () => {
  const myStyle = await read('shop-mnp/pages/my/my.scss')
  const information = await read('shop-mnp/packagesPublic/Information/Information.vue')
  const informationStyle = await read('shop-mnp/packagesPublic/Information/Information.scss')

  assert.match(myStyle, /\.profile-info\s*\{[\s\S]*?padding:\s*36rpx\s+0\s+2rpx/)
  assert.match(information, /class="profile-summary"/)
  assert.match(information, /class="information-card"/)
  assert.match(information, /基本信息/)
  assert.match(information, /更多信息/)
  assert.match(information, /avatarDisplay/)
  assert.match(information, /open-type="chooseAvatar"/)
  assert.match(information, /@chooseavatar="onChooseAvatar"/)
  assert.match(information, /uploadUserAvatar/)
  assert.match(information, /updateInfo\(\{ avatar \}\)/)
  assert.match(information, /await getInfo\(\)/)
  assert.match(information, /uni\.setStorageSync\('userInfo', userInfo\)/)
  assert.match(information, /\/static\/home-design\/brand-logo-transparent\.png/)
  assert.match(information, /birthday\s*\|\|\s*'未设置'/)
  assert.doesNotMatch(information, /2020-02-02/)
  assert.doesNotMatch(information, /管理头像与联系方式/)
  assert.doesNotMatch(information, /让服务更贴合您的需要/)
  assert.match(informationStyle, /\$accent:\s*#701018/)
  assert.match(informationStyle, /\.information-card\s*\{[\s\S]*?border-radius:\s*24rpx/)
  assert.doesNotMatch(informationStyle, /\.page\s*\{[^}]*position:\s*absolute/)
})

test('profile uses one premium generated icon set at production size', async () => {
  const source = await read('shop-mnp/pages/my/my.vue')
  const style = await read('shop-mnp/pages/my/my.scss')
  const icons = [
    'settings',
    'order-payment',
    'order-shipping',
    'order-receive',
    'order-refund',
    'service-advisor',
    'service-address',
    'service-favorite'
  ]
  let totalBytes = 0

  for (const icon of icons) {
    assert.match(source, new RegExp(`/static/profile-icons/${icon}\\.png`))
    const image = await fs.readFile(path.join(
      projectRoot,
      `shop-mnp/static/profile-icons/${icon}.png`
    ))
    assert.equal(image.readUInt32BE(16), 128, `${icon} width`)
    assert.equal(image.readUInt32BE(20), 128, `${icon} height`)
    assert.equal(getTopLeftPaletteAlpha(image), 0, `${icon} background should be transparent`)
    assert.ok(image.length < 10000, `${icon} should stay package-friendly`)
    totalBytes += image.length
  }

  assert.ok(totalBytes < 70000, 'profile icon set should stay below 70 KB')
  assert.doesNotMatch(source, /order-(?:unpaid|ship|receive|review)\.svg/)
  assert.match(style, /\.settings-icon\s*\{/)
  assert.match(style, /\.service-icon\s*\{/)
})

test('navigation and service tabs use refined transparent flat icons without a red underline', async () => {
  const tabBar = await read('shop-mnp/components/TabBar/TabBar.vue')
  const tabBarStyle = await read('shop-mnp/components/TabBar/TabBar.scss')
  const service = await read('shop-mnp/pages/classify/classify.vue')
  const serviceStyle = await read('shop-mnp/pages/classify/classify.scss')
  const tabIcons = ['nav-home', 'nav-service', 'nav-coins', 'nav-support', 'nav-profile']
  const serviceIcons = ['service-travel', 'service-activity', 'service-education']
  const icons = [...tabIcons, ...serviceIcons]
  let totalBytes = 0

  for (const icon of tabIcons) {
    assert.match(tabBar, new RegExp(`/static/navigation-icons/${icon}\\.png`))
  }
  for (const icon of serviceIcons) {
    assert.match(service, new RegExp(`/static/navigation-icons/${icon}\\.png`))
  }
  for (const icon of icons) {
    const image = await fs.readFile(path.join(
      projectRoot,
      `shop-mnp/static/navigation-icons/${icon}.png`
    ))
    assert.equal(image.readUInt32BE(16), 96, `${icon} width`)
    assert.equal(image.readUInt32BE(20), 96, `${icon} height`)
    assert.equal(getTopLeftPaletteAlpha(image), 0, `${icon} background should be transparent`)
    assert.ok(image.length < 10000, `${icon} should stay package-friendly`)
    totalBytes += image.length
  }

  const switcherMarkup = service.slice(
    service.indexOf('<view class="switcher">'),
    service.indexOf('<view class="page_body_view">')
  )
  const switcherStyle = serviceStyle.slice(
    serviceStyle.indexOf('.switcher {'),
    serviceStyle.indexOf('.page_body_view')
  )
  assert.ok(totalBytes < 70000, 'navigation icon set should stay below 70 KB')
  assert.doesNotMatch(tabBar, /\/static\/home-design\/tab-/)
  assert.match(tabBarStyle, /\.tab-icon\s*\{[\s\S]*?width:\s*48rpx;[\s\S]*?height:\s*48rpx;[\s\S]*?opacity:\s*0\.68/)
  assert.match(tabBarStyle, /&\.active\s*\{[\s\S]*?\.tab-icon\s*\{[\s\S]*?opacity:\s*1/)
  assert.doesNotMatch(switcherMarkup, /<u-icon/)
  assert.match(switcherStyle, /\.switcher-icon\s*\{[\s\S]*?width:\s*42rpx;[\s\S]*?height:\s*42rpx/)
  assert.match(switcherStyle, /&\.active\s*\{[\s\S]*?\.switcher-icon\s*\{[\s\S]*?filter:\s*brightness\(0\)\s+invert\(1\)/)
  assert.doesNotMatch(switcherStyle, /&::after/)
})

test('about us presents a concise customer-facing company introduction', async () => {
  const settings = await read('shop-mnp/packagesPublic/Setting/Setting.vue')
  const about = await read('shop-mnp/packagesPublic/AboutUs/AboutUs.vue')
  const style = await read('shop-mnp/packagesPublic/AboutUs/AboutUs.scss')

  assert.match(settings, /url:\s*'\/packagesPublic\/AboutUs\/AboutUs'/)
  assert.match(about, /上海智享居健康科技有限公司/)
  assert.match(about, /让美好退休生活自然发生/)
  assert.match(about, /class="footer-logo" src="\/static\/home-design\/brand-logo-transparent\.png"/)
  assert.doesNotMatch(about, /class="hero-mark"/)
  assert.match(about, /康养旅居/)
  assert.match(about, /社区康养/)
  assert.match(about, /专业照护/)
  assert.doesNotMatch(about, /全国 50\+ 旅居基地/)
  assert.doesNotMatch(about, /五大业务板块/)
  assert.doesNotMatch(about, /双翼服务模式/)
  assert.doesNotMatch(about, /OUR JOURNEY/)
  assert.doesNotMatch(about, /资料更新于/)
  assert.doesNotMatch(about, /getNoticeInfo/)
  assert.match(style, /\$accent:\s*#701018/)
  assert.match(style, /\.hero-card\s*\{[\s\S]*?border-radius:/)
  assert.doesNotMatch(style, /\.footer-logo\s*\{[^}]*background:/)
  assert.doesNotMatch(style, /\.footer-logo\s*\{[^}]*border-radius:/)
})

test('settings and authorization surfaces use the official Yixianghui logo', async () => {
  const settings = await read('shop-mnp/packagesPublic/Setting/Setting.vue')
  const authorization = await read('shop-mnp/components/AuthProfilePopup/AuthProfilePopup.vue')

  for (const source of [settings, authorization]) {
    assert.match(source, /\/static\/home-design\/brand-logo-transparent\.png/)
    assert.doesNotMatch(source, /\/static\/home-design\/brand-mark\.png/)
  }
})
