const HOME_PAGE_URL = '/pages/home/home'

export function safeBack() {
	const pages = getCurrentPages()
	if (pages.length > 1) {
		uni.navigateBack()
		return
	}

	uni.switchTab({ url: HOME_PAGE_URL })
}
