import thumbnails from './catalogThumbnails'

export function catalogImageUrl(path, host, failed = {}) {
	if (!path) return ''
	const source = String(path)
	const relative = host && source.startsWith(host + '/') ? source.slice(host.length) : source
	const thumbnail = thumbnails[relative]
	const selected = thumbnail && !failed[relative] ? thumbnail : source
	return /^https?:\/\//.test(selected) || selected.startsWith('/static/') ? selected : host + selected
}

export function catalogImageKey(path, host) {
	const source = String(path || '')
	return host && source.startsWith(host + '/') ? source.slice(host.length) : source
}
