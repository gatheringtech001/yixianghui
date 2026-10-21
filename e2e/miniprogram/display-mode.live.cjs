// Run against the already-open native WeChat DevTools, never a new browser.
// No booking, payment, login or profile writes are performed.
const assert = require('node:assert/strict')
const Launcher = require('miniprogram-automator/out/Launcher').default
const delay = ms => new Promise(resolve => setTimeout(resolve, ms))

async function run() {
  const mini = await new Launcher().connectTool({wsEndpoint: process.env.MINIPROGRAM_WS || 'ws://127.0.0.1:9425'})
  const originalAuth = await mini.evaluate(() => JSON.stringify([wx.getStorageSync('token'), wx.getStorageSync('userInfo')]))
  let changedForm = false
  let originalForm
  const state = () => mini.evaluate(() => {
    const page = getCurrentPages().slice(-1)[0]
    return {route:page.route, senior:page.$vm.isSeniorMode, chosen:page.$vm.displayModeChosen}
  })
  async function route(url, name, screenshot = false) {
    await mini.reLaunch(url)
    await delay(800)
    const current = await state()
    assert.equal(current.route, url.split('?')[0].replace(/^\//,''))
    if (screenshot && process.env.DISPLAY_MODE_SCREENSHOTS) {
      await mini.screenshot({path:process.env.DISPLAY_MODE_SCREENSHOTS+'/'+name+'.png'})
    }
    return mini.currentPage()
  }
  async function choose(mode) {
    // uni-app custom components are not exposed by DevTools' page selector.
    // Invoke the same visible component handlers; no storage or API mocking.
    await delay(300)
    const found = await mini.evaluate(mode => {
      const vm = getCurrentPages().slice(-1)[0].$vm
      const picker = vm.$children.find(child => child.$options.name === 'DisplayModePicker' && child.visible)
      if (!picker) return false
      picker.pendingMode = mode
      picker.confirm()
      return true
    }, mode)
    assert.equal(found,true,'Visible display picker not found')
    await delay(350)
  }
  async function assertContained(page, parentSelector, childSelector) {
    const parent = await page.$(parentSelector)
    const child = await page.$(childSelector)
    assert.ok(parent && child, `${parentSelector} and ${childSelector} must be visible`)
    const [p, ps, c, cs] = await Promise.all([parent.offset(), parent.size(), child.offset(), child.size()])
    assert.ok(c.left >= p.left - 1 && c.left + cs.width <= p.left + ps.width + 1,
      `${childSelector} overflows ${parentSelector}: ${JSON.stringify({p,ps,c,cs})}`)
    assert.ok(c.top >= p.top - 1 && c.top + cs.height <= p.top + ps.height + 1,
      `${childSelector} vertically overflows ${parentSelector}`)
  }
  async function assertHomeLayout(page, mode) {
    assert.equal(await page.$('.display-mode-entry'), null, 'Home must not offer a mode switch')
    for (const selector of ['.housekeeper-avatar','.housekeeper-title','.housekeeper-slogan','.housekeeper-btn']) {
      await assertContained(page, '.housekeeper', selector)
    }
    await assertContained(page, '.head-search', '.search')
    const search = await page.$('.search')
    const head = await page.$('.head-search')
    assert.ok((await search.size()).width >= (await head.size()).width - 2, 'Search uses the full header')
    if (mode === 'senior') assert.ok((await (await page.$('.housekeeper-btn')).size()).height >= 48)
    const cards = await page.$$('.product-card')
    assert.ok(cards.length > 0, 'Production home must contain real product cards')
    for (const card of cards.slice(0, 3)) {
      const row = await card.$('.product-price-row')
      const btn = await card.$('.product-btn')
      const label = await btn.$('text')
      const [ro, rs, bo, bs, ls] = await Promise.all([row.offset(), row.size(), btn.offset(), btn.size(), label.size()])
      assert.ok(bo.left >= ro.left - 1 && bo.left + bs.width <= ro.left + rs.width + 1)
      assert.ok(ls.width <= bs.width && ls.height < 35, 'Action label must fit on one line')
      if (mode === 'senior') {
        const body = await card.$('.product-body')
        const image = await card.$('.product-cover')
        const [co, cs, io, is, to, ts] = await Promise.all([card.offset(), card.size(), image.offset(), image.size(), body.offset(), body.size()])
        assert.ok(rs.width >= cs.width - 30, 'Senior product footer uses the full card width')
        assert.ok(ro.top >= Math.max(io.top + is.height, to.top + ts.height) - 1, 'Footer must be below image and copy')
        assert.ok(bo.top + bs.height <= co.top + cs.height + 1)
        assert.ok(bs.width >= 88 && bs.height >= 48)
      }
    }
    if (process.env.DISPLAY_MODE_SCREENSHOTS) {
      const scroll = await page.$('.home-scroll')
      const first = await cards[0].offset()
      await scroll.scrollTo(0, Math.max(0, first.top - (await scroll.offset()).top))
      await delay(400)
      await mini.screenshot({path:process.env.DISPLAY_MODE_SCREENSHOTS+`/home-products-${mode}.png`})
      await scroll.scrollTo(0, 0)
    }
    console.log(`PASS ${mode} home: no mode switch, balanced housekeeper card, horizontal product actions`)
  }
  try {
    let page = await route('/packagesPublic/Setting/Setting','settings')
    await (await page.$('.settings-row')).tap()
    await choose('senior')
    assert.equal((await state()).senior,true)
    assert.equal((await state()).chosen,true)
    page = await route('/pages/home/home','home-senior',true)
    await assertHomeLayout(page, 'senior')
    assert.equal((await state()).senior,true)
    console.log('PASS settings mode picker, shared state and saved preference')

    page = await route('/pages/classify/classify?cls=58','service-senior',true)
    const box = await (await page.$('.body_goods_view')).size()
    const rail = await (await page.$('.category-rail')).size()
    const viewport = await mini.evaluate(() => wx.getWindowInfo())
    assert.ok(box.height > 120 && box.height < viewport.windowHeight, 'List must be a bounded scroll container')
    assert.ok(box.width <= viewport.windowWidth && box.width > viewport.windowWidth * .8, 'List must use the available width')
    assert.equal(rail.height,56)
    const primary = await page.$('.btn-primary')
    assert.ok(primary, 'Real production courses are visible')
    assert.ok((await primary.size()).height >= 48)
    assert.equal(await (await page.$('.card-title')).style('font-size'),'22px')
    console.log('PASS production catalog, full-width layout, 48px action and 22px title')

    page = await route('/packagesMall/GoodsDetails/EducationGoodsDetails?id=279','course-senior',true)
    assert.equal(await (await page.$('.course-name')).style('font-size'),'22px')
    assert.ok((await (await page.$('.bottom-action .primary')).size()).height >= 48)
    const grid = await (await page.$('.course-info-grid')).size()
    assert.ok(grid.width > 250)
    await route('/packagesMall/Activity/detail/index?id=151','activity-senior',true)
    console.log('PASS live course and activity detail pages')

    page = await route('/packagesMall/ConfirmOrder/ActivityConfirmOrder?id=151','signup-senior')
    originalForm = await mini.evaluate(() => {
      const vm = getCurrentPages().slice(-1)[0].$vm
      return {name:vm.signName,mobile:vm.signMobile,attempted:vm.formAttempted}
    })
    changedForm = true
    await mini.evaluate(() => {
      const vm = getCurrentPages().slice(-1)[0].$vm
      vm.signName = ''
      vm.signMobile = ''
    })
    await (await page.$('.pay_button_view')).tap()
    await delay(300)
    assert.ok(await page.$('.field-error'), 'Invalid fields must have persistent inline messages')
    await mini.evaluate(() => {
      const vm = getCurrentPages().slice(-1)[0].$vm
      vm.signName = '适老界面测试'
      vm.signMobile = '13800000000'
    })
    await mini.navigateTo('/packagesPublic/Setting/Setting')
    await delay(300)
    page = await mini.currentPage()
    await (await page.$('.settings-row')).tap()
    await choose('standard')
    await mini.navigateBack()
    await delay(300)
    assert.equal((await state()).senior,false)
    const preserved = await mini.evaluate(() => {
      const vm = getCurrentPages().slice(-1)[0].$vm
      return vm.signName === '适老界面测试' && vm.signMobile === '13800000000'
    })
    assert.equal(preserved,true)
    await mini.evaluate(original => {
      const vm = getCurrentPages().slice(-1)[0].$vm
      vm.signName = original.name
      vm.signMobile = original.mobile
      vm.formAttempted = original.attempted
    },originalForm)
    changedForm = false
    console.log('PASS inline validation, settings switch and unsubmitted form preservation')

    await route('/pages/my/my','profile-standard')
    await mini.evaluate(() => getCurrentPages().slice(-1)[0].$vm.$refs.authProfilePopup.open(()=>{},()=>null))
    await delay(300)
    const repeated = await mini.evaluate(() => {
      const auth = getCurrentPages().slice(-1)[0].$vm.$refs.authProfilePopup
      const picker = auth.$children.find(child => child.$options.name === 'DisplayModePicker')
      return {authVisible:auth.visible, exists:!!picker, pickerVisible:picker && picker.visible}
    })
    assert.deepEqual(repeated,{authVisible:true,exists:true,pickerVisible:false})
    await mini.evaluate(() => getCurrentPages().slice(-1)[0].$vm.$refs.authProfilePopup.onCancel())
    page = await route('/pages/home/home','home-standard-final',true)
    await assertHomeLayout(page, 'standard')
    assert.equal((await state()).senior,false)
    const finalAuth = await mini.evaluate(() => JSON.stringify([wx.getStorageSync('token'), wx.getStorageSync('userInfo')]))
    assert.equal(finalAuth,originalAuth, 'Display mode must not mutate the account session')
    console.log('PASS no repeated first-choice prompt, standard-mode restore and unchanged login')
  } finally {
    if (changedForm && originalForm) {
      await mini.evaluate(original => {
        const vm = getCurrentPages().slice(-1)[0].$vm
        if ('signName' in vm) { vm.signName=original.name; vm.signMobile=original.mobile; vm.formAttempted=original.attempted }
      },originalForm).catch(()=>{})
    }
    mini.disconnect()
  }
}
run().catch(error => {console.error(error.message);process.exitCode=1})
