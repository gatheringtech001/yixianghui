import BaseUrl from '@/api/baseUrl.js'

export function normalizeAvatarPath(avatar) {
	if (!avatar) return ''
	let value = String(avatar).trim()
	const profileIndex = value.indexOf('/profile/')
	if (profileIndex >= 0) {
		value = value.substring(profileIndex)
	}
	if (value.length > 500) {
		throw new Error('\u5934\u50cf\u5730\u5740\u8fc7\u957f\uff0c\u8bf7\u91cd\u65b0\u4e0a\u4f20')
	}
	return value
}

export function uploadUserAvatar(filePath) {
	return new Promise((resolve, reject) => {
		if (!filePath) {
			reject(new Error('\u8bf7\u5148\u9009\u62e9\u5934\u50cf'))
			return
		}
		uni.uploadFile({
			url: `${BaseUrl.publicUrl}common/upload`,
			filePath,
			name: 'file',
			header: {
				Authorization: uni.getStorageSync('token') ? `Bearer ${uni.getStorageSync('token')}` : ''
			},
			success(res) {
				try {
					const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
					const rawAvatar = data.fileName || data.url || (data.data && (data.data.fileName || data.data.url))
					if (data.code === 200 && rawAvatar) {
						resolve(normalizeAvatarPath(rawAvatar))
						return
					}
					reject(new Error(data.msg || '\u5934\u50cf\u4e0a\u4f20\u5931\u8d25'))
				} catch (error) {
					reject(error instanceof Error ? error : new Error('\u5934\u50cf\u4e0a\u4f20\u5931\u8d25'))
				}
			},
			fail(err) {
				const errMsg = (err && err.errMsg) || ''
				if (errMsg.indexOf('\u56fe\u7247\u83b7\u53d6\u5931\u8d25') !== -1) {
					reject(new Error('\u5934\u50cf\u6587\u4ef6\u5df2\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u9009\u62e9\u5934\u50cf'))
					return
				}
				if (errMsg.indexOf('url not in domain list') !== -1) {
					reject(new Error('\u4e0a\u4f20\u57df\u540d\u672a\u914d\u7f6e\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458'))
					return
				}
				reject(new Error(errMsg || '\u5934\u50cf\u4e0a\u4f20\u5931\u8d25'))
			}
		})
	})
}
