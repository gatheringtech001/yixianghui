import request from "./request.js"
const uri = 'mnp/app_user/'

/* 用户登录 */
export function login(data) {
  return request('login', 'POST', data)
}
// 手机号码登录方法
export function loginByMobile(data) {
  return request('loginByMobile', 'POST', data)
}
// 微信授权登录 { code, parentUserId }
export function loginByWx(params) {
	const code = (params && params.code) ? params.code : ''
	let url = `system/auth/social-login/wechat_mnp?code=${encodeURIComponent(code)}`
	const parentUserId = (params && params.parentUserId) || uni.getStorageSync('parentUserId')
	if (parentUserId) {
		url += `&parentUserId=${encodeURIComponent(parentUserId)}`
	}
	return request(url, 'GET')
}

// 注册方法
export function register(data) {
	return request('register', 'POST', data)
}

// 获取用户详细信息
export function getInfo() {
	return request(`${uri}data`, 'GET')
}

// 微信一键授权（绑定手机号 + 自动补全资料）
export function wxProfileAuth(data) {
	return request(`${uri}wx_profile_auth`, 'POST', data)
}

// 修改个人信息（昵称、头像）
export function updateInfo(data) {
	return request(`${uri}update_by_mnp`, 'POST', data)
}

// 退出登录
export function logout() {
	return request('logout', 'POST')
}

// 获取短信验证码
export function getLoginSms(data) {
	return request('getLoginSms', 'POST', data)
}
