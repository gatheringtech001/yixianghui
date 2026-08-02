/** 登录 token 本地有效期：7 天（毫秒） */
export const TOKEN_TTL_MS = 7 * 24 * 60 * 60 * 1000

/** 记录本次登录时间，用于本地过期判断 */
export function saveLoginTime() {
	uni.setStorageSync('setTokenTime', Date.now())
}

/** 保存 token 并刷新登录时间 */
export function saveToken(token) {
	uni.setStorageSync('token', token)
	saveLoginTime()
}

/** 本地 token 是否已过期（无记录视为过期） */
export function isTokenExpired() {
	const setTokenTime = uni.getStorageSync('setTokenTime')
	if (!setTokenTime) {
		return true
	}
	return Date.now() - Number(setTokenTime) >= TOKEN_TTL_MS
}
