import { loginByWx, getInfo, wxProfileAuth, updateInfo } from '@/api/public'
import { bindInviter } from '@/api/member/index'
import { saveToken, isTokenExpired } from '@/utils/auth'
import { getInviteParentUserId, clearInviteParentUserId } from '@/utils/invite'
import { uploadUserAvatar, normalizeAvatarPath } from '@/utils/uploadAvatar'

const DEFAULT_NICKNAME = '微信小程序用户'

export function isProfileComplete(userInfo) {
	if (!userInfo || typeof userInfo !== 'object') return false
	const nick = userInfo.nickName
	const avatar = userInfo.avatar
	return !!(nick && nick !== DEFAULT_NICKNAME && avatar)
}

function isSessionValid() {
	const token = uni.getStorageSync('token')
	return !!(token && !isTokenExpired())
}

export function isAuthorizedUser() {
	const userInfo = uni.getStorageSync('userInfo')
	return isSessionValid() && isProfileComplete(userInfo)
}

function resolveAuthPopup(pageVm) {
	if (!pageVm || !pageVm.$refs) return null
	const ref = pageVm.$refs.authProfilePopup
	if (!ref) return null
	return Array.isArray(ref) ? ref[0] : ref
}

export function bindPageAuthPopup(pageVm) {
	resolveAuthPopup(pageVm)
}

export function syncConsultantStorage(consultant) {
	if (consultant && consultant.consultantId) {
		uni.setStorageSync('consultant', consultant)
	} else {
		uni.removeStorageSync('consultant')
	}
}

async function fetchUserInfo() {
	const res = await getInfo()
	syncConsultantStorage(res.consultant)
	uni.setStorageSync('userCard', res.userCard)
	const userInfo = res.data || {}
	if (res.liveAddress != null) {
		userInfo.liveAddress = res.liveAddress
	}
	uni.setStorageSync('userInfo', userInfo)
	uni.setStorageSync('userData', res.userInfo)
	await tryBindInviter()
	return userInfo
}

async function tryBindInviter() {
	const parentUserId = getInviteParentUserId()
	if (!parentUserId) return
	const userInfo = uni.getStorageSync('userInfo')
	const currentUserId = userInfo && userInfo.userId ? Number(userInfo.userId) : null
	if (currentUserId && currentUserId === Number(parentUserId)) {
		clearInviteParentUserId()
		return
	}
	try {
		await bindInviter({ parentUserId })
		clearInviteParentUserId()
	} catch (e) {
		console.warn('绑定邀请人失败:', e)
	}
}

function wxSilentLogin() {
	return new Promise((resolve, reject) => {
		uni.login({
			provider: 'weixin',
			success(loginRes) {
				if (!loginRes.code) {
					reject(new Error('微信登录失败'))
					return
				}
				loginByWx({ code: loginRes.code, parentUserId: getInviteParentUserId() }).then(res => {
					if (!res || !res.token) {
						reject(new Error((res && res.msg) || '登录失败'))
						return
					}
					saveToken(res.token)
					fetchUserInfo().then(resolve).catch(reject)
				}).catch(reject)
			},
			fail: () => reject(new Error('微信登录失败'))
		})
	})
}

function openAuthPopup(pageVm) {
	return new Promise((resolve) => {
		const popup = resolveAuthPopup(pageVm)
		if (!popup || typeof popup.open !== 'function') {
			uni.showToast({ title: '授权组件未就绪，请重试', icon: 'none' })
			resolve(null)
			return
		}
		popup.open(resolve, async () => {
			const userInfo = await wxSilentLogin()
			return {
				profileComplete: isProfileComplete(userInfo),
				userInfo
			}
		})
	})
}

function normalizeApiError(e, fallback) {
	const msg = (e && e.message) || fallback
	if (/404|Not Found|not found/i.test(msg)) {
		return '服务端接口未更新，请部署最新后端后重试'
	}
	if (msg.indexOf('用户账号') !== -1) {
		return '服务端接口未更新，请部署最新后端后重试'
	}
	if (/Error updating database|Data too long|SQLException/i.test(msg)) {
		if (/nick_name|nickName/i.test(msg)) {
			return '昵称不能超过30个字符'
		}
		if (/avatar/i.test(msg)) {
			return '头像保存失败，请重新上传头像'
		}
		return '资料保存失败，请检查昵称和头像后重试'
	}
	return msg
}

function isLocalAvatar(path) {
	if (!path) return false
	if (path.startsWith('/profile/') || path.startsWith('/api/profile/')) return false
	return path.startsWith('wxfile://')
		|| path.startsWith('http://tmp')
		|| path.startsWith('https://tmp')
		|| path.startsWith('http://usr')
		|| path.startsWith('https://usr')
		|| (!path.startsWith('/') && !path.startsWith('http'))
}

async function saveProfile(profile) {
	if (!profile || !profile.nickName || !profile.avatar) {
		throw new Error('请完善头像和昵称')
	}
	if (!isSessionValid()) {
		await wxSilentLogin()
	}

	let avatar = profile.avatar
	if (isLocalAvatar(avatar)) {
		avatar = await uploadUserAvatar(avatar)
	} else {
		avatar = normalizeAvatarPath(avatar)
	}

	const nickName = String(profile.nickName || '').trim()
	if (nickName.length > 30) {
		throw new Error('昵称不能超过30个字符')
	}

	const updateRes = await updateInfo({
		nickName,
		avatar
	})
	if (updateRes && updateRes.code && updateRes.code !== 200) {
		throw new Error(updateRes.msg || '资料保存失败')
	}

	if (profile.phoneCode) {
		const res = await wxProfileAuth({ phoneCode: profile.phoneCode })
		if (!res || res.code !== 200) {
			throw new Error((res && res.msg) || '手机号绑定失败')
		}
	}

	await fetchUserInfo()
}

async function continueAuthorizedLogin(pageVm, options = {}) {
	const { showLoading = true } = options
	const profile = await openAuthPopup(pageVm)
	if (!profile) return false
	if (profile.loginOnly) return isAuthorizedUser()

	if (showLoading) {
		uni.showLoading({ title: '登录中...', mask: true })
	}
	try {
		await saveProfile(profile)
		return isAuthorizedUser()
	} catch (e) {
		uni.showToast({
			title: normalizeApiError(e, '登录失败，请重试'),
			icon: 'none',
			duration: 3000
		})
		return false
	} finally {
		if (showLoading) {
			uni.hideLoading({ noConflict: true })
		}
	}
}

export function runWithAuth(pageVm, onDone, options = {}) {
	if (typeof onDone !== 'function') return
	if (isAuthorizedUser()) {
		onDone(true)
		return
	}
	if (typeof options.onNeedAuth === 'function') {
		options.onNeedAuth()
	}
	const popup = resolveAuthPopup(pageVm)
	if (!popup || typeof popup.open !== 'function') {
		uni.showToast({ title: '授权组件未就绪，请重试', icon: 'none' })
		onDone(false)
		return
	}
	continueAuthorizedLogin(pageVm, options).then(onDone)
}

export function ensureLogin(pageVm, options = {}) {
	return new Promise((resolve) => {
		runWithAuth(pageVm, resolve, options)
	})
}

const initAuthorization = () => {
	uni.showToast({ title: '请使用页面内授权登录', icon: 'none' })
}

export default initAuthorization
