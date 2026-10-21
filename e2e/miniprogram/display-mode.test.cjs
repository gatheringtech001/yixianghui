const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const loadPage = require('./vue-page-harness.cjs')
const read = file => fs.readFileSync(path.resolve(__dirname, '../../shop-mnp', file), 'utf8')

function load(saved) {
  const values = new Map(saved === undefined ? [] : [['yxh-display-mode-v1', saved]])
  const storage = {getStorageSync:key=>values.get(key), setStorageSync:(key,value)=>values.set(key,value)}
  const context = {uni:storage}
  vm.createContext(context)
  vm.runInContext(read('utils/displayMode.js').replace(/export /g,''), context)
  return {context, storage, values, preference: vm.runInContext('createDisplayModePreference(uni)', context)}
}

test('first login has no inferred age or preselected permanent preference', () => {
  for (const invalid of [undefined, '', 'old', {}, true]) {
    const {preference} = load(invalid)
    assert.equal(preference.state.chosen, false)
    assert.equal(preference.state.mode, 'standard')
  }
})

test('both modes survive restart and no longer require a first choice', () => {
  const {context, preference} = load()
  for (const mode of ['senior','standard']) {
    assert.equal(preference.select(mode), true)
    assert.equal(preference.state.mode, mode)
    const restarted = vm.runInContext('createDisplayModePreference(uni)', context)
    assert.equal(restarted.state.mode, mode)
    assert.equal(restarted.state.chosen, true)
  }
  assert.equal(preference.select('unknown'), false)
  assert.equal(preference.state.mode, 'standard')
})

test('storage failures do not falsely report saved choices or break browsing', () => {
  const {context} = load()
  context.broken = {getStorageSync(){throw Error('read')},setStorageSync(){throw Error('quota')}}
  const result = vm.runInContext('createDisplayModePreference(broken)', context)
  assert.equal(result.state.mode, 'standard')
  assert.equal(result.select('senior'), false)
  assert.equal(result.state.chosen, false)
})

test('logout only clears account caches and preserves the display choice', () => {
  const {values, preference} = load()
  preference.select('senior')
  for (const key of ['token','userInfo','consultant']) values.set(key,'test')
  vm.runInNewContext(read('utils/sessionStorage.js').replace('export ','' )+'\nclearAuthStorage()', {uni:{removeStorageSync:key=>values.delete(key)}})
  assert.equal(values.get('yxh-display-mode-v1'), 'senior')
  assert.equal(values.has('token'), false)
  assert.equal(values.has('userInfo'), false)
})

test('switching updates the shared view state and remeasures without navigation', () => {
  const {context} = load()
  let mixin
  context.Vue = {observable:value=>value, mixin:value=>{mixin=value}}
  vm.runInContext('installDisplayMode(Vue);selectDisplayMode("senior")',context)
  assert.equal(mixin.computed.isSeniorMode(),true)
  assert.equal(mixin.computed.displayModeChosen(),true)
  const calls=[]
  mixin.watch.isSeniorMode.call({$nextTick:fn=>fn(), setPageScrollHeight:()=>calls.push('measure')})
  assert.deepEqual(calls,['measure'])
  assert.doesNotMatch(read('utils/displayMode.js'),/uni\.(reLaunch|navigateTo|switchTab)|removeStorageSync/)
})

test('guest settings are accessible while private profile and address actions remain gated', () => {
  const navigations=[]
  const {page} = loadPage('pages/my/my.vue', {TabBar:{}, AuthProfilePopup:{}, uni:{navigateTo:args=>navigations.push(args.url)}})
  page.onSetting()
  assert.deepEqual(navigations,['/packagesPublic/Setting/Setting'])
  let allowed=false
  const settings=loadPage('packagesPublic/Setting/Setting.vue', {AuthProfilePopup:{},runWithAuth:(_,fn)=>fn(allowed),uni:{navigateTo:args=>navigations.push(args.url)}}).page
  settings.onAddress()
  settings.onSetting('account')
  assert.equal(navigations.length,1)
  allowed=true
  settings.onAddress()
  assert.equal(navigations[1],'/packagesPublic/AddressList/AddressList')
})

test('first-choice prompt belongs to auth only; home and settings open it explicitly', () => {
  assert.match(read('components/AuthProfilePopup/AuthProfilePopup.vue'),/:visible="!displayModeChosen" first-choice/)
  for(const file of ['pages/home/home.vue','packagesPublic/Setting/Setting.vue']) {
    assert.match(read(file),/:visible="showDisplayMode"/)
    assert.match(read(file),/showDisplayMode: false/)
  }
  assert.doesNotMatch(read('components/DisplayModePicker/DisplayModePicker.vue'),/coupon|token|userInfo|request\(/i)
})

test('rich text enlarges baked-in HTML font styles but never rewrites images or business text', () => {
  const {context} = load()
  const transform=vm.runInContext('readableRichText',context)
  const html='<p style="font-size:10px;color:#aaa;width:100%"><span>费用500元</span><img src="/a.jpg" style="width:100%"></p>'
  assert.equal(transform(html,false),html)
  const senior=transform(html,true)
  assert.match(senior,/font-size:18px!important/)
  assert.doesNotMatch(senior,/font-size:10px|color:#aaa/)
  assert.match(senior,/费用500元/)
  assert.match(senior,/<img src="\/a.jpg" style="width:100%">/)
  assert.match(transform('<strong style=font-size:10px>重要说明</strong>',true),/font-size:18px!important/)
  assert.match(transform('<a href="\/x">点击查看</a>',true),/color:#701018!important/)
  assert.match(transform(`<p style='font-family:"Arial";font-size:12px'>正文</p>`,true),/style='font-family:"Arial";/)
})

test('both modes retain the same homepage sections and senior mode stops automatic rotation', () => {
  const home=read('pages/home/home.vue')
  assert.match(home,/:autoplay="!isSeniorMode"/)
  assert.match(home,/:autoplay="!isSeniorMode && hotCardList.length > 5"/)
  for(const label of ['全国旅居','云南好物','聚会活动','芳华学院']) assert.match(home,new RegExp('entry-title">'+label))
  const style=read('style/senior-mode.scss')
  assert.match(read('style/senior-tokens.scss'),/\$senior-body: 18px/)
  assert.match(read('style/senior-tokens.scss'),/\$senior-touch: 48px/)
  assert.doesNotMatch(style,/transform:\s*scale|zoom:/)
})

test('home mode entry names the current mode and housekeeper grid can shrink safely', () => {
  const home = read('pages/home/home.vue')
  assert.match(home, /class="display-mode-label">\{\{ displayModeLabel \}\}/)
  assert.doesNotMatch(home, /大字模式/)
  assert.match(home, /当前.*displayModeLabel.*点击切换显示模式/)
  const style = read('style/senior-mode.scss')
  assert.match(style, /\.housekeeper \{ grid-template-columns: 48px minmax\(0, 1fr\)/)
  assert.match(style, /\.housekeeper \.housekeeper-btn \{ grid-column: 2/)
  assert.doesNotMatch(style, /\.housekeeper-copy\s*\{[^}]*min-width:\s*180px/)
  assert.match(read('pages/home/home.scss'), /grid-template-columns: 100rpx minmax\(0, 1fr\) auto/)
})

test('service scroll styles are native CSS strings with a bounded senior catalog height', () => {
  const {page} = loadPage('pages/classify/classify.vue', {TabBar:{},AuthProfilePopup:{},sharePageMixin:{}})
  page.bodyScrollHeight=514
  page.isSeniorMode=true
  assert.equal(page.categoryScrollStyle,'height:56px')
  assert.equal(page.goodsScrollStyle,'height:458px')
  page.isSeniorMode=false
  assert.equal(page.categoryScrollStyle,'height:514px')
  assert.equal(page.goodsScrollStyle,'height:514px')
})

test('signup errors remain inline until corrected and name and phone targets are labelled', () => {
  for(const file of ['ActivityConfirmOrder','EducationConfirmOrder']) {
    const source=read('packagesMall/ConfirmOrder/'+file+'.vue')
    assert.match(source,/:scroll-into-view="formErrorTarget"/)
    assert.match(source,/id="signup-name"/)
    assert.match(source,/id="signup-phone"/)
    assert.match(source,/formAttempted && nameError/)
    assert.match(source,/formAttempted && phoneError/)
  }
})
