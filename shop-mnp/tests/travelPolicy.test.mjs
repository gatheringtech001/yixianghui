import assert from 'node:assert/strict'
import test from 'node:test'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'

import * as policy from '../utils/travelPolicy.js'
import * as presentation from '../utils/travelPresentation.js'

const travelPolicy = {
	version: 1, capacity: 2, includedMeals: '三餐',
	stayStart: '2026-01-01', stayEnd: '2026-12-31',
	excludedStayRanges: [{ start: '2026-07-10', end: '2026-08-20' }]
}
const metadata = value => JSON.stringify({ travelPolicy: value })
const stay = overrides => ({ travelPolicy, nights: 6, checkInDate: '2026-10-29', checkOutDate: '2026-11-04', ...overrides })
const catalog = (value = travelPolicy) => ({
	goodsId: 77, goodsName: '测试基地', goodsImages: '',
	optionList: [
		{ skuId: 200, skuType: '200', price: 0, status: '1', options: [] },
		{ skuId: 202, parSkuId: 200, skuType: '202', skuName: '周套餐', status: '1', options: [
			{ optionType: '303', skuSeqNo: 0, optionValue: '7', optionValueUnit: '晚', optionParam: metadata(value) },
			{ optionType: '302', skuSeqNo: 1, optionValue: '1200', optionValueUnit: '元/间' },
			{ optionType: '304', skuSeqNo: 1, optionValue: '套餐' }
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
		...policy, ...presentation, module: { exports: {} }, Calendar: {}, AuthProfilePopup: {},
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
