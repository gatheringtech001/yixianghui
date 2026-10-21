import { getGoodsList, getGoodsCatrgorys } from '@/api/shop/index'
import { getActivityList, getActivityCategoryList } from '@/api/activity/index'
import { createCatalogCache } from './catalogCache'

const cache = createCatalogCache()
function cached(kind, params, loader, { refresh = false } = {}) {
	const normalized = Object.keys(params || {}).sort().map(key => [key, params[key]])
	// Site-specific browsing must never share results between different sites.
	const site = params && params.ignoreSite ? null : (uni.getStorageSync('site') || {}).deptId
	const key = JSON.stringify([kind, site, normalized])
	if (refresh) cache.invalidate(key)
	return cache.get(key, () => loader(params))
}
export const getCatalogGoods = (params, options) => cached('goods', params, getGoodsList, options)
export const getCatalogCategories = params => cached('categories', params, getGoodsCatrgorys)
export const getCatalogActivities = (params, options) => cached('activities', params, getActivityList, options)
export const getCatalogActivityCategories = params => cached('activityCategories', params, getActivityCategoryList)
export const clearCatalogCache = () => cache.clear()
