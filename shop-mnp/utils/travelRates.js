import { defaultTravelDates, validateTravelSelection } from './travelPolicy.js'
import { formatCalendarPrice, resolveCalendarUnitPrice } from './travelPresentation.js'

const DAY = 86400000
export function travelDateAfter(date, nights) {
	const time = Date.parse(`${date}T00:00:00Z`)
	return Number.isFinite(time) ? new Date(time + Number(nights) * DAY).toISOString().slice(0, 10) : ''
}

// Presentation only: keep every source SKU and its total intact for checkout.
// Never merge different room parents, meals, units, capacity or eligibility.
export function groupTravelRates(skus = []) {
	const groups = []
	skus.forEach((sku, index) => {
		const policy = sku.travelPolicy
		const { stayStart, stayEnd, excludedStayRanges, ...benefits } = policy || {}
		const signature = !sku.policyError && policy && stayStart && stayEnd && Number(sku.day) > 0
			? JSON.stringify([sku.parSkuId || '', Number(sku.day), Object.keys(benefits).sort().map(k => [k, benefits[k]]), (sku.combinationList || []).map(c => [c.name, c.priceUnit, c.skuSeqNo])]) : `legacy:${index}`
		let group = groups.find(item => item.signature === signature && item.variants.every(other => {
			const p = other.travelPolicy
			return p && (p.stayEnd < stayStart || stayEnd < p.stayStart)
		}))
		if (!group) {
			group = { signature, label: policy && !sku.policyError ? `${Number(sku.day) + 1}天${sku.day}晚` : sku.name, indices: [], variants: [] }
			groups.push(group)
		}
		group.indices.push(index)
		group.variants.push(sku)
	})
	return groups
}

export function rateCombination(sku, seq) {
	return (sku && sku.combinationList || []).find(c => Number(c.skuSeqNo) === Number(seq))
}

export function resolveTravelRate(variants, date, nights, seq, today) {
	if (!date || date < today) return null
	const end = travelDateAfter(date, nights)
	return (variants || []).find(sku => {
		const combo = rateCombination(sku, seq)
		return Number(sku.day) === Number(nights) && combo && Number.isFinite(Number(combo.price)) && Number(combo.price) > 0
			&& !validateTravelSelection({ ...sku, nights, checkInDate: date, checkOutDate: end })
	}) || null
}

export function defaultRateSelection(variants, nights, seq, date, today) {
	const selected = resolveTravelRate(variants, date, nights, seq, today)
	if (selected) return { sku: selected, checkInDate: date, checkOutDate: travelDateAfter(date, nights) }
	return (variants || []).map(sku => ({ sku, ...defaultTravelDates({ ...sku, nights, today }) }))
		.filter(item => {
			const combo = rateCombination(item.sku, seq)
			return item.checkInDate && combo && Number.isFinite(Number(combo.price)) && Number(combo.price) > 0
		})
		.sort((a, b) => a.checkInDate.localeCompare(b.checkInDate))[0] || null
}

export function travelRatePrice(sku, seq) {
	const combo = rateCombination(sku, seq)
	return combo ? formatCalendarPrice(resolveCalendarUnitPrice({ average: combo.average, total: combo.price, nights: sku.day }), combo.priceUnit) : ''
}

export function buildTravelRateCalendar(variants, nights, seq, today) {
	if (!variants || !variants.length) return { first: today, end: '', prices: {}, months: [], count: 6 }
	const starts = (variants || []).map(s => (s.travelPolicy || {}).stayStart).filter(Boolean).sort()
	const ends = (variants || []).map(s => (s.travelPolicy || {}).stayEnd).filter(Boolean).sort()
	const first = starts.length === variants.length && starts[0] > today ? starts[0] : today
	const fallbackEnd = new Date(`${first}T00:00:00Z`)
	fallbackEnd.setUTCMonth(fallbackEnd.getUTCMonth() + 6)
	const last = ends.length === variants.length ? ends[ends.length - 1] : fallbackEnd.toISOString().slice(0, 10)
	const prices = {}
	const months = []
	for (let date = first; date && date <= last; date = travelDateAfter(date, 1)) {
		const sku = resolveTravelRate(variants, date, nights, seq, today)
		if (!sku) continue
		prices[date] = travelRatePrice(sku, seq)
		const month = date.slice(0, 7)
		if (!months.some(m => m.month === month)) months.push({ month, date, label: `${month.slice(0, 4)}年${Number(month.slice(5))}月` })
	}
	const end = travelDateAfter(last, 1)
	const count = Math.max(1, (Number(end.slice(0, 4)) - Number(first.slice(0, 4))) * 12 + Number(end.slice(5, 7)) - Number(first.slice(5, 7)) + 1)
	return { first, end, prices, months, count }
}
