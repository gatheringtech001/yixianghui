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

// Keep the large integer separate from its small suffix in compact cards.
// Round only nightly references; never relabel a legacy whole-package price.
export function catalogPriceParts(goods, price = goods && goods.price) {
	const unit = String((goods && goods.unit) || '').trim()
	const match = /^(人|间|套)\/晚起?$/.exec(unit)
	if (goods && goods.goodsType === 'hotel' && match) {
		const number = Number(price)
		return Number.isFinite(number) && number > 0
			? { amount: String(Math.round(number)), suffix: `起/${match[1]}/晚` }
			: { amount: '待确认', suffix: '' }
	}
	return { amount: String(price == null || price === '' ? 0 : price), suffix: '' }
}

export function formatCatalogPrice(goods, price = goods && goods.price) {
	const { amount, suffix } = catalogPriceParts(goods, price)
	return amount + suffix
}

function overviewText(value) {
	return String(value || '').replace(/<(script|style)\b[^>]*>[\s\S]*?<\/\1>/gi, '')
		.replace(/<[^>]*>/g, ' ')
		.replace(/&(?:nbsp|#160);/gi, ' ').replace(/&amp;/gi, '&')
		.replace(/&quot;/gi, '"').replace(/&apos;|&#39;/gi, "'")
		.replace(/&#(x[0-9a-f]+|[0-9]+);/gi, (_, code) => {
			const point = code[0].toLowerCase() === 'x' ? parseInt(code.slice(1), 16) : Number(code)
			return point > 0 && point <= 0x10ffff ? String.fromCodePoint(point) : ''
		}).replace(/&lt;/gi, '<').replace(/&gt;/gi, '>')
		.replace(/\s+/g, ' ').trim()
}

// Read ONLY the published Yuque overview rows; generic catalog tags are not
// substitutes for the source's 标签 row. No browser DOM dependency on WeChat.
export function travelCatalogOverview(goods) {
	const result = { tags: [], facts: '' }
	if (!goods || goods.goodsType !== 'hotel') return result
	const fields = Object.create(null)
	for (const section of Array.isArray(goods.features) ? goods.features : []) {
		if (!/基地概况/.test(section.sectionName || '')) continue
		for (const row of String(section.content || '').matchAll(/<tr\b[^>]*>([\s\S]*?)<\/tr>/gi)) {
			const cells = [...row[1].matchAll(/<t[dh]\b[^>]*>([\s\S]*?)<\/t[dh]>/gi)].map(cell => overviewText(cell[1]))
			if (cells.length !== 2) continue
			const key = cells[0].replace(/[\s：:]/g, '')
			const value = cells[1]
			if (value && !/待补|待确认|待核实|暂无|未提供|未注明|未知|^[-—/]+$/.test(value) && !fields[key]) fields[key] = value
		}
	}
	const year = fields['开业/装修'] || fields['开业／装修'] || fields['开业时间'] || fields['装修时间'] || fields['开业'] || fields['装修']
	const altitude = fields['海拔'] || fields['海拔高度']
	const facts = []
	if (year) facts.push(year.replace(/\s*(年|月|日)\s*/g, '$1'))
	if (altitude && /\d/.test(altitude)) facts.push('海拔' + altitude.replace(/^海拔[：:\s]*/, '').replace(/\s*米/g, '米') + (/米|m\b/i.test(altitude) ? '' : '米'))
	result.facts = facts.join(' | ')
	result.tags = [...new Set((fields['标签'] || '').split(/[/／,，、|｜;；\n]+/).map(tag => tag.trim()).filter(Boolean))]
	return result
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
