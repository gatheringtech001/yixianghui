function removeStyleProperties(styleValue, blockedProperties) {
	const blocked = new Set(blockedProperties)
	return String(styleValue || '')
		.split(';')
		.map(declaration => declaration.trim())
		.filter(Boolean)
		.filter(declaration => {
			const separator = declaration.indexOf(':')
			if (separator < 0) return false
			return !blocked.has(declaration.slice(0, separator).trim().toLowerCase())
		})
		.join(';')
}

export function normalizeRichTextHtml(html) {
	if (!html) return ''

	let content = String(html)

	content = content.replace(/<img\b([^>]*?)\s*\/?>/gi, (match, attrs) => {
		let nextAttrs = String(attrs || '')
			.replace(/\swidth\s*=\s*["'][^"']*["']/gi, '')
			.replace(/\swidth\s*=\s*[^\s/>]+/gi, '')
			.replace(/\sheight\s*=\s*["'][^"']*["']/gi, '')
			.replace(/\sheight\s*=\s*[^\s/>]+/gi, '')
			.replace(/\s+\/$/, '')

		if (/style\s*=\s*["']([^"']*)["']/i.test(nextAttrs)) {
			nextAttrs = nextAttrs.replace(/style\s*=\s*["']([^"']*)["']/i, (styleMatch, styleValue) => {
				const preserved = removeStyleProperties(styleValue, [
					'width', 'min-width', 'max-width', 'height', 'min-height', 'max-height', 'object-fit'
				])
				return `style="width:100%;max-width:100%;display:block;${preserved ? `${preserved};` : ''}"`
			})
		} else {
			nextAttrs += ' style="width:100%;max-width:100%;display:block;"'
		}

		return `<img${nextAttrs}>`
	})

	content = content.replace(/<figure([^>]*)>/gi, '<div$1>')
	content = content.replace(/<\/figure>/gi, '</div>')

	content = content.replace(/<(div|p|section|table|tbody|tr|td)([^>]*?)style\s*=\s*["']([^"']*)["']([^>]*?)>/gi, (match, tag, before, styleValue, after) => {
		if (/background(-image)?\s*:/i.test(styleValue)) {
			return match
		}
		if (!/(?:width|height|max-width|max-height|min-width|min-height)\s*:|overflow\s*:\s*hidden/i.test(styleValue)) {
			return match
		}
		const style = removeStyleProperties(styleValue, [
			'width', 'min-width', 'max-width', 'height', 'min-height', 'max-height', 'overflow'
		])
		return `<${tag}${before}style="max-width:100%;${style ? `${style};` : ''}"${after}>`
	})

	return content
}

export function addHostPrefixToRichText(content, host) {
	if (!content || !host) return content || ''
	const normalizedHost = String(host).replace(/\/$/, '')

	return String(content).replace(/(src|href)=["']([^"']*)["']/g, (match, attr, url) => {
		if (!url) return match
		const profilePathIndex = url.indexOf('/profile/')
		if (profilePathIndex >= 0) {
			return `${attr}="${normalizedHost}${url.slice(profilePathIndex)}"`
		}
		if (url.startsWith('http') || url.startsWith('https') || url.startsWith('//') || url.startsWith('data:')) {
			return match
		}

		if (url.startsWith('/')) {
			let normalizedUrl = url.replace('/dev-api', '').replace('/api', '')
			if (!normalizedUrl.startsWith('/')) {
				normalizedUrl = `/${normalizedUrl}`
			}
			return `${attr}="${normalizedHost}${normalizedUrl}"`
		}

		return `${attr}="${normalizedHost}/${url}"`
	})
}

export function prepareRichTextHtml(content, host) {
	return normalizeRichTextHtml(addHostPrefixToRichText(content, host))
}
