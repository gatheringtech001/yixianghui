function positiveNumber(value) {
	const number = Number(value)
	return Number.isFinite(number) && number > 0 ? number : 0
}

export function resolveCalendarUnitPrice({ average, nightPrice, total, nights } = {}) {
	const explicitAverage = positiveNumber(average)
	if (explicitAverage) return explicitAverage

	const explicitNightPrice = positiveNumber(nightPrice)
	if (explicitNightPrice) return explicitNightPrice

	const packageTotal = positiveNumber(total)
	const stayNights = positiveNumber(nights)
	return packageTotal && stayNights ? packageTotal / stayNights : 0
}

export function travelPriceUnit(value) {
	return /间|套/.test(String(value || '')) ? '间' : '人'
}

export function formatCalendarPrice(value, unit = '人') {
	const number = positiveNumber(value)
	if (!number) return ''
	const rounded = Math.round(number * 100) / 100
	const text = Number.isInteger(rounded)
		? String(rounded)
		: rounded.toFixed(2).replace(/0+$/, '').replace(/\.$/, '')
	return `￥${text}/${travelPriceUnit(unit)}`
}

// Only nightly catalog references are rounded; package and non-travel prices stay exact.
export function formatCatalogPrice(goods, price = goods && goods.price) {
	const unit = String((goods && goods.unit) || '').trim()
	if (goods && goods.goodsType === 'hotel' && /^(人|间|套)\/晚起?$/.test(unit)) {
		const number = Number(price)
		return Number.isFinite(number) && number > 0 ? `${Math.round(number)}/${unit}` : '待确认'
	}
	return String(price == null || price === '' ? 0 : price)
}

export function compactListingText(value, maxLength = 56) {
	const text = String(value || '')
		.replace(/<[^>]*>/g, ' ')
		.replace(/&nbsp;|&#160;/gi, ' ')
		.replace(/&amp;/gi, '&')
		.replace(/[\r\n\t]+/g, ' ')
		.replace(/[•·]{2,}/g, '·')
		.replace(/\s+/g, ' ')
		.trim()
	if (!text || text.length <= maxLength) return text
	return `${text.slice(0, maxLength).trim()}…`
}

export function sanitizeTravelCustomerText(value) {
	const text = String(value || '').trim()
	if (!text) return ''
	const sanitized = text.replace(/知识库价格[，,：:\s-]*/g, '').trim()
	return sanitized === '按人计价' ? '' : sanitized
}

const money = cents => (cents / 100).toFixed(2).replace(/\.00$/, '').replace(/(\.\d)0$/, '$1')

// Only an explicit quotation occupancy may be used, never the legacy capacity default.
export function travelQuote(sku, combo) {
	const cents = Math.round(Number(combo && combo.price) * 100)
	const nights = Number(sku && sku.day)
	if (!Number.isSafeInteger(cents) || cents <= 0 || !Number.isSafeInteger(nights) || nights <= 0) return null
	const unit = travelPriceUnit(combo.priceUnit)
	const occupancy = Number((sku.travelPolicy || {}).capacity)
	const divisor = unit === '人' ? 1 : Number.isSafeInteger(occupancy) && occupancy > 0 ? occupancy : 1
	const displayUnit = unit === '人' || divisor > 1 || occupancy === 1 ? '人' : '间'
	return {
		nightly: cents / 100 / divisor / nights,
		// Whole-yuan nightly reference only; never round the payable package total.
		amount: String(Math.round(cents / 100 / divisor / nights)), unit: displayUnit,
		// Native 302 remains the exact payable room/package total, never the rounded reference.
		total: money(cents), totalUnit: unit,
		occupancyLabel: unit === '间' && occupancy > 0 ? `${occupancy}人/间套餐` : ''
	}
}
