const SERVICE_FILTER_KEY = 'serviceFilter'

export function getServiceFilter() {
	return uni.getStorageSync(SERVICE_FILTER_KEY) || null
}

export function saveServiceFilter(filter) {
	if (!filter) return
	uni.setStorageSync(SERVICE_FILTER_KEY, filter)
	if (filter.cityId) {
		uni.setStorageSync('site', {
			deptId: filter.cityId,
			deptName: filter.cityName,
			parentId: filter.provinceId,
			parentName: filter.provinceName
		})
	}
}

export async function resolveServiceFilter(getSiteBydepId) {
	const cached = getServiceFilter()
	const site = uni.getStorageSync('site')
	if (cached && cached.provinceId && cached.cityId !== undefined) {
		if (!site || !site.deptId || cached.cityId === site.deptId || cached.cityId === 0) {
			return cached
		}
	}
	if (!site || !site.deptId) {
		return cached
	}
	try {
		const res = await getSiteBydepId(site.deptId)
		const data = res && res.data
		if (!data) return cached
		const filter = {
			provinceId: data.parentId,
			provinceName: data.parentName || '',
			cityId: data.deptId,
			cityName: data.deptName
		}
		saveServiceFilter(filter)
		return filter
	} catch (error) {
		console.error('resolveServiceFilter', error)
		return cached
	}
}
