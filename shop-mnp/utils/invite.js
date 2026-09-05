/**
 * ???????????? userId???????????????? app_user_inviter
 */

export function saveInviteParentUserId(sceneOrId) {
	if (sceneOrId === undefined || sceneOrId === null || sceneOrId === '') return
	let raw
	try {
		raw = decodeURIComponent(String(sceneOrId)).trim()
	} catch (error) {
		return
	}
	if (raw.indexOf('u') === 0) {
		raw = raw.substring(1)
	}
	if (!/^\d+$/.test(raw)) return
	const id = Number(raw)
	if (!Number.isSafeInteger(id) || id <= 0) return

	const userInfo = uni.getStorageSync('userInfo')
	if (userInfo && userInfo.userId && Number(userInfo.userId) === id) {
		return
	}
	uni.setStorageSync('parentUserId', id)
}

export function getInviteParentUserId() {
	const id = uni.getStorageSync('parentUserId')
	return id ? Number(id) : null
}

export function clearInviteParentUserId() {
	uni.removeStorageSync('parentUserId')
}

const DISTRIBUTION_SOURCE_KEY = 'distributionLaunchSource'
let distributionLaunchSource = null
let distributionEntryId = 0

export function saveDistributionLaunchSource(options = {}) {
	const query = options.query || options
	const referrer = options.referrerInfo || {}
	const extraData = referrer.extraData || {}
	const channelCode = query.channelCode || extraData.channelCode
	// 渠道只属于本次外部进入，不能用上一次的缓存触发领券。
	clearDistributionLaunchSource()
	if (!channelCode || !/^[A-Za-z0-9_-]{2,64}$/.test(String(channelCode))) return
	distributionLaunchSource = {
		channelCode: String(channelCode),
		sourceAppId: String(referrer.appId || query.sourceAppId || ''),
		scene: String(options.scene || ''),
		entryId: ++distributionEntryId
	}
}

export function getDistributionLaunchSource() {
	return distributionLaunchSource
}

export function clearDistributionLaunchSource() {
	distributionLaunchSource = null
	uni.removeStorageSync(DISTRIBUTION_SOURCE_KEY)
}

export function getCurrentShareUserId() {
	const userInfo = uni.getStorageSync('userInfo')
	if (userInfo && userInfo.userId) {
		return Number(userInfo.userId)
	}
	return null
}

export function buildInviteQuery(extraQuery = {}) {
	const userId = getCurrentShareUserId()
	const query = { ...extraQuery }
	if (userId) {
		query.parentUserId = userId
	}
	return Object.keys(query)
		.filter((key) => query[key] !== undefined && query[key] !== null && query[key] !== '')
		.map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(query[key])}`)
		.join('&')
}

export function buildInviteSharePath(path, extraQuery = {}) {
	const basePath = path.startsWith('/') ? path : `/${path}`
	const queryStr = buildInviteQuery(extraQuery)
	return queryStr ? `${basePath}?${queryStr}` : basePath
}

export function buildShareAppMessage({ title, path = '/pages/home/home', query = {}, imageUrl = '' } = {}) {
	const sharePath = buildInviteSharePath(path, query)
	const config = {
		title,
		path: sharePath
	}
	if (imageUrl) {
		config.imageUrl = imageUrl
	}
	return config
}

export function buildShareTimeline({ title, query = {}, imageUrl = '' } = {}) {
	const config = {
		title,
		query: buildInviteQuery(query)
	}
	if (imageUrl) {
		config.imageUrl = imageUrl
	}
	return config
}

export function enableShareMenu() {
	if (typeof uni === 'undefined' || typeof uni.showShareMenu !== 'function') {
		return false
	}
	uni.showShareMenu({
		withShareTicket: true,
		menus: ['shareAppMessage', 'shareTimeline'],
		fail(error) {
			console.warn('[share] failed to enable share menu', error)
		}
	})
	return true
}

export function bindCopyUrl(getShareConfig) {
	if (typeof wx === 'undefined' || typeof wx.onCopyUrl !== 'function') {
		return false
	}
	wx.onCopyUrl(() => {
		const config = typeof getShareConfig === 'function'
			? getShareConfig()
			: getShareConfig
		const safeConfig = config || {}
		return {
			title: safeConfig.title || '',
			query: buildInviteQuery(safeConfig.query || {})
		}
	})
	return true
}

export function unbindCopyUrl() {
	if (typeof wx === 'undefined' || typeof wx.offCopyUrl !== 'function') {
		return false
	}
	wx.offCopyUrl()
	return true
}

export function buildInviteScene() {
	const userId = getCurrentShareUserId()
	return userId ? `u${userId}` : ''
}

export function parseLaunchInviteOptions(options) {
	if (!options) return
	saveDistributionLaunchSource(options)
	if (options.parentUserId) {
		saveInviteParentUserId(options.parentUserId)
	}
	if (options.query) {
		if (options.query.scene) {
			saveInviteParentUserId(options.query.scene)
		}
		if (options.query.parentUserId) {
			saveInviteParentUserId(options.query.parentUserId)
		}
	}
}

export function parseInvitePageOptions(options) {
	const query = options || {}
	if (query.channelCode && (!distributionLaunchSource || distributionLaunchSource.channelCode !== String(query.channelCode))) {
		saveDistributionLaunchSource({query})
	}
	if (query.scene) saveInviteParentUserId(query.scene)
	if (query.parentUserId) saveInviteParentUserId(query.parentUserId)
}
