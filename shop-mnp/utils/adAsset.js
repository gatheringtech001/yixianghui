import { getBannerPosList, getBannerList } from '@/api/index'

export const AD_POSITION = {
	BRAND_LOGO: 'mnp_brand_logo',
	HOME_HOUSEKEEPER: 'mnp_home_housekeeper',
	PROFILE_STEWARD: 'mnp_profile_steward'
}

export const AD_FALLBACK = {
	BRAND_LOGO: '/static/home-design/brand-logo-transparent.png',
	HOME_HOUSEKEEPER: '/static/home-design/support-avatar-1.png',
	PROFILE_STEWARD: ''
}

export function resolveAdImageUrl(host, adImage) {
	const image = adImage == null ? '' : String(adImage).trim()
	if (!image) return ''
	return image.startsWith('http') ? image : (host || '') + image
}

function normalizeList(data) {
	if (Array.isArray(data)) return data
	if (data && Array.isArray(data.rows)) return data.rows
	return []
}

export async function loadAdImageUrl(positionCode, host) {
	if (positionCode === AD_POSITION.BRAND_LOGO) {
		return AD_FALLBACK.BRAND_LOGO
	}
	try {
		const { data: positions } = await getBannerPosList({
			positionCode,
			pageNum: 1,
			pageSize: 10
		})
		const positionList = normalizeList(positions)
		if (!positionList.length) return ''

		const positionId = positionList[0].positionId
		const { data: contents } = await getBannerList({
			positionId
		})
		const contentList = normalizeList(contents)
		if (!contentList.length || !contentList[0].adImage) return ''

		return resolveAdImageUrl(host, contentList[0].adImage)
	} catch (error) {
		console.log('loadAdImageUrl', positionCode, error)
		return ''
	}
}
