function parseStayNights(order) {
	const interCount = Number(order && order.interCount)
	if (Number.isFinite(interCount) && interCount > 0) return interCount
	if (!order || !order.checkInDate || !order.checkOutDate) return 0
	const checkIn = new Date(String(order.checkInDate).replace(/-/g, '/'))
	const checkOut = new Date(String(order.checkOutDate).replace(/-/g, '/'))
	const diff = checkOut.getTime() - checkIn.getTime()
	return diff > 0 ? Math.ceil(diff / (1000 * 3600 * 24)) : 0
}

function getStayDurationText(order) {
	const nights = parseStayNights(order)
	if (!nights) return ''
	return `${nights + 1}天${nights}晚`
}

function isCustomNightHotelOrder(order, goods) {
	if (!goods || goods.goodsType !== 'hotel' || !order) return false
	const seq = Number(order.skuSeqNo)
	return !Number.isFinite(seq) || seq <= 0
}

export function getOrderProductName(order, goods, productNameMap = {}) {
	if (!goods) return ''
	const goodsId = (order && order.goodsId) || goods.goodsId
	const cached = goodsId && productNameMap[goodsId]
	if (cached) return cached
	if (goods.specifications && goods.goodsName) return goods.goodsName
	return goods.goodsName || ''
}

export function getOrderProductSpec(order, goods, productNameMap = {}) {
	if (!goods) return ''
	let spec = goods.specifications || goods.skuDataValues || ''
	const productName = getOrderProductName(order, goods, productNameMap)

	if (!spec && productName && goods.goodsName && goods.goodsName !== productName) {
		spec = goods.goodsName
	}

	if (goods.goodsType === 'hotel') {
		if (isCustomNightHotelOrder(order, goods)) {
			const duration = getStayDurationText(order)
			if (duration) {
				return spec ? `${spec} · 自选${duration}` : `自选${duration}`
			}
			return spec || '自选入住'
		}
		return spec
	}

	if (spec && spec !== productName) return spec
	if (goods.unit && goods.goodsType !== 'education' && goods.unit !== productName) {
		return goods.unit
	}
	return ''
}

export function collectGoodsIds(orders) {
	const ids = new Set()
	;(orders || []).forEach(order => {
		if (order && order.goodsId) ids.add(order.goodsId)
	})
	return [...ids]
}
