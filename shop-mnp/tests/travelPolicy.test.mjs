import assert from 'node:assert/strict'
import test from 'node:test'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'

import * as policy from '../utils/travelPolicy.js'
import * as presentation from '../utils/travelPresentation.js'
import * as rates from '../utils/travelRates.js'

const travelPolicy = {
	version: 1, capacity: 2, includedMeals: '三餐',
	stayStart: '2026-01-01', stayEnd: '2026-12-31',
	excludedStayRanges: [{ start: '2026-07-10', end: '2026-08-20' }]
}
const metadata = value => JSON.stringify({ travelPolicy: value })
const longPackageDescription = '三餐；整间总价，最多2人；2026-10-30至2026-12-31住宿夜'
const eligibilityNotice = '入住同行需有一人年满50岁；75岁以上需要有家人陪同方可入住'
const stay = overrides => ({ travelPolicy, nights: 6, checkInDate: '2026-10-29', checkOutDate: '2026-11-04', ...overrides })
const catalog = (value = travelPolicy) => ({
	goodsId: 77, goodsName: '测试基地', goodsImages: '',
	optionList: [
		{ skuId: 200, skuType: '200', price: 0, status: '1', options: [] },
		{ skuId: 202, parSkuId: 200, skuType: '202', skuName: '周套餐', status: '1', options: [
			{ optionType: '303', skuSeqNo: 0, optionValue: '7', optionValueUnit: '晚', optionParam: metadata(value) },
			{ optionType: '302', skuSeqNo: 1, optionValue: '1200', optionValueUnit: '元/间' },
			{ optionType: '304', skuSeqNo: 1, optionValue: longPackageDescription }
		] },
		{ skuId: 201, skuType: '201', skuName: '额外收费供餐', price: 50, status: '1' }
	]
})

test('explicit 303 nights win over legacy package names', () => {
	for (const name of ['周套餐', '半月套餐', '月套餐']) assert.equal(policy.parseStayNights('7', name, '晚'), 7)
	assert.equal(policy.parseStayNights('7', '普通套餐', '天'), 6)
	assert.equal(policy.parseStayNights(undefined, '半月套餐'), 14)
	for (const value of ['7x', '1.5', 0, -1, Infinity]) assert.equal(policy.parseStayNights(value, '周套餐', '晚'), 0)
})

test('metadata is optional, but a present policy must validate strictly', () => {
	for (const value of [undefined, null, '', '{}', '{"other":true}']) assert.equal(policy.parseTravelPolicy(value), null)
	assert.deepEqual(policy.parseTravelPolicy(metadata({ version: 1 })), { version: 1 })
	for (const value of ['三餐', '早餐', '一早一正', '不含餐', '早晚餐']) {
		assert.equal(policy.parseTravelPolicy(metadata({ version: 1, includedMeals: value })).includedMeals, value)
	}
	for (const value of ['broken', 'null', '[]', metadata(null), metadata({}), metadata({ version: '1' }), metadata({ version: 2 })]) {
		assert.throws(() => policy.parseTravelPolicy(value))
	}
	for (const capacity of [null, '2', 0, -1, 1.5, true]) assert.throws(() => policy.parseTravelPolicy(metadata({ version: 1, capacity })))
	for (const includedMeals of [null, '', '  ', 3, []]) assert.throws(() => policy.parseTravelPolicy(metadata({ version: 1, includedMeals })))
})

test('invalid policy dates and excluded ranges fail closed', () => {
	for (const extra of [
		{ stayStart: '2026-02-29' }, { stayEnd: '2026-13-01' }, { stayStart: '2026-1-01' },
		{ stayStart: null }, { stayStart: '2026-12-31', stayEnd: '2026-01-01' },
		{ excludedStayRanges: null }, { excludedStayRanges: {} }, { excludedStayRanges: [null] },
		{ excludedStayRanges: [{ start: '2026-07-10' }] },
		{ excludedStayRanges: [{ start: '2026-07-10', end: '2026-07-09' }] }
	]) assert.throws(() => policy.parseTravelPolicy(metadata({ version: 1, ...extra })))
	assert.equal(policy.parseTravelPolicy(metadata({ version: 1, stayStart: '2028-02-29' })).stayStart, '2028-02-29')
})

test('only the public seq0 303 policy is authoritative; duplicates and bad JSON block selection', () => {
	const sku = catalog().optionList[1]
	assert.equal(policy.readTravelPackage(sku).day, 7)
	assert.deepEqual(policy.readTravelPackage(sku).travelPolicy, travelPolicy)
	const bad = { ...sku, options: [{ ...sku.options[0], optionParam: '{bad' }] }
	assert.match(policy.readTravelPackage(bad).policyError, /政策配置错误/)
	assert.match(policy.readTravelPackage({ ...sku, options: [sku.options[0], sku.options[0]] }).policyError, /重复/)
	const privateRow = { ...sku.options[0], skuSeqNo: 1 }
	assert.equal(policy.readTravelPackage({ ...sku, options: [privateRow] }).travelPolicy, null)
})

test('zero or invalid nightly price does not disable fixed packages, but forbids custom nights', () => {
	for (const price of [0, '0', -1, 'bad', Infinity]) assert.equal(policy.supportsCustomNights({ skuId: 200, price }), false)
	assert.equal(policy.supportsCustomNights({ skuId: 200, price: '88' }), true)
	assert.equal(policy.readTravelPackage(catalog().optionList[1]).day, 7)
})

test('every occupied night respects the inclusive year bounds, excluding checkout day', () => {
	assert.equal(policy.validateTravelSelection(stay()), '')
	assert.equal(policy.validateTravelSelection(stay({ checkInDate: '2026-12-26', checkOutDate: '2027-01-01' })), '')
	assert.match(policy.validateTravelSelection(stay({ checkInDate: '2026-12-27', checkOutDate: '2027-01-02' })), /范围/)
	assert.match(policy.validateTravelSelection(stay({ checkInDate: '2025-12-31', checkOutDate: '2026-01-06' })), /范围/)
	assert.match(policy.validateTravelSelection(stay({ checkInDate: '2027-01-01', checkOutDate: '2027-01-07' })), /范围/)
})

test('optional policy fields can constrain one boundary independently', () => {
	assert.equal(policy.validateTravelSelection(stay({ travelPolicy: { version: 1 } })), '')
	assert.match(policy.validateTravelSelection(stay({ travelPolicy: { version: 1, stayStart: '2026-10-30' } })), /范围/)
	assert.match(policy.validateTravelSelection(stay({ travelPolicy: { version: 1, stayEnd: '2026-11-02' } })), /范围/)
	assert.equal(policy.validateTravelSelection(stay({ travelPolicy: { version: 1, excludedStayRanges: [] } })), '')
})

test('excluded dates block starts, ends, and interior overlap, inclusive on both ends', () => {
	assert.equal(policy.validateTravelSelection(stay({ checkInDate: '2026-07-04', checkOutDate: '2026-07-10' })), '')
	for (const [checkInDate, checkOutDate, nights] of [
		['2026-07-05', '2026-07-11', 6], ['2026-07-10', '2026-07-16', 6],
		['2026-08-20', '2026-08-26', 6], ['2026-07-01', '2026-09-01', 62]
	]) assert.match(policy.validateTravelSelection(stay({ checkInDate, checkOutDate, nights })), /不可入住/)
	assert.equal(policy.validateTravelSelection(stay({ checkInDate: '2026-08-21', checkOutDate: '2026-08-27' })), '')
})

test('invalid dates, duration, and policy errors cannot proceed', () => {
	for (const checkInDate of ['', '2026-02-30', '2026-10-29x']) assert.match(policy.validateTravelSelection(stay({ checkInDate })), /有效/)
	for (const nights of [0, -1, 1.5, NaN]) assert.match(policy.validateTravelSelection(stay({ nights })), /晚数无效/)
	assert.match(policy.validateTravelSelection(stay({ nights: 7 })), /不匹配/)
	assert.equal(policy.validateTravelSelection(stay({ policyError: '配置损坏' })), '配置损坏')
	assert.equal(policy.validateTravelSelection(stay({ travelPolicy: null })), '')
})

test('capacity sets only the maximum; fewer guests are valid', () => {
	assert.equal(policy.travelCapacity(null), 2)
	assert.equal(policy.validateTravelSelection(stay({ roomNumber: 1, peopleNumber: 1 })), '')
	assert.equal(policy.validateTravelSelection(stay({ roomNumber: 2, peopleNumber: 4 })), '')
	assert.match(policy.validateTravelSelection(stay({ roomNumber: 1, peopleNumber: 3 })), /每间最多入住2人/)
	assert.match(policy.validateTravelSelection(stay({ roomNumber: 0, peopleNumber: 1 })), /正整数/)
	assert.match(policy.validateTravelSelection(stay({ roomNumber: 1, peopleNumber: 1.5 })), /正整数/)
})

// Exercise the actual page options as pure JS: all APIs are local stubs, no network/order is created.
function pageHarness(file, data) {
	const events = { toasts: [], navigations: [], requests: [] }
	const source = readFileSync(new URL(`../packagesMall/${file}.vue`, import.meta.url), 'utf8')
	const script = source.match(/<script>([\s\S]*?)<\/script>/)[1]
		.replace(/^\s*import\s+[\s\S]*?\s+from\s+['"][^'"]+['"]\s*$/gm, '')
		.replace('export default', 'module.exports =')
	const context = {
		...policy, ...presentation, ...rates, module: { exports: {} }, Calendar: {}, AuthProfilePopup: {},
		sharePageMixin: {}, console: { log() {} }, BaseUrl: {},
		getGoodsInfo: async () => ({ data }), getBannerList: async () => ({ data: [] }),
		runWithAuth: (_, callback) => callback(true),
		createOrder: async params => { events.requests.push(params); return { code: 500 } },
		uni: { showToast: value => events.toasts.push(value), navigateTo: value => events.navigations.push(value) }
	}
	vm.runInNewContext(script, context)
	const options = context.module.exports
	const page = { ...options.data.call({}), $nextTick() {}, $refs: {}, $u: { throttle: callback => callback() } }
	for (const [key, fn] of Object.entries(options.methods)) page[key] = fn.bind(page)
	for (const [key, fn] of Object.entries(options.computed)) Object.defineProperty(page, key, { get: () => fn.call(page) })
	return { page, options, events }
}

async function confirmation(data = catalog(), overrides = {}) {
	const harness = pageHarness('ConfirmOrder/SojournConfirmOrder', data)
	harness.options.onLoad.call(harness.page, {
		id: '77', skuId: '202', skuSeqNo: '1', day: '7', checkInDate: '2026-10-29', checkOutDate: '2026-11-05', ...overrides
	})
	await new Promise(resolve => setImmediate(resolve))
	return harness
}

test('confirmation uses metadata over configured 201, explicit nights, room totals and initial capacity', async () => {
	const { page, events } = await confirmation()
	assert.equal(page.selectionError, '')
	assert.equal(page.reserveData.peopleNumber, 2)
	assert.equal(page.roomCapacity, 2)
	assert.equal(page.skuPrice, 1200)
	assert.equal(page.mealFee, 0)
	assert.equal(page.currentCombo.comboId, null)
	assert.equal(page.currentCombo.name, '三餐')
	assert.equal(page.availableCombos.length, 1)
	assert.match(page.mealDescription, /三餐/)
	assert.doesNotMatch(page.mealDescription, /份早餐|份中餐|份晚餐/)
	page.reserveData.peopleNumber = 1
	assert.equal(page.selectionError, '')
	assert.equal(page.payableAmount, 1200)
	page.reserveData.roomNumber = 2
	assert.equal(page.maxPeopleNumber, 4)
	assert.equal(page.payableAmount, 2400)
	assert.equal(events.requests.length, 0)
})

test('all supplied includedMeals labels display literally without inferred meal counts or extra charges', async () => {
	for (const includedMeals of ['三餐', '早餐', '一早一正', '不含餐', '早晚餐']) {
		const { page } = await confirmation(catalog({ ...travelPolicy, capacity: 1, includedMeals }))
		assert.equal(page.reserveData.peopleNumber, 1)
		assert.equal(page.maxPeopleNumber, 1)
		assert.match(page.mealDescription, new RegExp(includedMeals))
		assert.doesNotMatch(page.mealDescription, /份早餐|份中餐|份晚餐/)
		page.comboIndex = 2
		assert.equal(page.mealFee, 0)
		assert.equal(page.currentCombo.comboId, null)
	}
})

test('metadata still replaces breakfast fallback when no 201 exists', async () => {
	const data = catalog({ ...travelPolicy, includedMeals: '一早一正' })
	data.optionList.pop()
	const { page } = await confirmation(data)
	assert.equal(page.comboList[0].name, '一早一正')
	assert.equal(page.currentCombo.price, 0)
	assert.doesNotMatch(page.mealDescription, /早餐/)
})

test('confirmation meal-price heading says included for metadata and retains legacy pricing', async () => {
	const source = readFileSync(new URL('../packagesMall/ConfirmOrder/SojournConfirmOrder.vue', import.meta.url), 'utf8')
	assert.match(source, /<view class="title_price_view" v-if="includedMeals">\{\{ metadataMealPresentation.title \}\}<\/view>\s*<view class="title_price_view" v-else>/)
	assert.match(source, /<text>￥<text>\{\{currentCombo.price\}\}<\/text><\/text>\/人\/天/)
	for (const includedMeals of ['三餐', '早餐', '一早一正', '不含餐', '早晚餐']) {
		const { page } = await confirmation(catalog({ ...travelPolicy, includedMeals }))
		assert.equal(page.includedMeals, includedMeals)
		assert.equal(page.mealFee, 0)
	}
	const legacyData = catalog()
	delete legacyData.optionList[1].options[0].optionParam
	const legacy = await confirmation(legacyData)
	assert.equal(legacy.page.includedMeals, '')
	assert.equal(legacy.page.currentCombo.price, 50)
	assert.equal(legacy.page.mealFee, 700)
	legacyData.optionList.pop()
	const no201 = await confirmation(legacyData)
	assert.equal(no201.page.includedMeals, '')
	assert.equal(no201.page.currentCombo.price, 0)
})

test('90 no-meal and 271 unspecified-meal metadata never promise included meals', async () => {
	for (const [goodsId, includedMeals, title] of [
		[90, '不含餐', '不含餐'],
		[271, '餐次未注明（餐食标准见基地说明）', '餐食标准见资料']
	]) {
		const data = catalog({ ...travelPolicy, includedMeals })
		data.goodsId = goodsId
		const { page, events } = await confirmation(data, { id: String(goodsId) })
		assert.equal(page.metadataMealPresentation.title, title)
		assert.equal(page.mealDescription, `餐食说明：${includedMeals}；本订单未另外计收餐费。`)
		assert.ok(page.selectedTravelPolicyText.includes(`餐食说明：${includedMeals}；本订单未另外计收餐费`))
		for (const text of [page.mealDescription, page.selectedTravelPolicyText]) {
			assert.doesNotMatch(text, /套餐供餐|供餐：|已含套餐内|套餐不另收餐费|含餐承诺/)
		}
		assert.equal(page.currentCombo.name, includedMeals)
		assert.equal(page.currentCombo.comboId, null)
		assert.equal(page.mealFee, 0)
		assert.equal(page.payableAmount, 1200)
		page.reserveData.name = '合成测试'
		page.reserveData.phone = '13800000000'
		page.isReadingAgreement = true
		page.createOrderFn()
		assert.equal(events.requests.length, 1)
		assert.equal(events.requests[0].selfSkuId, null)
	}
})

test('positive source meal labels retain included title and original policy wording', async () => {
	for (const includedMeals of ['三餐', '早餐', '一早一正', '早晚餐', '午餐']) {
		const { page } = await confirmation(catalog({ ...travelPolicy, includedMeals }))
		assert.equal(page.metadataMealPresentation.title, '已含套餐内')
		assert.equal(page.mealDescription, `套餐供餐：${includedMeals}（不另收餐费）。`)
		assert.ok(page.selectedTravelPolicyText.includes(`供餐：${includedMeals}，套餐不另收餐费`))
		assert.equal(page.mealFee, 0)
	}
})

test('confirmation preserves illegal routed dates and blocks date confirmation and order submission', async () => {
	for (const overrides of [
		{ checkInDate: '2026-07-06', checkOutDate: '2026-07-13' },
		{ checkInDate: '2026-12-26', checkOutDate: '2027-01-02' },
		{ checkOutDate: '2026-11-06' }, { skuSeqNo: '99' }, { skuId: '999' },
		{ customNight: '1', skuId: '200' }
	]) {
		const { page, events } = await confirmation(catalog(), overrides)
		assert.notEqual(page.selectionError, '')
		page.popupDate.show = true
		page.confirmFn()
		assert.equal(page.popupDate.show, true)
		page.reserveData.name = '合成测试'
		page.reserveData.phone = '13800000000'
		page.isReadingAgreement = true
		page.createOrderFn()
		assert.equal(events.requests.length, 0)
		assert.ok(events.toasts.length)
	}
})

test('bad metadata and over-capacity guests cannot reach createOrder API', async () => {
	for (const data of [catalog({ ...travelPolicy, capacity: '2' }), catalog()]) {
		const { page, events } = await confirmation(data)
		page.reserveData.peopleNumber = 3
		page.createOrderFn()
		assert.equal(events.requests.length, 0)
		assert.notEqual(page.selectionError, '')
	}
})

test('valid submission payload uses a room total and no 201 charge (stub only, no real order)', async () => {
	const { page, events } = await confirmation()
	page.reserveData.name = '合成测试'
	page.reserveData.phone = '13800000000'
	page.isReadingAgreement = true
	page.reserveData.roomNumber = 2
	page.reserveData.peopleNumber = 1
	page.createOrderFn()
	assert.equal(events.requests.length, 1)
	const request = events.requests[0]
	assert.equal(request.skuId, 202)
	assert.equal(request.skuSeqNo, 1)
	assert.equal(request.interCount, 7)
	assert.equal(request.goodsCount, 2)
	assert.equal(request.selfGoodsCount, 1)
	assert.equal(request.selfSkuId, null)
	assert.equal(page.payableAmount, 2400)
})

test('disabled parent rooms and sub-minimum custom nights cannot be submitted', async () => {
	const disabled = catalog()
	disabled.optionList[0].status = '0'
	const { page, events } = await confirmation(disabled)
	assert.notEqual(page.selectionError, '')
	page.createOrderFn()
	assert.equal(events.requests.length, 0)
	const enabled = catalog()
	enabled.optionList[0].price = 100
	const custom = await confirmation(enabled, { customNight: '1', skuId: '200', day: '6', checkOutDate: '2026-11-04' })
	assert.notEqual(custom.page.selectionError, '')
	custom.page.createOrderFn()
	assert.equal(custom.events.requests.length, 0)
})

test('detail selects fixed 202 at zero room price, labels capacity and blocks unsupported custom nights', async () => {
	const { page, events } = pageHarness('GoodsDetails/SojournGoodsDetails', catalog())
	page.loadCollectState = () => {}
	page.getGoodsDetailFn(77)
	await new Promise(resolve => setImmediate(resolve))
	const sku = page.skuGroupList[0].skuDataList[0]
	const combo = sku.combinationList[0]
	assert.equal(combo.name, '三餐')
	assert.equal(sku.day, 7)
	assert.match(combo.priceLabel, /含2人\/间/)
	assert.equal(combo.average, 171.43)
	page.reserveFn(sku, combo, 0)
	page.popupDate.checkInDate = '2026-10-29'
	page.popupDate.checkOutDate = '2026-11-05'
	assert.equal(page.canCustomizeNights, false)
	page.selectDayFn('customize')
	assert.equal(page.popupDate.customNight, false)
	assert.equal(page.selectionError, '')
	page.confirmFn()
	assert.equal(events.navigations.length, 1)
	assert.match(events.navigations[0].url, /skuId=202&skuSeqNo=1/)
	page.popupDate.checkInDate = '2026-07-06'
	page.popupDate.checkOutDate = '2026-07-13'
	page.confirmFn()
	assert.equal(events.navigations.length, 1)
	assert.match(events.toasts.at(-1).title, /不可入住/)
	page.popupDate.customNight = true
	page.confirmFn()
	assert.equal(events.navigations.length, 1)
	for (const optionParam of [undefined, metadata({ version: 1 })]) {
		const legacyData = catalog()
		legacyData.optionList[1].options[0].optionParam = optionParam
		const legacy = pageHarness('GoodsDetails/SojournGoodsDetails', legacyData)
		legacy.page.loadCollectState = () => {}
		legacy.page.getGoodsDetailFn(77)
		await new Promise(resolve => setImmediate(resolve))
		assert.equal(legacy.page.skuGroupList[0].skuDataList[0].combinationList[0].name, longPackageDescription)
	}
})

test('detail blocks malformed metadata and year overflow before navigation', async () => {
	for (const value of [travelPolicy, { ...travelPolicy, excludedStayRanges: 'invalid' }]) {
		const { page, events } = pageHarness('GoodsDetails/SojournGoodsDetails', catalog(value))
		page.loadCollectState = () => {}
		page.getGoodsDetailFn(77)
		await new Promise(resolve => setImmediate(resolve))
		const sku = page.skuGroupList[0].skuDataList[0]
		page.reserveFn(sku, sku.combinationList[0], 0)
		page.popupDate.checkInDate = '2026-12-26'
		page.popupDate.checkOutDate = '2027-01-02'
		page.confirmFn()
		assert.notEqual(page.selectionError, '')
		assert.equal(events.navigations.length, 0)
	}
})

test('metadata popup tabs separate duration and season while retaining full policy, prices and dates', async () => {
	for (const [nights, checkOutDate] of [[7, '2026-11-06'], [14, '2026-11-13'], [30, '2026-11-29']]) {
		const expectedPolicy = { ...travelPolicy, stayStart: '2026-10-30' }
		const data = catalog(expectedPolicy)
		const sourceName = `${nights}晚套餐（2026-10-30至2026-12-31住宿夜）`
		data.optionList[1].skuName = sourceName
		data.optionList[1].options[0].optionValue = String(nights)
		const { page } = pageHarness('GoodsDetails/SojournGoodsDetails', data)
		page.loadCollectState = () => {}
		page.getGoodsDetailFn(77)
		await new Promise(resolve => setImmediate(resolve))
		const sku = page.skuGroupList[0].skuDataList[0]
		const combo = sku.combinationList[0]
		page.reserveFn(sku, combo, 0)
		page.popupDate.checkInDate = '2026-10-30'
		page.popupDate.checkOutDate = checkOutDate
		assert.equal(page.popupSkuDataList[0].popupLabel, `${nights + 1}天${nights}晚`)
		assert.equal(page.popupSkuDataList[0].popupPeriod, '2026.10.30–12.31')
		assert.equal(sku.name, sourceName)
		assert.equal(sku.day, nights)
		assert.equal(combo.name, '三餐')
		assert.equal(combo.price, '1200')
		assert.equal(page.popupDate.checkInDate, '2026-10-30')
		assert.equal(page.popupDate.checkOutDate, checkOutDate)
		assert.equal(page.selectionError, '')
		assert.match(page.selectedTravelPolicyText, /2026-10-30至2026-12-31/)
		assert.match(page.selectedTravelPolicyText, /2026-07-10至2026-08-20/)
		assert.deepEqual(JSON.parse(JSON.stringify(sku.travelPolicy)), expectedPolicy)
	}
	const legacyData = catalog()
	legacyData.optionList[1].skuName = '传统周套餐（原名称）'
	delete legacyData.optionList[1].options[0].optionParam
	const { page } = pageHarness('GoodsDetails/SojournGoodsDetails', legacyData)
	page.loadCollectState = () => {}
	page.getGoodsDetailFn(77)
	await new Promise(resolve => setImmediate(resolve))
	assert.equal(page.popupSkuDataList[0].popupLabel, '传统周套餐（原名称）')
})

test('season labels retain year boundaries and optional start/end bounds', () => {
	assert.equal(policy.travelPackagePeriodLabel(null), '')
	assert.equal(policy.travelPackagePeriodLabel({ version: 1 }), '')
	assert.equal(policy.travelPackagePeriodLabel({ stayStart: '2026-12-01', stayEnd: '2026-12-31' }), '2026.12.01–12.31')
	assert.equal(policy.travelPackagePeriodLabel({ stayStart: '2026-12-01', stayEnd: '2027-01-31' }), '2026.12.01–2027.01.31')
	assert.equal(policy.travelPackagePeriodLabel({ stayStart: '2026-12-01' }), '2026.12.01起')
	assert.equal(policy.travelPackagePeriodLabel({ stayEnd: '2026-12-31' }), '截至2026.12.31')
})

test('switching seasonal packages selects valid dates for both six and fourteen nights', () => {
	for (const nights of [6, 14]) {
		let checkInDate = ''
		for (const [stayStart, stayEnd] of [['2026-12-01', '2026-12-31'], ['2027-01-04', '2027-01-31'], ['2027-03-01', '2027-03-31']]) {
			const travelPolicy = { version: 1, stayStart, stayEnd }
			const dates = policy.defaultTravelDates({ travelPolicy, nights, checkInDate, today: '2026-10-31' })
			assert.equal(dates.checkInDate, stayStart)
			assert.equal(policy.validateTravelSelection({ travelPolicy, nights, ...dates }), '')
			checkInDate = dates.checkInDate
		}
	}
})

test('default dates preserve valid selection, skip excluded stays and never pick a past or expired stay', () => {
	const base = { nights: 6, today: '2026-10-31' }
	assert.deepEqual(policy.defaultTravelDates({ ...base, checkInDate: '2026-11-05' }), { checkInDate: '2026-11-05', checkOutDate: '2026-11-11' })
	assert.equal(policy.defaultTravelDates({ ...base, checkInDate: '2026-10-01' }).checkInDate, '2026-10-31')
	assert.equal(policy.defaultTravelDates({ ...base, travelPolicy: { excludedStayRanges: [{ start: '2026-11-02', end: '2026-11-08' }, { start: '2026-11-10', end: '2026-11-15' }] } }).checkInDate, '2026-11-16')
	const empty = { checkInDate: '', checkOutDate: '' }
	assert.deepEqual(policy.defaultTravelDates({ ...base, travelPolicy: { stayEnd: '2026-11-04' } }), empty)
	assert.deepEqual(policy.defaultTravelDates({ ...base, policyError: '配置无效' }), empty)
	assert.deepEqual(policy.defaultTravelDates({ ...base, nights: 0 }), empty)
	assert.deepEqual(policy.defaultTravelDates({ ...base, today: 'bad' }), empty)
	assert.deepEqual(policy.defaultTravelDates({ ...base, nights: 1, travelPolicy: { stayEnd: '2026-10-31' } }), { checkInDate: '2026-10-31', checkOutDate: '2026-11-01' })
})

test('actual popup switches seasonal dates before calendar ref is mounted, but keeps manual invalid date for validation', async () => {
	const data = catalog({ version: 1, stayStart: '2027-01-04', stayEnd: '2027-01-31' })
	data.optionList[1].options[0].optionValue = '6'
	const { page } = pageHarness('GoodsDetails/SojournGoodsDetails', data)
	page.loadCollectState = () => {}
	page.$nextTick = callback => callback()
	page.getGoodsDetailFn(77)
	await new Promise(resolve => setImmediate(resolve))
	const offer = page.skuGroupList[0].skuDataList[0]
	page.reserveFn(offer, offer.combinationList[0], 0)
	assert.equal(page.popupDate.checkInDate, '2027-01-04')
	assert.equal(page.popupDate.checkOutDate, '2027-01-10')
	assert.equal(page.calendarStayStart, '2027-01-04')
	assert.equal(page.calendarStayEnd, '2027-02-01')
	assert.equal(page.selectionError, '')
	page.calendarClickFn({ startStr: { dateStr: '2027-01-30' } })
	assert.equal(page.popupDate.checkInDate, '2027-01-30')
	assert.match(page.selectionError, /范围/)
})

test('three six-night seasonal offers have distinct labels, prices and enforced stay windows', async () => {
	const data = catalog()
	const prototype = data.optionList[1]
	const periods = [
		['2026-12-01', '2026-12-31', '3054'],
		['2027-01-04', '2027-01-31', '3054'],
		['2027-03-01', '2027-03-31', '2645']
	]
	const packages = periods.map(([stayStart, stayEnd, price], index) => ({
		...prototype, skuId: 202 + index, skuName: `7天6晚·三餐（${stayStart}~${stayEnd}）`,
		options: prototype.options.map(option => String(option.optionType) === '303'
			? { ...option, optionValue: '6', optionParam: metadata({ version: 1, capacity: 4, includedMeals: '三餐', stayStart, stayEnd }) }
			: String(option.optionType) === '302' ? { ...option, optionValue: price } : { ...option })
	}))
	data.optionList = [data.optionList[0], ...packages]
	const { page, events } = pageHarness('GoodsDetails/SojournGoodsDetails', data)
	page.loadCollectState = () => {}
	page.getGoodsDetailFn(77)
	await new Promise(resolve => setImmediate(resolve))
	const offers = page.skuGroupList[0].skuDataList
	assert.equal(offers.length, 3)
	assert.equal(new Set(offers.map(offer => offer.popupPeriod)).size, 3)
	assert.ok(offers.every(offer => offer.popupLabel === '7天6晚'))
	page.reserveFn(offers[0], offers[0].combinationList[0], 0)
	page.popupDate.checkInDate = '2027-01-05'
	page.popupDate.checkOutDate = '2027-01-11'
	page.confirmFn()
	assert.equal(events.navigations.length, 0)
	assert.match(page.selectionError, /范围/)
	page.selectDayFn(1)
	assert.equal(page.selectionError, '')
	assert.equal(page.selectedTravelPackage.combinationList[0].price, '3054')
	page.confirmFn()
	assert.equal(events.navigations.length, 1)
	assert.match(events.navigations[0].url, /skuId=203&skuSeqNo=1/)
})

test('popup duration is grouped independently of seasonal periods with no fixed card height', () => {
	const source = readFileSync(new URL('../packagesMall/GoodsDetails/SojournGoodsDetails.vue', import.meta.url), 'utf8')
	const style = readFileSync(new URL('../packagesMall/GoodsDetails/SojournGoodsDetails.scss', import.meta.url), 'utf8')
	assert.match(source, /class="package_duration">\{\{item.label\}\}/)
	assert.match(source, /v-for="\(item, index\) in popupDurationOptions"/)
	assert.doesNotMatch(source, /class="package_period"/)
	const popupCards = style.slice(style.indexOf('.popup_tag_view'), style.indexOf('.popup_customize_view'))
	assert.match(popupCards, /height: auto/)
	assert.match(popupCards, /flex-direction: column/)
	assert.match(popupCards, /word-break: break-all/)
	assert.doesNotMatch(popupCards, /(?<!-)height: 90rpx/)
})

test('optional eligibilityNotice preserves source text and strictly rejects invalid or overlong values', () => {
	for (const value of [eligibilityNotice, '住'.repeat(100)]) {
		const parsed = policy.parseTravelPolicy(metadata({ version: 1, eligibilityNotice: value }))
		assert.equal(parsed.eligibilityNotice, value)
		assert.ok(policy.travelPolicyText(parsed).includes(`入住资格（自我声明）：${value}`))
	}
	assert.equal(policy.parseTravelPolicy(metadata({ version: 1 })).eligibilityNotice, undefined)
	for (const value of [undefined, null, '', ' \t\n', true, 50, [], {}, '住'.repeat(101)]) {
		assert.throws(() => policy.parseTravelPolicy({ travelPolicy: { version: 1, eligibilityNotice: value } }), /eligibilityNotice/)
	}
})

test('actual detail and confirmation pages display exact eligibility policy without claiming age verification', async () => {
	const data = catalog({ ...travelPolicy, eligibilityNotice })
	data.goodsId = 278
	const detail = pageHarness('GoodsDetails/SojournGoodsDetails', data)
	detail.page.loadCollectState = () => {}
	detail.page.getGoodsDetailFn(278)
	await new Promise(resolve => setImmediate(resolve))
	const sku = detail.page.skuGroupList[0].skuDataList[0]
	detail.page.reserveFn(sku, sku.combinationList[0], 0)
	assert.ok(detail.page.selectedTravelPolicyText.includes(eligibilityNotice))
	const { page } = await confirmation(data, { id: '278', travelEligibilityConfirmed: 'true' })
	assert.equal(page.eligibilityNotice, eligibilityNotice)
	assert.ok(page.selectedTravelPolicyText.includes(eligibilityNotice))
	assert.equal(page.travelEligibilityConfirmed, false)
	assert.equal(page.isReadingAgreement, false)
	const source = readFileSync(new URL('../packagesMall/ConfirmOrder/SojournConfirmOrder.vue', import.meta.url), 'utf8')
	assert.match(source, /v-if="eligibilityNotice"/)
	assert.match(source, /<u-checkbox v-model="travelEligibilityConfirmed"><\/u-checkbox>/)
	assert.match(source, /\{\{ eligibilityNotice \}\}/)
	assert.match(source, /自我声明，非年龄核验/)
})

test('eligibility confirmation requires boolean true independently of booking agreement (stub orders only)', async () => {
	const { page, events } = await confirmation(catalog({ ...travelPolicy, eligibilityNotice }))
	page.reserveData.name = '合成测试'
	page.reserveData.phone = '13800000000'
	page.isReadingAgreement = true
	for (const value of [false, undefined, null, 0, 1, 'true', 'false', {}, []]) {
		page.travelEligibilityConfirmed = value
		page.createOrderFn()
		assert.equal(events.requests.length, 0)
		assert.match(events.toasts.at(-1).title, /入住资格自我声明/)
	}
	page.travelEligibilityConfirmed = true
	page.isReadingAgreement = false
	page.createOrderFn()
	assert.equal(events.requests.length, 0)
	assert.match(events.toasts.at(-1).title, /预订及入住须知/)
	page.isReadingAgreement = true
	page.createOrderFn()
	assert.equal(events.requests.length, 1)
	const request = JSON.parse(JSON.stringify(events.requests[0]))
	assert.deepEqual(request.params, { travelEligibilityConfirmed: true })
	assert.equal(Object.hasOwn(request, 'travelEligibilityConfirmed'), false)
	assert.deepEqual(Object.keys(page.reserveData).sort(), ['checkInDate', 'checkOutDate', 'day', 'name', 'peopleNumber', 'phone', 'roomNumber'].sort())
	// Recheck at the delayed submission boundary, not just on the initial click.
	let submit
	page.$u.throttle = callback => { submit = callback }
	page.createOrderFn()
	page.travelEligibilityConfirmed = false
	submit()
	assert.equal(events.requests.length, 1)
	assert.match(events.toasts.at(-1).title, /入住资格自我声明/)
})

test('746 without eligibilityNotice retains prior submission rules and payload', async () => {
	for (const value of [travelPolicy, null]) {
		const data = catalog(value)
		data.optionList[1].skuId = 746
		if (value === null) delete data.optionList[1].options[0].optionParam
		const { page, events } = await confirmation(data, { skuId: '746' })
		assert.equal(page.eligibilityNotice, '')
		assert.equal(page.eligibilityConfirmationError, '')
		assert.equal(page.travelEligibilityConfirmed, false)
		page.reserveData.name = '合成测试'
		page.reserveData.phone = '13800000000'
		page.isReadingAgreement = true
		page.createOrderFn()
		assert.equal(events.requests.length, 1)
		assert.equal(events.requests[0].skuId, 746)
		assert.equal(Object.hasOwn(events.requests[0], 'params'), false)
	}
})

test('invalid eligibility metadata blocks real page navigation and submission even when checked', async () => {
	for (const value of ['', 50, '住'.repeat(101)]) {
		const data = catalog({ ...travelPolicy, eligibilityNotice: value })
		const detail = pageHarness('GoodsDetails/SojournGoodsDetails', data)
		detail.page.loadCollectState = () => {}
		detail.page.getGoodsDetailFn(278)
		await new Promise(resolve => setImmediate(resolve))
		const sku = detail.page.skuGroupList[0].skuDataList[0]
		detail.page.reserveFn(sku, sku.combinationList[0], 0)
		detail.page.popupDate.checkInDate = '2026-10-29'
		detail.page.popupDate.checkOutDate = '2026-11-05'
		detail.page.confirmFn()
		assert.match(detail.page.selectionError, /eligibilityNotice/)
		assert.equal(detail.events.navigations.length, 0)
		const { page, events } = await confirmation(data)
		page.reserveData.name = '合成测试'
		page.reserveData.phone = '13800000000'
		page.isReadingAgreement = true
		page.travelEligibilityConfirmed = true
		page.createOrderFn()
		assert.match(page.selectionError, /eligibilityNotice/)
		assert.equal(events.requests.length, 0)
	}
})

function seasonalCatalog() {
	const data = catalog()
	const source = data.optionList[1]
	const windows = [['2026-12-01', '2026-12-31'], ['2027-01-04', '2027-01-31'], ['2027-03-01', '2027-03-31']]
	let id = 1579
	data.optionList = [data.optionList[0], ...[6, 14, 29].flatMap(nights => windows.flatMap(([stayStart, stayEnd], i) => {
		if (nights === 29 && i === 1) return []
		const total = { 6: [3054, 3054, 2645], 14: [6752, 6752, 5350], 29: [12394, 0, 9520] }[nights][i]
		return [{ ...source, skuId: id++, options: [...source.options.map(o => String(o.optionType) === '303'
			? { ...o, optionValue: String(nights), optionParam: metadata({ version: 1, capacity: 4, includedMeals: '三餐', stayStart, stayEnd }) }
			: String(o.optionType) === '302' ? { ...o, optionValue: String(total) } : { ...o }), { optionType: '301', skuSeqNo: 1, optionValue: String(total / nights) }] }]
	}))]
	return data
}

test('eight source seasonal SKUs render as three durations; selected date chooses original price and checkout SKU', async () => {
	const { page, events } = pageHarness('GoodsDetails/SojournGoodsDetails', seasonalCatalog())
	page.loadCollectState = () => {}
	page.$nextTick = fn => fn()
	page.getGoodsDetailFn(77)
	await new Promise(resolve => setImmediate(resolve))
	assert.equal(page.skuGroupList[0].skuDataList.length, 8)
	assert.deepEqual(Array.from(page.durationOptions(0), o => o.label), ['7天6晚', '15天14晚', '30天29晚'])
	const from = page.getCurrentCombinationList(0)[0]
	assert.equal(from.price, '2645')
	assert.equal(from.isExact, false)
	page.reserveFn(page.skuGroupList[0].skuDataList[from.skuIndex], from, 0)
	assert.equal(page.popupDurationOptions.length, 3)
	assert.equal(page.calendarRateData.prices['2026-12-01'], '￥509/间')
	assert.equal(page.calendarRateData.prices['2027-01-04'], '￥509/间')
	assert.equal(page.calendarRateData.prices['2027-03-01'], '￥440.83/间')
	assert.equal(page.calendarRateData.prices['2027-02-01'], undefined)
	assert.equal(page.calendarRateData.prices['2026-12-27'], undefined)
	page.selectCalendarMonth(page.calendarRateData.months[0])
	assert.equal(page.popupDate.skuId, 1579)
	assert.equal(page.popupDate.checkOutDate, '2026-12-07')
	assert.equal(page.selectedRateTotal, '3054/间')
	assert.equal(page.calendarFocusId, 'travel-month-2026-12')
	page.selectCalendarMonth(page.calendarRateData.months[2])
	assert.equal(page.popupDate.skuId, 1581)
	assert.equal(page.getCurrentCombinationList(0)[0].price, '2645')
	assert.equal(page.calendarFocusId, 'travel-month-2027-03')
	page.confirmFn()
	assert.match(events.navigations[0].url, /skuId=1581&skuSeqNo=1/)
	page.selectDayFn(page.popupDurationOptions[2].indices[0])
	assert.equal(page.popupDate.day, 29)
	assert.equal(page.calendarRateData.prices['2027-01-04'], undefined)
	assert.equal(page.calendarRateData.prices['2027-03-03'], '￥328.28/间')
})

test('confirmation date changes update season and keep new raw SKU through order resynchronization', async () => {
	const { page } = await confirmation(seasonalCatalog(), { skuId: '1579', day: '6', checkInDate: '2026-12-01', checkOutDate: '2026-12-07' })
	assert.equal(page.selectionError, '')
	assert.equal(page.selectedRateFamily.variants.length, 3)
	assert.equal(page.calendarRateData.prices['2027-03-01'], '￥440.83/间')
	page.calendarClickFn({ startStr: { dateStr: '2027-03-01' } })
	assert.equal(page.reserveData.checkOutDate, '2027-03-07')
	assert.equal(page.skuPrice, 2645)
	assert.equal(page.selSkuId, 1581)
	assert.equal(page.selectionError, '')
	page.syncSkuSelection()
	assert.equal(page.selSkuId, 1581)
	page.calendarClickFn({ startStr: { dateStr: '2027-02-01' } })
	assert.equal(page.selSkuId, 1581)
	assert.equal(page.reserveData.checkInDate, '2027-03-01')
	page.calendarClickFn({ startStr: { dateStr: '2027-01-05' } })
	assert.equal(page.selSkuId, 1580)
	assert.equal(page.skuPrice, 3054)
	assert.equal(page.selectionError, '')
})

test('rate grouping never merges distinct benefits, room parents, combinations, overlap or invalid policy', () => {
	const make = (start, end) => ({ day: 6, parSkuId: 200, travelPolicy: { version: 1, capacity: 4, stayStart: start, stayEnd: end }, combinationList: [{ name: '三餐', priceUnit: '元/间', skuSeqNo: 1, price: 600 }] })
	const a = make('2026-12-01', '2026-12-31'), b = make('2027-01-04', '2027-01-31')
	assert.equal(rates.groupTravelRates([a, b]).length, 1)
	for (const changed of [
		{ ...b, parSkuId: 201 }, { ...b, day: 14 }, { ...b, policyError: 'bad' },
		{ ...b, travelPolicy: null }, { ...b, travelPolicy: { ...b.travelPolicy, capacity: 2 } },
		{ ...b, travelPolicy: { ...b.travelPolicy, eligibilityNotice: '需陪同' } },
		{ ...b, combinationList: [{ ...b.combinationList[0], name: '早餐' }] },
		{ ...b, combinationList: [{ ...b.combinationList[0], priceUnit: '元/人' }] }, a
	]) assert.equal(rates.groupTravelRates([a, changed]).length, 2)
	assert.equal(rates.defaultRateSelection([{ ...b, combinationList: [{ ...b.combinationList[0], price: Infinity }] }], 6, 1, '', '2026-10-31'), null)
})

test('shared calendar blocks missing seasonal quotes while preserving flat-price consumers', () => {
	const source = readFileSync(new URL('../components/mobile-calendar-simple/Calendar.vue', import.meta.url), 'utf8')
	const context = { module: { exports: {} } }
	vm.runInNewContext(source.match(/<script>([\s\S]*?)<\/script>/)[1].replace('export default', 'module.exports ='), context)
	const options = context.module.exports
	const page = { ...options.data(), dailyPrices: { '2027-03-01': '￥440.83/间' }, price: '￥509/间' }
	for (const [key, fn] of Object.entries(options.methods)) page[key] = fn.bind(page)
	assert.equal(page.datePrice(1, 3, 2027), '￥440.83/间')
	assert.equal(page.datePrice(1, 2, 2027), '不可订')
	page.chooseDate(1, 2, 2027)
	assert.equal(page.startDates, '')
	page.dailyPrices = null
	assert.equal(page.datePrice(1, 2, 2027), '￥509/间')
	assert.equal(page.dateUnavailable(1, 2, 2027), false)
	page.dailyPrices = { '2027-03-01': '￥440.83/间' }
	page.fixedEnd = true
	page.mode = 2
	page.today = 0
	page.betweenEnds = NaN
	const events = []
	page.$emit = (...event) => events.push(event)
	page.chooseDate(1, 3, 2027)
	assert.equal(events[0][0], 'calendarClick')
	assert.equal(events[0][1].startStr.dateStr, '2027-03-01')
	page.focusMonthId = 'travel-month-2027-03'
	page.$nextTick = fn => fn()
	const selectors = []
	const query = { select(selector) { selectors.push(selector); return this }, boundingClientRect() { return this }, exec(fn) { fn([{ top: -700 }, { top: 100 }]) } }
	context.uni = { createSelectorQuery: () => ({ in: () => query }) }
	page.scrollToFocusedMonth()
	assert.deepEqual(selectors, ['.calendar-tz', '#travel-month-2027-03'])
	assert.equal(events[1][0], 'monthFocus')
	assert.equal(events[1][1], 800)
	assert.match(source, /senior-mode\.rate-calendar/)
	assert.match(source, /font-size: 24rpx !important/)
})
