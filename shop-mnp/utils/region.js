import provinces from '@/uview-ui/libs/address/provinces.json'
import citys from '@/uview-ui/libs/address/citys.json'
import areas from '@/uview-ui/libs/address/areas.json'

export function formatRegionAddress(province, city, area) {
	if (!province || !city || !area) return ''
	return `${province.name}${city.name}${area.name}`
}

export function parseRegionFromAddress(addressText) {
	if (!addressText) return null
	const text = String(addressText).trim()
	if (!text) return null

	const firstTwoKey = text.substring(0, 2)
	let provinceIndex = -1
	let province = null

	for (let i = 0; i < provinces.length; i++) {
		const item = provinces[i]
		if (item.name.indexOf(firstTwoKey) === 0) {
			province = item
			provinceIndex = i
			break
		}
	}
	if (provinceIndex === -1) return null

	const citysArr = citys[provinceIndex]
	let cityIndex = -1
	let city = null

	for (let i = 0; i < citysArr.length; i++) {
		const item = citysArr[i]
		const cityName = item.name.substr(0, item.name.length - 1)
		if (text.indexOf(cityName) > -1) {
			city = item
			cityIndex = i
			break
		}
	}
	if (cityIndex === -1) return null

	const areasArr = areas[provinceIndex][cityIndex]
	let area = null

	for (let i = 0; i < areasArr.length; i++) {
		const item = areasArr[i]
		let reg = item.name
		if (item.name.length > 2) {
			reg += `|${item.name.substr(0, item.name.length - 1)}`
		}
		if (text.search(new RegExp(reg)) > -1) {
			area = item
			break
		}
	}
	if (!area) return null

	return {
		province,
		city,
		area,
		codes: [province.code, city.code, area.code],
		names: [province.name, city.name, area.name]
	}
}
