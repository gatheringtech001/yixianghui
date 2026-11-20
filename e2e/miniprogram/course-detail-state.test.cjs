const assert = require('node:assert/strict')
const test = require('node:test')
const loadPage = require('./vue-page-harness.cjs')
const file = 'packagesMall/GoodsDetails/EducationGoodsDetails.vue'
function setup(fetch) {
  const calls = []
  const fixture = loadPage(file, {
    AuthProfilePopup: {}, sharePageMixin: {}, parseInvitePageOptions() {},
    getGoodsInfo: async id => { calls.push(id); return fetch(id) },
    parseCourseMeta: () => ({}), prepareRichTextHtml: s => s,
    getCourseSignupPhase: () => 'open',
    uni: {getStorageSync: () => null}
  })
  fixture.page.$nextTick = fn => fn()
  fixture.page.setPageScrollHeight = () => {}
  return {...fixture, calls}
}
const valid = {data:{goodsId:279,goodsName:'测试课程',price:250}}

test('missing or malformed course ids show an actionable state without fetching', async () => {
  for (const options of [undefined, {}, {id:'undefined'}, {id:'0'}, {id:'-1'}, {id:'abc'}]) {
    const {page, component, calls} = setup(() => valid)
    await component.onLoad.call(page, options)
    assert.match(page.loadError, /从课程列表重新进入/)
    assert.equal(page.loading,false)
    assert.equal(page.courseData,null)
    assert.equal(calls.length,0)
  }
})
test('live course id and old goodsId links both load actual content', async () => {
  for (const options of [{id:279}, {goodsId:'279'}]) {
    const {page,component,calls} = setup(() => valid)
    await component.onLoad.call(page,options)
    assert.equal(page.courseData.name,'测试课程')
    assert.equal(page.loadError,'')
    assert.equal(page.loading,false)
    assert.deepEqual(calls,['279'])
  }
})
test('pending request shows loading and prevents duplicate retries', async () => {
  let finish
  const {page,calls} = setup(() => new Promise(resolve => {finish=resolve}))
  const request = page.getCourseDetail('279')
  assert.equal(page.loading,true)
  page.getCourseDetail('279')
  assert.equal(calls.length,1)
  finish(valid)
  await request
  assert.equal(page.loading,false)
})
test('empty or invalid API responses do not render a nameless course or application buttons', async () => {
  for (const response of [null, {}, {data:null}, {data:{}}, {data:{goodsId:279}}]) {
    const {page} = setup(() => response)
    await page.getCourseDetail(279)
    assert.match(page.loadError,/暂不可查看/)
    assert.equal(page.courseData,null)
    assert.equal(page.loading,false)
  }
})
test('network failure remains visible and a retry recovers without stale content', async () => {
  let fail = true
  const {page,source} = setup(() => {if(fail) throw Error('offline');return valid})
  page.courseData={id:100,name:'旧课程'}
  await page.getCourseDetail(279)
  assert.equal(page.courseData,null)
  assert.match(page.loadError,/加载失败.*重试/)
  assert.equal(page.loading,false)
  fail=false
  await page.getCourseDetail(page.courseId)
  assert.equal(page.courseData.id,279)
  assert.equal(page.loadError,'')
  assert.match(source, /v-if="loading \|\| loadError"/)
  assert.match(source, /@tap="getCourseDetail\(courseId\)"/)
  assert.match(source, /@tap="backToCourses"/)
})
test('malformed legacy feature metadata cannot throw the detail into blank content', async () => {
  const {page} = setup(() => ({data:{...valid.data,features:{legacy:true},content:'课程内容'}}))
  await page.getCourseDetail(279)
  assert.equal(page.courseData.sections.length,1)
  assert.equal(page.loadError,'')
})
test('return to course list resolves the live category rather than guessing its id', async () => {
  const {page} = setup(() => valid)
  const actions=[]
  // Methods retain the uni object passed through the harness; override via a dedicated fixture.
  const second=loadPage(file, {AuthProfilePopup:{},sharePageMixin:{},uni:{
    getStorageSync:()=>[{categoryId:99,parentId:0,categoryName:'老年教育'}],
    setStorageSync:(...args)=>actions.push(args),switchTab:o=>actions.push(o.url)
  }})
  await second.page.backToCourses()
  assert.deepEqual(actions,[['currentCls',99],'/pages/classify/classify'])
  assert.equal(page.getShareConfig().path,'/pages/classify/classify')
})

test('direct cold detail links can return to courses without a cached category list', async () => {
  const actions=[]
  const categories=[{categoryId:99,parentId:0,categoryName:'老年教育'}]
  const {page}=loadPage(file, {AuthProfilePopup:{},sharePageMixin:{},
    getGoodsCatrgorys:async()=>({data:categories}),uni:{
      getStorageSync:()=>null,setStorageSync:(...args)=>actions.push(args),
      switchTab:o=>actions.push(o.url)
    }})
  await page.backToCourses()
  assert.deepEqual(actions,[['cls',categories],['currentCls',99],'/pages/classify/classify'])
})
test('return still reaches service retry UI when live categories fail', async () => {
  const actions=[]
  const {page}=loadPage(file, {AuthProfilePopup:{},sharePageMixin:{},
    getGoodsCatrgorys:async()=>{throw Error('offline')},uni:{
      getStorageSync:()=>null,switchTab:o=>actions.push(o.url)
    }})
  await page.backToCourses()
  assert.deepEqual(actions,['/pages/classify/classify'])
})
