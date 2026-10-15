const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const read = file => fs.readFileSync(path.resolve(__dirname,'../../shop-mnp',file),'utf8')

test('cart stays in service but is removed from profile', () => {
  assert.match(read('pages/classify/classify.vue'), /class="cart-entry"/)
  assert.match(read('pages/classify/classify.vue'), /packagesMall\/cart\/cart/)
  assert.doesNotMatch(read('pages/my/my.vue'), /onServer\('cart'\)|case 'cart'|cartCount|getCartList|loadCartCount/)
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

test('activity introduction stacks its heading above full-width rich text without changing metadata rows', () => {
  const style = read('packagesMall/Activity/detail/index.scss')
  const description = style.match(/\.activity-description\s*\{([^}]+)\}/)[1]
  const content = style.match(/\.description-content\s*\{([^}]+)\}/)[1]
  assert.match(style, /\.activity-progress-head,\s*\.activity-field\s*\{[^}]*grid-template-columns:\s*200rpx 1fr/)
  assert.match(description, /grid-template-columns:\s*minmax\(0,\s*1fr\)/)
  assert.match(content, /grid-column:\s*1\s*\/\s*-1/)
  assert.match(content, /justify-self:\s*stretch/)
  assert.match(content, /min-width:\s*0/)
  assert.match(content, /(?:^|\s)width:\s*100%/)
  assert.match(content, /max-width:\s*100%/)
  assert.doesNotMatch(content, /grid-column:\s*2\s*;|justify-self:\s*end/)
  assert.match(read('packagesMall/Activity/detail/index.vue'), /class="activity-field activity-description"[\s\S]*活动详情介绍[\s\S]*class="description-content"[\s\S]*<u-parse :html="detailInfo.content"/)
})
