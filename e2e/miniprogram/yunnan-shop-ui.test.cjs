const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')

const root = path.resolve(__dirname, '../..')
const read = relativePath => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('home keeps its structure while adding the requested recommendation and service entries', () => {
	const home = read('shop-mnp/pages/home/home.vue')
	const travelIndex = home.indexOf('travel-entry-card')
	const yunnanIndex = home.indexOf("goClassify('云南好物')")
	const activityIndex = home.indexOf("goClassify('聚会活动')")
	const schoolIndex = home.indexOf("goClassify('老年教育')")

	assert.match(home, /class="recommendation-swiper"/)
	assert.match(home, /loadRecommendations/)
	assert.ok(travelIndex < yunnanIndex)
	assert.ok(yunnanIndex < activityIndex)
	assert.ok(activityIndex < schoolIndex)
	assert.match(home, /entry-stay-wide\.jpg/)
	assert.match(home, /entry-yunnan\.jpg/)
})

test('shop pages do not expose membership pricing and use regular price for checkout', () => {
	const detail = read('shop-mnp/packagesMall/GoodsDetails/GoodsDetails.vue')
	const cart = read('shop-mnp/packagesMall/cart/cart.vue')
	const order = read('shop-mnp/packagesMall/ConfirmOrder/ConfirmOrder.vue')

	for (const source of [detail, cart, order]) {
		assert.doesNotMatch(source, /会员价|年卡会员|会员折扣/)
	}
	assert.doesNotMatch(detail, /class="vip min" v-else/)
	assert.match(detail, /v-if="goodsDetail\.goodsType == 'hotel'">起<\/text>/)
	assert.match(cart, /Number\(goods\.price \|\| 0\)/)
	assert.match(order, /Number\(this\.goodsDetail\.price \|\| 0\) \* this\.count/)
	assert.match(order, /remark: this\.remark\.trim\(\)/)
})

test('product details filter only redundant source labels and force rich images to fit', () => {
	const detail = read('shop-mnp/packagesMall/GoodsDetails/GoodsDetails.vue')
	const richText = read('shop-mnp/utils/richText.js')

	assert.match(detail, /!\['云野集', '云南好物'\]\.includes\(tag\)/)
	assert.match(detail, /tag-style="richTextTagStyle"/)
	assert.match(richText, /width:100%;max-width:100%;display:block/)
})
