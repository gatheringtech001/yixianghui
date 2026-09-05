const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const read = file => fs.readFileSync(path.resolve(__dirname,'../../shop-mnp',file),'utf8')

test('cart is discoverable from service and profile without changing the main navigation', () => {
  assert.match(read('pages/classify/classify.vue'), /class="cart-entry"/)
  assert.match(read('pages/classify/classify.vue'), /packagesMall\/cart\/cart/)
  assert.match(read('pages/my/my.vue'), /onServer\('cart'\)/)
  assert.match(read('pages/my/my.vue'), /case 'cart'/)
  assert.match(read('pages/my/my.vue'), /cartCount/)
})

test('detail share and favorite icons follow the same existing icon set', () => {
  const pages=['GoodsDetails/GoodsDetails','GoodsDetails/SojournGoodsDetails','GoodsDetails/EducationGoodsDetails','Activity/detail/index']
  for(const page of pages){
    const source=read('packagesMall/'+page+'.vue')
    assert.match(source,/name="share-fill"/,page)
    assert.match(source,/'heart-fill'/,page)
  }
})

test('fixed commerce footers reserve the real device safe area', () => {
  for(const file of ['packagesMall/cart/cart.scss','packagesMall/ConfirmOrder/RetailConfirmOrder.vue',
    'packagesMall/GoodsDetails/GoodsDetails.scss','packagesMall/GoodsDetails/SojournGoodsDetails.scss',
    'packagesMall/GoodsDetails/EducationGoodsDetails.scss','packagesMall/Activity/detail/index.scss']){
    const source=read(file)
    assert.match(source,/calc\([^;]+env\(safe-area-inset-bottom\)/,file)
  }
})

test('brand color and key touch targets are consistent and readable', () => {
  assert.match(read('uni.scss'), /\$base:\s*#701018/)
  assert.match(read('uni.scss'), /\$u-type-primary:\s*#701018/)
  assert.match(read('components/TabBar/TabBar.scss'), /font-size:\s*24rpx/)
  assert.match(read('packagesMall/cart/cart.scss'), /min-width:\s*44px/)
  assert.match(read('packagesMall/cart/cart.scss'), /min-height:\s*44px/)
})
