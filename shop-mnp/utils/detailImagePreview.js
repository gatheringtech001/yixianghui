// Keep detail previews on the original image, never the catalog thumbnail.
export function detailImageUrl(value, host = '') {
	if (typeof value !== 'string' || !value.trim()) return ''
	const url = value.trim().replace(/^@\//, '/')
	if (/^(https?:\/\/|data:image\/|wxfile:\/\/|\/static\/)/i.test(url)) return url
	if (url.startsWith('//')) return `https:${url}`
	return `${String(host).replace(/\/$/, '')}/${url.replace(/^\//, '')}`
}

export function buildDetailImagePreview(current, images, host) {
	const selected = detailImageUrl(current, host)
	if (!selected) return null
	const urls = [...new Set((Array.isArray(images) ? images : [current])
		.map(image => detailImageUrl(image, host)).filter(Boolean))]
	if (!urls.includes(selected)) urls.unshift(selected)
	return { current: selected, urls }
}

// Shared Vue page method; the WeChat native viewer provides pinch zoom/swiping.
export function previewDetailImages(current, images) {
	const options = buildDetailImagePreview(current, images, this.host || this.$host)
	if (!options) return
	uni.previewImage({
		...options,
		fail: () => uni.showToast({ title: '图片暂时无法打开，请稍后重试', icon: 'none' })
	})
}
