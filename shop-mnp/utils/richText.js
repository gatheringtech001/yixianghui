export function normalizeRichTextHtml(html) {
	if (!html) return ''

	let content = String(html)

	content = content.replace(/<img\b([^>]*?)\s*\/?>/gi, (match, attrs) => {
		let nextAttrs = String(attrs || '')
			.replace(/\sheight\s*=\s*["'][^"']*["']/gi, '')
			.replace(/\sheight\s*=\s*[^\s/>]+/gi, '')
			.replace(/\s+\/$/, '')

		if (/style\s*=\s*["']([^"']*)["']/i.test(nextAttrs)) {
			nextAttrs = nextAttrs.replace(/style\s*=\s*["']([^"']*)["']/i, (styleMatch, styleValue) => {
				let style = styleValue
					.replace(/(?:^|;|\s)height\s*:\s*[^;]+;?/gi, '')
					.replace(/(?:^|;|\s)max-height\s*:\s*[^;]+;?/gi, '')
					.replace(/(?:^|;|\s)min-height\s*:\s*[^;]+;?/gi, '')

				if (!/max-width\s*:/i.test(style)) {
					style = `max-width:100%;${style}`
				}
				if (!/height\s*:/i.test(style)) {
					style = `height:auto;${style}`
				}
				if (!/display\s*:/i.test(style)) {
					style = `display:block;${style}`
				}
				return `style="${style}"`
			})
		} else {
			nextAttrs += ' style="max-width:100%;height:auto;display:block;"'
		}

		return `<img${nextAttrs}>`
	})

	content = content.replace(/<figure([^>]*)>/gi, '<div$1>')
	content = content.replace(/<\/figure>/gi, '</div>')

	content = content.replace(/<div([^>]*?)style\s*=\s*["']([^"']*)["']([^>]*?)>/gi, (match, before, styleValue, after) => {
		if (/background(-image)?\s*:/i.test(styleValue)) {
			return match
		}
		if (!/height\s*:|max-height\s*:|overflow\s*:\s*hidden/i.test(styleValue)) {
			return match
		}
		const style = styleValue
			.replace(/(?:^|;|\s)height\s*:\s*[^;]+;?/gi, '')
			.replace(/(?:^|;|\s)max-height\s*:\s*[^;]+;?/gi, '')
			.replace(/(?:^|;|\s)overflow\s*:\s*hidden;?/gi, '')
		return `<div${before}style="${style}"${after}>`
	})

	return content
}

export function addHostPrefixToRichText(content, host) {
	if (!content || !host) return content || ''

	return String(content).replace(/(src|href)=["']([^"']*)["']/g, (match, attr, url) => {
		if (!url) return match
		if (url.startsWith('http') || url.startsWith('https') || url.startsWith('//') || url.startsWith('data:')) {
			return match
		}

		if (url.startsWith('/')) {
			let normalizedUrl = url.replace('/dev-api', '').replace('/api', '')
			if (!normalizedUrl.startsWith('/')) {
				normalizedUrl = `/${normalizedUrl}`
			}
			return `${attr}="${host}${normalizedUrl}"`
		}

		return `${attr}="${host}/${url}"`
	})
}

export function prepareRichTextHtml(content, host) {
	return normalizeRichTextHtml(addHostPrefixToRichText(content, host))
}
