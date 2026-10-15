import { getGoodsList, getGoodsCatrgorys } from '@/api/shop/index'
import { getActivityList, getActivityCategoryList } from '@/api/activity/index'
import { createCatalogCache } from './catalogCache'

const cache = createCatalogCache()
function cached(kind, params, loader) {
	const normalized = Object.keys(params || {}).sort().map(key => [key, params[key]])
	// Site-specific browsing must never share results between different sites.
	const site = params && params.ignoreSite ? null : (uni.getStorageSync('site') || {}).deptId
	return cache.get(JSON.stringify([kind, site, normalized]), () => loader(params))
}
export const getCatalogGoods = params => cached('goods', params, getGoodsList)
export const getCatalogCategories = params => cached('categories', params, getGoodsCatrgorys)
export const getCatalogActivities = params => cached('activities', params, getActivityList)
export const getCatalogActivityCategories = params => cached('activityCategories', params, getActivityCategoryList)
export const clearCatalogCache = () => cache.clear()
