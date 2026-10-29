const DAY = 86400000
const own = (value, key) => Object.prototype.hasOwnProperty.call(value, key)
const object = value => value !== null && typeof value === 'object' && !Array.isArray(value)
const positiveInteger = value => Number.isSafeInteger(value) && value > 0

// UTC day arithmetic avoids DST and Date's silent rollover of invalid calendar dates.
function dateDay(value) {
	if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return NaN
	const time = Date.parse(`${value}T00:00:00Z`)
	return Number.isFinite(time) && new Date(time).toISOString().slice(0, 10) === value ? time / DAY : NaN
}

export function parseStayNights(value, name = '', unit = '') {
	const total = Number(value)
	// An explicit 303 night count is authoritative, even for a package named 周/月.
	if (String(unit).includes('晚') && !String(unit).includes('天')) {
		return positiveInteger(total) ? total : 0
	}
	if (String(name).includes('半月')) return 14
	if (String(name).includes('周')) return 6
	if (String(name).includes('月')) return 29
	return positiveInteger(total) ? Math.max(total - 1, 0) : 0
}

export function supportsCustomNights(group) {
	return !!(group && group.skuId && Number.isFinite(Number(group.price)) && Number(group.price) > 0)
}

export function parseTravelPolicy(param) {
	if (param === undefined || param === null || param === '') return null
	const metadata = typeof param === 'string' ? JSON.parse(param) : param
	if (!object(metadata)) throw new Error('套餐 metadata 必须为 JSON 对象')
	if (!own(metadata, 'travelPolicy')) return null
	const policy = metadata.travelPolicy
	if (!object(policy) || policy.version !== 1) throw new Error('travelPolicy.version 必须为 1')
	if (own(policy, 'capacity') && !positiveInteger(policy.capacity)) throw new Error('capacity 必须为正整数')
	if (own(policy, 'includedMeals') && (typeof policy.includedMeals !== 'string' || !policy.includedMeals.trim())) {
		throw new Error('includedMeals 必须为非空字符串')
	}
	if (own(policy, 'eligibilityNotice') && (typeof policy.eligibilityNotice !== 'string' || !policy.eligibilityNotice.trim() || policy.eligibilityNotice.length > 100)) {
		throw new Error('eligibilityNotice 必须为非空且不超过100字符的字符串')
	}
	for (const key of ['stayStart', 'stayEnd']) {
		if (own(policy, key) && !Number.isFinite(dateDay(policy[key]))) throw new Error(`${key} 必须为有效 YYYY-MM-DD 日期`)
	}
	if (policy.stayStart && policy.stayEnd && policy.stayStart > policy.stayEnd) throw new Error('入住范围起止倒置')
	if (own(policy, 'excludedStayRanges')) {
		if (!Array.isArray(policy.excludedStayRanges)) throw new Error('excludedStayRanges 必须为数组')
		policy.excludedStayRanges.forEach(range => {
			if (!object(range) || !Number.isFinite(dateDay(range.start)) || !Number.isFinite(dateDay(range.end)) || range.start > range.end) {
				throw new Error('不可入住日期范围无效')
			}
		})
	}
	return policy
}

// The only policy authority is the package's public (seq 0) 303 row.
export function readTravelPackage(sku) {
	const rows = (sku.options || []).filter(row => String(row.optionType) === '303' && String(row.skuSeqNo) === '0')
	const row = rows[0] || {}
	const day = parseStayNights(row.optionValue, sku.skuName, row.optionValueUnit)
	try {
		if (rows.length > 1) throw new Error('公共 303 配置重复')
		return { day, travelPolicy: parseTravelPolicy(row.optionParam), policyError: '' }
	} catch (error) {
		return { day, travelPolicy: null, policyError: `套餐政策配置错误：${error.message}` }
	}
}

export function travelCapacity(policy) {
	return policy && policy.capacity !== undefined ? policy.capacity : 2
}

export function travelMealPresentation(meals) {
	if (meals === '不含餐' || meals.startsWith('餐次未注明')) {
		return {
			title: meals === '不含餐' ? '不含餐' : '餐食标准见资料',
			description: `餐食说明：${meals}；本订单未另外计收餐费。`,
			policyText: `餐食说明：${meals}；本订单未另外计收餐费`
		}
	}
	return {
		title: '已含套餐内',
		description: `套餐供餐：${meals}（不另收餐费）。`,
		policyText: `供餐：${meals}，套餐不另收餐费`
	}
}

export function travelPolicyText(policy) {
	if (!policy) return ''
	const text = []
	if (policy.capacity) text.push(`每间最多${policy.capacity}人`)
	if (policy.includedMeals) text.push(travelMealPresentation(policy.includedMeals).policyText)
	if (policy.stayStart || policy.stayEnd) text.push(`每个入住夜须在${policy.stayStart || '不限'}至${policy.stayEnd || '不限'}内`)
	if (policy.excludedStayRanges && policy.excludedStayRanges.length) text.push(`不可入住：${policy.excludedStayRanges.map(range => `${range.start}至${range.end}（含首尾）`).join('、')}`)
	if (policy.eligibilityNotice) text.push(`入住资格（自我声明）：${policy.eligibilityNotice}`)
	return text.join('；')
}

export function validateTravelSelection({ travelPolicy, policyError, nights, checkInDate, checkOutDate, roomNumber, peopleNumber }) {
	if (policyError) return policyError
	if (!positiveInteger(Number(nights))) return '套餐晚数无效，请返回重新选择'
	const start = dateDay(checkInDate)
	const end = dateDay(checkOutDate)
	if (!Number.isFinite(start) || !Number.isFinite(end)) return '请选择有效的入住和退房日期'
	if (end - start !== Number(nights)) return `套餐为${nights}晚，入住与退房日期不匹配`
	const policy = travelPolicy || {}
	// [check-in, check-out): the checkout day is not an occupied night.
	if ((policy.stayStart && start < dateDay(policy.stayStart)) || (policy.stayEnd && end - 1 > dateDay(policy.stayEnd))) {
		return '每个入住夜均须在套餐可入住日期范围内'
	}
	const excluded = (policy.excludedStayRanges || []).find(range => start <= dateDay(range.end) && end > dateDay(range.start))
	if (excluded) return `不可入住${excluded.start}至${excluded.end}（含首尾），请选择其他日期`
	if (roomNumber !== undefined || peopleNumber !== undefined) {
		if (!positiveInteger(Number(roomNumber)) || !positiveInteger(Number(peopleNumber))) return '房间数和入住人数必须为正整数'
		if (Number(peopleNumber) > Number(roomNumber) * travelCapacity(policy)) return `每间最多入住${travelCapacity(policy)}人，请调整人数`
	}
	return ''
}
