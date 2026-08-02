const PROVINCE_SUFFIXES = ['\u603b\u516c\u53f8', '\u5206\u516c\u53f8', '\u7701']

export function formatProvinceName(name) {
	if (!name) return ''
	let text = String(name).trim()
	PROVINCE_SUFFIXES.forEach((suffix) => {
		if (text.endsWith(suffix)) {
			text = text.slice(0, -suffix.length)
		}
	})
	return text.trim()
}
