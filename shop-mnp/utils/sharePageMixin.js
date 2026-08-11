import {
	bindCopyUrl,
	buildShareAppMessage,
	buildShareTimeline,
	enableShareMenu,
	unbindCopyUrl
} from '@/utils/invite'

function getPageShareConfig(page) {
	if (!page || typeof page.getShareConfig !== 'function') {
		console.warn('[share] page is missing getShareConfig')
		return {}
	}
	return page.getShareConfig() || {}
}

export default {
	onShow() {
		enableShareMenu()
		bindCopyUrl(() => getPageShareConfig(this))
	},
	onHide() {
		unbindCopyUrl()
	},
	onUnload() {
		unbindCopyUrl()
	},
	onShareAppMessage() {
		return buildShareAppMessage(getPageShareConfig(this))
	},
	onShareTimeline() {
		return buildShareTimeline(getPageShareConfig(this))
	}
}
