const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const loadPage = require('./vue-page-harness.cjs')
const read = file => fs.readFileSync(path.resolve(__dirname, '../../shop-mnp', file), 'utf8')
const categories = [
  {categoryId:25,parentId:0,categoryName:'全国旅居'},
  {categoryId:83,parentId:0,categoryName:'云南好物'},
  {categoryId:33,parentId:0,categoryName:' 云野好物'},
  {categoryId:41,parentId:0,categoryName:'老年教育'},
  {categoryId:42,parentId:0,categoryName:'聚会活动',linkType:'activity'},
  {categoryId:50,parentId:0,categoryName:'商城'},
  {categoryId:26,parentId:25,categoryName:'版纳'}
]
function service() {
  return loadPage('pages/classify/classify.vue', {TabBar:{},AuthProfilePopup:{},sharePageMixin:{},parseInvitePageOptions(){},
    uni:{getStorageSync:()=>categories,setStorageSync(){}},
    getGoodsCatrgorys:async()=>({data:categories})})
}
test('all login preflight UI uses own brand and neutral login copy', () => {
  for (const file of ['components/AuthProfilePopup/AuthProfilePopup.vue','packagesPublic/login/login.vue']) {
    const source=read(file)
    const template=source.match(/<template>([\s\S]*?)<\/template>/)[1]
    assert.match(template,/手机号快捷登录/)
    assert.doesNotMatch(template,/微信|wx_ico|weixin|wechat/i)
  }
  assert.match(read('components/AuthProfilePopup/AuthProfilePopup.vue'),/open-type="getPhoneNumber"/)
  assert.match(read('utils/login.js'),/provider: 'weixin'/)
})
test('service hides retail and empty mall for fresh cache and API data', async () => {
  const {page,source}=service()
  page.navbarSelect=83
  page.syncNavbarListFromCache()
  assert.deepEqual(Array.from(page.navbarList,x=>x.categoryId),[25,41,42])
  assert.equal(page.navbarSelect,25)
  page.navbarSelect=50
  await page.refreshNavbarListFromApi()
  assert.equal(page.navbarSelect,25)
  assert.deepEqual(Array.from(page.navbarList,x=>x.categoryId),[25,41,42])
  assert.doesNotMatch(source,/@click="switchToYunnanGoods"/)
  const before=page.navbarSelect
  page.applyNavbarItem(categories[1])
  page.applyNavbarItem(categories[5])
  assert.equal(page.navbarSelect,before)
})
test('cold launch with a retired category selects a live service', () => {
  const {page,component}=service()
  component.onLoad.call(page,{cls:83})
  assert.equal(page.navbarSelect,25)
  assert.deepEqual(Array.from(page.navbarList,x=>x.categoryId),[25,41,42])
})
test('empty legacy mall is not registered or linked', () => {
  assert.doesNotMatch(read('pages.json'),/"path"\s*:\s*"shop\/list"/)
  for (const file of ['pages/home/home.vue','pages/my/my.vue','components/TabBar/TabBar.vue']) {
    assert.doesNotMatch(read(file),/packagesMall\/shop\/list/)
  }
})

test('direct service launch fetches live categories instead of an empty cached shell', async () => {
  const {page}=service()
  let loaded=0
  page.getGoodsCatrgoryFn=()=>{loaded++}
  page.$nextTick=()=>{}
  await page.loadContentList()
  assert.equal(page.navbarSelect,25)
  assert.equal(loaded,1)
})
test('service bootstrap errors expose a retry, not a silent blank page', async () => {
  const {page}=service()
  page.refreshNavbarListFromApi=async()=>{throw Error('offline')}
  await page.loadContentList()
  assert.match(page.contentError,/重试/)
  assert.equal(page.loading,false)
})
