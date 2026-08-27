export const CUSTOMER_SERVICE_POSITION_ID = 7
export const HOUSEKEEPER_ROTATION_KEY = 'housekeeperQrCursor'

export function selectRotatingHousekeeper(items, cursor) {
	if (!Array.isArray(items) || !items.length) {
		return { item: null, nextCursor: 0 }
	}

	const parsedCursor = Number.parseInt(cursor, 10)
	const safeCursor = Number.isFinite(parsedCursor) && parsedCursor >= 0
		? parsedCursor % items.length
		: 0

	return {
		item: items[safeCursor],
		nextCursor: (safeCursor + 1) % items.length
	}
}
