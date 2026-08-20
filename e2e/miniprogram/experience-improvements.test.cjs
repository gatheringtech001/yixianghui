const assert = require('node:assert/strict')
const fs = require('node:fs/promises')
const path = require('node:path')
const test = require('node:test')
const {
  getPaletteAlphaBounds,
  getTopLeftPaletteAlpha
} = require('./png-test-helpers.cjs')

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
  assert.match(source, /登录逸享荟，开启美好退休生活/)
  assert.doesNotMatch(source, /授权登录后/)
  assert.match(source, /class="desc login-desc"/)
  assert.match(source, /\.login-desc\s*\{[^}]*white-space:\s*nowrap/)
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

test('authorization policy links use the existing published single-page records', async () => {
  const authorization = await read('shop-mnp/components/AuthProfilePopup/AuthProfilePopup.vue')
  const article = await read('shop-mnp/packagesPublic/Article/index.vue')

  assert.doesNotMatch(authorization, /\$store/)
  assert.match(authorization, /POLICY_PAGE_IDS\s*=\s*\{\s*agreement:\s*1,\s*privacy:\s*5\s*\}/)
  assert.match(authorization, /const articleId = POLICY_PAGE_IDS\[type\]/)
  assert.match(authorization, /Article\/index\?id=\$\{articleId\}/)
  assert.match(article, /errorMessage/)
  assert.match(article, /内容加载失败，请稍后重试/)
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
  const settingIcons = ['address', 'security', 'membership', 'about']

  assert.match(source, /class="settings-hero"/)
  assert.match(source, /class="hero-background"/)
  assert.match(source, /\/static\/account\/account-center-background\.jpg/)
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
  assert.match(style, /\.hero-background\s*\{[\s\S]*?position:\s*absolute/)
  assert.doesNotMatch(style, /position:\s*fixed/)

  for (const icon of settingIcons) {
    assert.match(source, new RegExp(`/static/settings-icons/${icon}\\.png`))
    const image = await fs.readFile(path.join(
      projectRoot,
      `shop-mnp/static/settings-icons/${icon}.png`
    ))
    assert.equal(image.readUInt32BE(16), 128, `${icon} width`)
    assert.equal(image.readUInt32BE(20), 128, `${icon} height`)
    assert.equal(getTopLeftPaletteAlpha(image), 0, `${icon} background should be transparent`)
    assert.deepEqual(
      getPaletteAlphaBounds(image),
      { x: 16, y: 16, width: 96, height: 96 },
      `${icon} should share one centered visible footprint`
    )
    assert.ok(image.length < 10000, `${icon} should stay package-friendly`)
  }
})

test('profile identity sits lower and personal information follows the current account design', async () => {
  const mySource = await read('shop-mnp/pages/my/my.vue')
  const myStyle = await read('shop-mnp/pages/my/my.scss')
  const information = await read('shop-mnp/packagesPublic/Information/Information.vue')
  const informationStyle = await read('shop-mnp/packagesPublic/Information/Information.scss')

  assert.match(myStyle, /\.profile-info\s*\{[\s\S]*?padding:\s*36rpx\s+0\s+2rpx/)
  assert.match(myStyle, /\.profile-head\s*\{[\s\S]*?padding:\s*124rpx\s+40rpx\s+48rpx/)
  assert.match(mySource, /const baseTop = uni\.upx2px\(124\)/)
  assert.match(mySource, /statusBarHeight \|\| 20\) \+ uni\.upx2px\(72\)/)
  assert.match(mySource, /menuButton\.bottom \+ uni\.upx2px\(48\)/)
  assert.match(information, /class="profile-summary"/)
  assert.match(information, /class="summary-background"/)
  assert.match(information, /\/static\/account\/account-center-background\.jpg/)
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
  assert.match(informationStyle, /\.summary-background\s*\{[\s\S]*?position:\s*absolute/)
  assert.doesNotMatch(informationStyle, /\.page\s*\{[^}]*position:\s*absolute/)

  const background = await fs.readFile(path.join(
    projectRoot,
    'shop-mnp/static/account/account-center-background.jpg'
  ))
  assert.deepEqual([...background.subarray(0, 3)], [0xff, 0xd8, 0xff])
  assert.ok(background.length < 30000, 'shared account background should stay package-friendly')
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
    const expectedBounds = icon === 'service-travel'
      ? { x: 24, y: 12, width: 48, height: 72 }
      : { x: 12, y: 12, width: 72, height: 72 }
    assert.deepEqual(getPaletteAlphaBounds(image), expectedBounds, `${icon} visible footprint`)
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
  assert.match(about, /让美好退休生活[\s\S]*自然发生/)
  assert.match(about, /class="hero-title-line"/)
  assert.match(about, /class="hero-background"/)
  assert.match(about, /\/static\/about\/about-hero-background\.jpg/)
  assert.match(about, /class="footer-logo" src="\/static\/home-design\/brand-logo-transparent\.png"/)
  assert.doesNotMatch(about, /hero-summary/)
  assert.doesNotMatch(about, /陪您发现好去处、认识新朋友/)
  assert.doesNotMatch(about, /class="hero-mark"/)
  assert.match(about, /康养旅居/)
  assert.match(about, /社区康养/)
  assert.match(about, /专业照护/)
  assert.match(about, /badge:\s*'旅'[\s\S]*badge:\s*'居'[\s\S]*badge:\s*'护'/)
  assert.doesNotMatch(about, /badge:\s*'康'/)
  assert.doesNotMatch(about, /全国 50\+ 旅居基地/)
  assert.doesNotMatch(about, /五大业务板块/)
  assert.doesNotMatch(about, /双翼服务模式/)
  assert.doesNotMatch(about, /OUR JOURNEY/)
  assert.doesNotMatch(about, /资料更新于/)
  assert.doesNotMatch(about, /getNoticeInfo/)
  assert.doesNotMatch(about, /我们的心愿/)
  assert.doesNotMatch(about, /promise-card/)
  assert.match(style, /\$accent:\s*#701018/)
  assert.match(style, /\.about-shell\s*\{[^}]*padding:\s*24rpx 28rpx calc\(56rpx \+ env\(safe-area-inset-bottom\)\);/)
  assert.match(style, /\.hero-card\s*\{[^}]*min-height:\s*280rpx;[^}]*border-radius:/)
  assert.match(style, /\.hero-brand\s*\{[^}]*font-size:\s*25rpx;/)
  assert.match(style, /\.hero-title\s*\{[^}]*margin-top:\s*18rpx;[^}]*font-size:\s*41rpx;/)
  assert.match(style, /\.hero-background\s*\{[\s\S]*?position:\s*absolute/)
  assert.match(style, /\.intro-card\s*\{[^}]*margin-top:\s*22rpx;[^}]*padding:\s*34rpx 32rpx;/)
  assert.match(style, /\.service-card\s*\{[^}]*gap:\s*22rpx;[^}]*padding:\s*28rpx 26rpx;/)
  assert.doesNotMatch(style, /\.promise-/)
  assert.match(style, /\.service-badge::before\s*\{/)
  assert.match(style, /\.service-badge\s*\{[\s\S]*?font-family:[^;]*KaiTi/)
  assert.doesNotMatch(style, /\.footer-logo\s*\{[^}]*background:/)
  assert.doesNotMatch(style, /\.footer-logo\s*\{[^}]*border-radius:/)
  assert.match(style, /\.footer-logo\s*\{[^}]*width:\s*112rpx;[^}]*height:\s*112rpx;/)

  const background = await fs.readFile(path.join(
    projectRoot,
    'shop-mnp/static/about/about-hero-background.jpg'
  ))
  assert.deepEqual([...background.subarray(0, 3)], [0xff, 0xd8, 0xff])
  assert.ok(background.length < 50000, 'about background should stay package-friendly')
})

test('settings and authorization surfaces use the official Yixianghui logo', async () => {
  const settings = await read('shop-mnp/packagesPublic/Setting/Setting.vue')
  const authorization = await read('shop-mnp/components/AuthProfilePopup/AuthProfilePopup.vue')

  for (const source of [settings, authorization]) {
    assert.match(source, /\/static\/home-design\/brand-logo-transparent\.png/)
    assert.doesNotMatch(source, /\/static\/home-design\/brand-mark\.png/)
  }
})
