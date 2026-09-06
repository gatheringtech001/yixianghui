const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const loadPage = require('./vue-page-harness.cjs')

function servicePage() {
  const calls=[]
  const result=loadPage('pages/classify/classify.vue', {
    TabBar:{},AuthProfilePopup:{},sharePageMixin:{},
    uni:{navigateTo:o=>calls.push(o.url),getStorageSync:()=>[]},
    runWithAuth:(_page,done)=>done(true),
    getGoodsList:async()=>{calls.push('goods request');return {data:[]}},
    getGoodsCatrgorys:async()=>{calls.push('categories request');return {data:[]}}
  })
  result.page.navbarList=[{categoryId:83,categoryName:'云南好物'},{categoryId:25,categoryName:'全国旅居'}]
  result.page.navbarSelect=83
  return {...result,calls}
}

test('Yunnan service content is replaced by an opaque preparing mask', () => {
  const {page,source}=servicePage()
  assert.equal(page.isYunnanPreparing,true)
  assert.match(source,/v-if="isYunnanPreparing" class="yunnan-preparing-mask"/)
  assert.match(source,/板块正在准备中，敬请期待/)
  assert.match(source,/class="yunnan-preparing-mask"[\s\S]*?<block v-else>/)
  const styles=fs.readFileSync(path.resolve(__dirname,'../../shop-mnp/pages/classify/classify.scss'),'utf8')
  assert.match(styles,/\.yunnan-preparing-mask\s*\{[\s\S]*?background:\s*#f7f7f5/)
  page.navbarSelect=25
  assert.equal(page.isYunnanPreparing,false)
})

test('preparing service does not fetch goods or expose search and cart actions', async () => {
  const {page,calls}=servicePage()
  page.goodsList=[{goodsId:260}]
  await page.getGoodsListFn()
  page.searchFn()
  page.openCart()
  assert.equal(page.goodsList.length,0)
  assert.deepEqual(calls,[])
})

test('home still routes its Yunnan entry to the corresponding service tab', () => {
  const actions=[]
  const {page,source}=loadPage('pages/home/home.vue',{TabBar:{},AuthProfilePopup:{},sharePageMixin:{},
    uni:{getStorageSync:()=>[{categoryId:83,parentId:0,categoryName:'云南好物'}],
      setStorageSync:(key,value)=>actions.push([key,value]),switchTab:o=>actions.push(o.url)}})
  page.goClassify('云南好物')
  assert.match(source,/@click="goClassify\('云南好物'\)"/)
  assert.ok(actions.some(value=>Array.isArray(value)&&value[0]==='currentCls'&&value[1]===83))
  assert.ok(actions.includes('/pages/classify/classify'))
})
