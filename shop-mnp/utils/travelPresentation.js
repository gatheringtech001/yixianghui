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

export function formatCalendarPrice(value) {
	const number = positiveNumber(value)
	if (!number) return ''
	const rounded = Math.round(number * 100) / 100
	const text = Number.isInteger(rounded)
		? String(rounded)
		: rounded.toFixed(2).replace(/0+$/, '').replace(/\.$/, '')
	return `￥${text}/人`
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
