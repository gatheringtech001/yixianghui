import { defaultRateSelection, rateCombination, travelDateAfter } from './travelRates.js'
import { validateTravelSelection } from './travelPolicy.js'
import { travelQuote } from './travelPresentation.js'

export { travelQuote } from './travelPresentation.js'
export const isSoloTravelRate = sku => Number((sku.travelPolicy || {}).capacity) === 1

export function groupTravelDurations(skus = []) {
	const groups = []
	skus.forEach((sku, index) => {
		if (isSoloTravelRate(sku) || !Number.isSafeInteger(Number(sku.day)) || Number(sku.day) <= 0) return
		let group = groups.find(item => item.nights === Number(sku.day))
		if (!group) {
			group = { nights: Number(sku.day), label: `${Number(sku.day) + 1}天${sku.day}晚`, indices: [], variants: [] }
			groups.push(group)
		}
		group.indices.push(index)
		group.variants.push(sku)
	})
	return groups.sort((a, b) => a.nights - b.nights)
}

export function travelMealLabel(name) {
	return ({ '三餐': '一日三餐', '含三餐': '一日三餐', '早餐': '只含早', '含早餐': '只含早' })[name] || name || '餐食标准见说明'
}

export function travelMealRows(skus, duration, date, today) {
	if (!duration) return []
	const rows = new Map()
	for (const sku of duration.variants) {
		for (const combo of sku.combinationList || []) {
			const quote = travelQuote(sku, combo)
			if (!quote || !defaultRateSelection([sku], sku.day, combo.skuSeqNo, '', today)) continue
			const exact = !!date && !validateTravelSelection({ ...sku, nights: sku.day, checkInDate: date, checkOutDate: travelDateAfter(date, sku.day) })
			if (date && !exact) continue
			const key = travelMealLabel(combo.name)
			const prior = rows.get(key)
			// Do not compare unlike units when no explicit occupancy exists.
			if (!prior || (quote.unit === '人' && prior.quote.unit !== '人') || (quote.unit === prior.quote.unit && quote.nightly < prior.quote.nightly)) {
				rows.set(key, { ...combo, name: travelMealLabel(combo.name), sourceMeal: key, nights: sku.day, quote, skuIndex: skus.indexOf(sku), isExact: exact })
			}
		}
	}
	return [...rows.values()]
}

// Match only the exact room, duration and meal entitlement. No fuzzy room/meal mapping.
export function travelPartyVariants(skus, selected) {
	const selectedCombo = rateCombination(selected, selected.skuSeqNo)
	const meal = (selected.travelPolicy || {}).includedMeals || (selectedCombo || {}).name
	return (skus || []).filter(sku => Number(sku.parSkuId) === Number(selected.parSkuId)
		&& Number(sku.day) === Number(selected.day)
		&& ((sku.travelPolicy || {}).includedMeals || (rateCombination(sku, sku.skuSeqNo) || {}).name) === meal
		&& !sku.policyError)
}

export function selectTravelPartyRate(variants, people, rooms, date, today) {
	if (![people, rooms].every(n => Number.isSafeInteger(Number(n)) && Number(n) > 0)) return null
	// A single-occupancy quote is shown only when the booking party itself is one.
	const candidates = variants.filter(sku => people === 1 ? rooms === 1 && isSoloTravelRate(sku)
		: !isSoloTravelRate(sku) && Number((sku.travelPolicy || {}).capacity) >= people / rooms)
	candidates.sort((a, b) => Number(a.travelPolicy.capacity) - Number(b.travelPolicy.capacity))
	return candidates.find(sku => date >= today && !validateTravelSelection({ ...sku, nights: sku.day, checkInDate: date,
		checkOutDate: travelDateAfter(date, sku.day), roomNumber: rooms, peopleNumber: people })) || null
}
