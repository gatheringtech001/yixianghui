const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')

function cart(api = {}) {
  const source = fs.readFileSync(path.resolve(__dirname,'../../shop-mnp/packagesMall/cart/cart.vue'),'utf8')
  const script = source.match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/import\s+[\s\S]*?\s+from\s+['"][^'"]+['"];?/g,'').replace('export default','module.exports =')
  const navigations = []
  const context = {module:{exports:{}},getCartList:async()=>({rows:[item(1,29.9,2),item(2,60,1),item(3,10,1,0)]}),
    updateCart:async()=>{},uni:{showToast(){},navigateTo:({url})=>navigations.push(url)},...api}
  vm.runInNewContext(script,context)
  const component=context.module.exports
  const page=component.data.call({$host:''})
  for(const [key,method] of Object.entries(component.methods))page[key]=method.bind(page)
  for(const [key,getter] of Object.entries(component.computed))Object.defineProperty(page,key,{get:getter.bind(page)})
  return {page,navigations}
}
function item(id,price,count,stock=10) {return {cartId:id,goodsId:id,goodsCount:count,goodsInfo:{price,stock,status:'1',goodsType:'online'}}}
test('available items start selected and totals include all selected quantities',async()=>{
  const {page,navigations}=cart();await page.loadCart()
  assert.deepEqual(Array.from(page.selectedIds),[1,2]);assert.equal(page.selectedTotal,'119.80')
  page.checkout();assert.match(navigations[0],/cartIds=1,2$/)
  page.selectItem(page.items[0]);assert.equal(page.selectedTotal,'60.00')
  page.selectAll();assert.equal(page.selectedItems.length,2)
  page.selectAll();assert.equal(page.selectedItems.length,0)
})
test('selection survives reload while invalid stock is excluded',async()=>{
  const {page}=cart();await page.loadCart();page.selectItem(page.items[0]);await page.loadCart()
  assert.deepEqual(Array.from(page.selectedIds),[2]);page.selectItem(page.items[2]);assert.equal(page.selectedItems.length,1)
})
test('quantity updates are serialized and checkout waits for the update',async()=>{
  let finish;let calls=0
  const {page,navigations}=cart({updateCart:()=>{calls++;return new Promise(resolve=>finish=resolve)}})
  await page.loadCart();const pending=page.changeCount(page.items[0],1)
  await page.changeCount(page.items[0],1);page.checkout();assert.equal(calls,1);assert.equal(navigations.length,0)
  finish();await pending;assert.equal(page.items[0].goodsCount,3);assert.equal(page.selectedTotal,'149.70')
})
