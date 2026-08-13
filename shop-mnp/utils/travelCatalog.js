const LOCAL_CITY_IMAGES = {
	'弥勒': '/static/home-design/city-mile.jpg',
	'普洱': '/static/home-design/city-puer.jpg',
	'西双版纳': '/static/home-design/city-banna.jpg',
	'版纳': '/static/home-design/city-banna.jpg',
	'芒市': '/static/home-design/city-mangshi.jpg'
}

const CITY_LABELS = {
	'西双版纳': '版纳'
}

function normalizeName(value) {
	return String(value || '').trim()
}

function categoryIdOf(item) {
	return String((item && item.categoryId) || '')
}

export function getGoodsPrimaryImage(item) {
	if (!item) return ''
	if (normalizeName(item.goodsCover)) return normalizeName(item.goodsCover)
	return normalizeName(item.goodsImages)
		.split(',')
		.map(normalizeName)
		.find(Boolean) || ''
}

export function isVisibleCatalogGoods(item) {
	return Boolean(
		item &&
		item.goodsId &&
		normalizeName(item.goodsName) &&
		getGoodsPrimaryImage(item) &&
		String(item.status) === '1'
	)
}

export function isVisibleTravelGoods(item) {
	if (!isVisibleCatalogGoods(item)) return false
	return !item.goodsType || item.goodsType === 'hotel'
}

export function getTravelCategoryRows(categories, goodsList) {
	const source = categories || []
	const travel = source.find(item => normalizeName(item.categoryName) === '全国旅居')
	if (!travel) return []

	const counts = {}
	const firstImages = {}
	;(goodsList || []).filter(isVisibleTravelGoods).forEach(item => {
		const id = categoryIdOf(item)
		if (!id) return
		counts[id] = (counts[id] || 0) + 1
		if (!firstImages[id]) firstImages[id] = getGoodsPrimaryImage(item)
	})

	return source
		.filter(item => (
			String(item.parentId) === String(travel.categoryId) &&
			String(item.status) === '1' &&
			counts[categoryIdOf(item)] > 0
		))
		.sort((a, b) => (
			Number(a.orderNum || 0) - Number(b.orderNum || 0) ||
			Number(a.categoryId || 0) - Number(b.categoryId || 0)
		))
		.map(item => ({
			category: item,
			goodsCount: counts[categoryIdOf(item)],
			firstImage: firstImages[categoryIdOf(item)]
		}))
}

function createCityCard(row, ad) {
	const category = row.category
	const categoryName = normalizeName(category.categoryName)
	const adImage = normalizeName(ad && ad.adImage)
	return {
		...(ad || {}),
		adName: CITY_LABELS[categoryName] || categoryName,
		adImage: adImage || LOCAL_CITY_IMAGES[categoryName] || row.firstImage,
		linkUrl: String(category.categoryId),
		categoryId: category.categoryId,
		goodsCount: row.goodsCount
	}
}

export function buildHotCityCards(categories, ads, goodsList) {
	const eligibleRows = getTravelCategoryRows(categories, goodsList)
	const rowById = {}
	const rowByName = {}
	eligibleRows.forEach(row => {
		rowById[categoryIdOf(row.category)] = row
		rowByName[normalizeName(row.category.categoryName)] = row
	})

	const cards = []
	const used = {}
	const sortedAds = (ads || []).slice().sort((a, b) => (
		Number(a.orderNum || 0) - Number(b.orderNum || 0) ||
		Number(a.contentId || 0) - Number(b.contentId || 0)
	))
	sortedAds.forEach(ad => {
		const name = normalizeName(ad.adName)
		if (!name || name === '全国' || name === '更多' || String(ad.status) !== '1') return
		const row = rowById[normalizeName(ad.linkUrl)] || rowByName[name]
		if (!row || used[categoryIdOf(row.category)]) return
		const card = createCityCard(row, ad)
		if (!card.adImage) return
		cards.push(card)
		used[categoryIdOf(row.category)] = true
	})

	eligibleRows.forEach(row => {
		const id = categoryIdOf(row.category)
		if (used[id]) return
		const card = createCityCard(row, null)
		if (!card.adImage) return
		cards.push(card)
		used[id] = true
	})
	return cards
}
