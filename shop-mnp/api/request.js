// 引入配置文件
import BaseUrl from "./baseUrl.js";

function sanitizeRequestError(msg) {
	const text = String(msg || '')
	if (/Error updating database|Data too long|SQLException/i.test(text)) {
		if (/nick_name|nickName/i.test(text)) {
			return '昵称不能超过30个字符'
		}
		if (/avatar/i.test(text)) {
			return '头像保存失败，请重新上传头像'
		}
		return '资料保存失败，请检查后重试'
	}
	return text
}

function buildUrl(path) {
	if (!path || path === 'undefined') {
		console.error('[request] invalid path:', path)
		return BaseUrl.publicUrl
	}
	return BaseUrl.publicUrl + path
}

export default function request(url, method, data) {
	if (!url) {
		console.error('[request] url is empty')
		return Promise.reject(new Error('接口地址无效'))
	}
	if (typeof data === 'string') {
		url = url + '/' + data
	}
	const baseConfig = {
		url: buildUrl(url),
		data: data,
		method: method,
		header: {
			'Authorization': uni.getStorageSync('token') ? ('Bearer ' + uni.getStorageSync('token')) : '',
			'X-Requested-With': 'XMLHttpRequest',
			"Accept": "application/json",
			'Content-Type': 'application/json; charset=UTF-8'
		},
		dataType: "json"
	}
	return new Promise((resolve, reject) => {
		uni.request({
			...baseConfig,
			timeout: 30000,
			success(res) {
				if (res.statusCode !== 200) {
					const msg = (res.data && (res.data.msg || res.data.message || res.data.error)) || ('请求失败(' + res.statusCode + ')')
					reject(new Error(msg))
					return
				}
				if (res.data && res.data.code == 401) {
					uni.removeStorageSync('token')
					uni.removeStorageSync('userInfo')
					reject(new Error('登录已过期'))
					return
				}
				if (res.data && res.data.code && res.data.code !== 200) {
					const msg = sanitizeRequestError(res.data.msg || '请求失败')
					reject(new Error(msg))
					return
				}
				resolve(res.data)
			},
			fail(err) {
				const msg = (err && err.errMsg) || '网络请求失败'
				uni.showToast({ title: msg, icon: 'none', duration: 2500 })
				reject(err)
			}
		})
	})
}
