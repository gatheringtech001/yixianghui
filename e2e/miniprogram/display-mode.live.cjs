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
  try {
    let page = await route('/pages/home/home','home-standard')
    await (await page.$('.display-mode-entry')).tap()
    await choose('senior')
    assert.equal((await state()).senior,true)
    assert.equal((await state()).chosen,true)
    await route('/pages/home/home','home-senior',true)
    assert.equal((await state()).senior,true)
    console.log('PASS home mode picker, shared state and saved preference')

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
    await route('/pages/home/home','home-standard-final',true)
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
