function text(value) {
	return value == null ? '' : String(value)
}

export function getActivityOrderStatus(order) {
	const payStatus = text(order && order.payStatus)
	const orderStatus = text(order && order.status)
	if (payStatus === '4' || orderStatus === '4') return '4'
	if (payStatus === '3' || orderStatus === '3') return '3'
	if (payStatus === '2' || orderStatus === '2') return '2'
	if (payStatus === '0' || orderStatus === '0') return '0'
	return '1'
}

export function findActiveActivityOrder(rows, activityId) {
	return (rows || []).find(order => {
		if (text(order && order.activityId) !== text(activityId)) return false
		return ['0', '1', '3'].includes(getActivityOrderStatus(order))
	}) || null
}

export function mapActivityOrderForOrderList(order) {
	const info = (order && order.activityInfo) || {}
	const signCount = Number(order && order.signCount) || 1
	const amount = Number(order && order.moneyPayable) || 0
	return {
		...order,
		orderKind: 'activity',
		status: getActivityOrderStatus(order),
		activityId: order && order.activityId,
		goodsCount: signCount,
		goodsList: [{
			goodsType: 'activity',
			goodsName: info.activityName || '活动预约',
			goodsCover: info.activityCover || '',
			price: signCount > 0 ? amount / signCount : amount,
			activityTime: info.activityTime || '',
			address: info.address || ''
		}]
	}
}

export function filterOrdersByTab(orders, type) {
	const tab = Number(type) || 0
	if (tab === 0) return orders || []
	return (orders || []).filter(order => text(order.status) === text(tab - 1))
}

export function mergeOrdersByCreateTime(goodsOrders, activityOrders) {
	return [...(goodsOrders || []), ...(activityOrders || [])].sort((left, right) => {
		return text(right && right.createTime).localeCompare(text(left && left.createTime))
	})
}
