/**
 * ???????????? userId???????????????? app_user_inviter
 */

export function saveInviteParentUserId(sceneOrId) {
	if (sceneOrId === undefined || sceneOrId === null || sceneOrId === '') return
	let raw = decodeURIComponent(String(sceneOrId)).trim()
	if (raw.indexOf('u') === 0) {
		raw = raw.substring(1)
	}
	const id = parseInt(raw, 10)
	if (Number.isNaN(id) || id <= 0) return

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

export function getCurrentShareUserId() {
	const userInfo = uni.getStorageSync('userInfo')
	if (userInfo && userInfo.userId) {
		return Number(userInfo.userId)
	}
	return null
}

export function buildInviteSharePath(path, extraQuery = {}) {
	const basePath = path.startsWith('/') ? path : `/${path}`
	const userId = getCurrentShareUserId()
	const query = { ...extraQuery }
	if (userId) {
		query.parentUserId = userId
	}
	const queryStr = Object.keys(query)
		.filter((key) => query[key] !== undefined && query[key] !== null && query[key] !== '')
		.map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(query[key])}`)
		.join('&')
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

export function buildInviteScene() {
	const userId = getCurrentShareUserId()
	return userId ? `u${userId}` : ''
}

export function parseLaunchInviteOptions(options) {
	if (!options) return
	if (options.scene) {
		saveInviteParentUserId(options.scene)
	}
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
	parseLaunchInviteOptions(options)
}
