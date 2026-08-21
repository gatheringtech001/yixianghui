const assert = require('node:assert/strict')
const fs = require('node:fs/promises')
const path = require('node:path')
const test = require('node:test')

const appRoot = path.resolve(__dirname, '../../shop-mnp')

async function loadInviteModule() {
  const source = await fs.readFile(path.join(appRoot, 'utils/invite.js'), 'utf8')
  const encoded = Buffer.from(source).toString('base64')
  return import(`data:text/javascript;base64,${encoded}`)
}

async function loadSafeBackModule() {
  const source = await fs.readFile(path.join(appRoot, 'utils/safeBack.js'), 'utf8')
  const encoded = Buffer.from(source).toString('base64')
  return import(`data:text/javascript;base64,${encoded}`)
}

test('share helpers enable friend, timeline and copy-url parameters', async () => {
  const calls = {}
  global.uni = {
    getStorageSync(key) {
      return key === 'userInfo' ? { userId: 7 } : null
    },
    showShareMenu(options) {
      calls.menu = options
      if (options.success) options.success()
    }
  }
  global.wx = {
    onCopyUrl(handler) {
      calls.copyHandler = handler
    },
    offCopyUrl() {
      calls.copyRemoved = true
    }
  }

  const {
    bindCopyUrl,
    buildShareTimeline,
    enableShareMenu,
    unbindCopyUrl
  } = await loadInviteModule()

  assert.equal(enableShareMenu(), true)
  assert.deepEqual(calls.menu.menus, ['shareAppMessage', 'shareTimeline'])
  assert.deepEqual(
    buildShareTimeline({ title: '昆明古滇基地', query: { id: 32 } }),
    { title: '昆明古滇基地', query: 'id=32&parentUserId=7' }
  )

  assert.equal(bindCopyUrl(() => ({
    title: '昆明古滇基地',
    query: { id: 32 }
  })), true)
  assert.deepEqual(calls.copyHandler(), {
    title: '昆明古滇基地',
    query: 'id=32&parentUserId=7'
  })
  unbindCopyUrl()
  assert.equal(calls.copyRemoved, true)
})

test('all public content and invitation pages use the unified share mixin', async () => {
  const pages = [
    'pages/home/home.vue',
    'pages/classify/classify.vue',
    'pages/SignIn/SignIn.vue',
    'pages/MembersOpened/MembersOpened.vue',
    'packagesMall/Activity/detail/index.vue',
    'packagesMall/GoodsDetails/GoodsDetails.vue',
    'packagesMall/GoodsDetails/SojournGoodsDetails.vue',
    'packagesMall/GoodsDetails/EducationGoodsDetails.vue',
    'packagesMember/retail/invite/index.vue',
    'packagesPublic/Article/index.vue'
  ]

  for (const page of pages) {
    const source = await fs.readFile(path.join(appRoot, page), 'utf8')
    assert.match(source, /import sharePageMixin from ['"]@\/utils\/sharePageMixin['"]/, page)
    assert.match(source, /mixins:\s*\[sharePageMixin\]/, page)
    assert.match(source, /getShareConfig\s*\(/, page)
  }
})

test('travel and education detail footers expose native share before contact', async () => {
  const cases = [
    {
      page: 'packagesMall/GoodsDetails/SojournGoodsDetails.vue',
      style: 'packagesMall/GoodsDetails/SojournGoodsDetails.scss',
      footerClass: 'page_foot_view',
      contactText: '联系客服',
      columns: /grid-template-columns:\s*180rpx\s+180rpx\s+1fr/
    },
    {
      page: 'packagesMall/GoodsDetails/EducationGoodsDetails.vue',
      style: 'packagesMall/GoodsDetails/EducationGoodsDetails.scss',
      footerClass: 'bottom-action',
      contactText: 'labels.consultPhone',
      columns: /grid-template-columns:\s*1fr\s+1fr\s+1\.35fr/
    }
  ]

  for (const item of cases) {
    const source = await fs.readFile(path.join(appRoot, item.page), 'utf8')
    const style = await fs.readFile(path.join(appRoot, item.style), 'utf8')
    const footerStart = source.indexOf(`class="${item.footerClass}"`)
    const footer = source.slice(footerStart, source.indexOf('</view>', footerStart) + 7)

    assert.notEqual(footerStart, -1, item.page)
    assert.match(footer, /<button[^>]*open-type="share"[^>]*>[\s\S]*?分享[\s\S]*?<\/button>/, item.page)
    assert.ok(footer.indexOf('open-type="share"') < footer.indexOf(item.contactText), item.page)
    assert.match(style, item.columns, item.style)
    assert.match(style, /\.share-button\s*\{[\s\S]*?&::after\s*\{[\s\S]*?border:\s*none/, item.style)
  }
})

test('safe back returns to the previous page when the page stack has history', async t => {
  const calls = []
  global.getCurrentPages = () => [{ route: 'pages/home/home' }, { route: 'packagesMall/Activity/detail/index' }]
  global.uni = {
    navigateBack() {
      calls.push({ method: 'navigateBack' })
    },
    switchTab(options) {
      calls.push({ method: 'switchTab', options })
    }
  }
  t.after(() => {
    delete global.getCurrentPages
    delete global.uni
  })

  const { safeBack } = await loadSafeBackModule()
  safeBack()

  assert.deepEqual(calls, [{ method: 'navigateBack' }])
})

test('safe back opens the home tab when a shared deep link is the only page', async t => {
  const calls = []
  global.getCurrentPages = () => [{ route: 'packagesMall/Activity/detail/index' }]
  global.uni = {
    navigateBack() {
      calls.push({ method: 'navigateBack' })
    },
    switchTab(options) {
      calls.push({ method: 'switchTab', options })
    }
  }
  t.after(() => {
    delete global.getCurrentPages
    delete global.uni
  })

  const { safeBack } = await loadSafeBackModule()
  safeBack()

  assert.deepEqual(calls, [{
    method: 'switchTab',
    options: { url: '/pages/home/home' }
  }])
})

test('navbar preserves custom back handling and uses safe back by default', async () => {
  const source = await fs.readFile(
    path.join(appRoot, 'uview-ui/components/u-navbar/u-navbar.vue'),
    'utf8'
  )

  assert.match(source, /import \{ safeBack \} from ['"]@\/utils\/safeBack['"]/)
  assert.match(source, /if \(typeof this\.customBack === ['"]function['"]\)[\s\S]*this\.customBack\.bind[\s\S]*else \{\s*safeBack\(\)/)
})
