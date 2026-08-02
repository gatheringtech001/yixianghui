export function formatMoney(value) {
	const num = Number(value)
	if (Number.isNaN(num)) return '0.00'
	return num.toFixed(2)
}

export function normalizeImageUrl(host, url) {
	if (!url) return ''
	if (url.startsWith('data:image') || url.startsWith('http')) return url
	if (url.startsWith('/')) return host + url
	return url
}

export function maskPhone(phone) {
	if (!phone) return ''
	const str = String(phone)
	if (str.length < 7) return str
	return `${str.slice(0, 3)}****${str.slice(-4)}`
}

export function formatDateTime(value) {
	if (!value) return ''
	const str = String(value)
	return str.length >= 10 ? str.slice(0, 10) : str
}

export function formatDateTimeFull(value) {
	if (!value) return ''
	return String(value).replace('T', ' ').slice(0, 19)
}
