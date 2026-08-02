const TENCENT_MAP_APP_ID = 'wx7643d5f831302ab0'

function copyText(text) {
	return new Promise((resolve, reject) => {
		uni.setClipboardData({
			data: text,
			success: resolve,
			fail: reject
		})
	})
}

function openTencentMap() {
	return new Promise((resolve, reject) => {
		uni.navigateToMiniProgram({
			appId: TENCENT_MAP_APP_ID,
			envVersion: 'release',
			success: resolve,
			fail: reject
		})
	})
}

export async function openActivityLocation(detail = {}) {
	const address = String(detail.address || '').trim()

	if (!address) {
		uni.showToast({ title: '暂无活动地点', icon: 'none' })
		return
	}

	try {
		await copyText(address)
	} catch (error) {
		uni.showToast({ title: '地址复制失败', icon: 'none' })
		return
	}

	uni.showToast({
		title: '地址已复制，正在打开腾讯地图',
		icon: 'none',
		duration: 2000
	})

	setTimeout(async () => {
		try {
			await openTencentMap()
		} catch (error) {
			uni.showToast({
				title: '打开腾讯地图失败，请手动粘贴地址搜索',
				icon: 'none'
			})
		}
	}, 400)
}
